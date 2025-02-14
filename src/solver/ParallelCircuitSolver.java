package solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import circuit.Circuit;
import circuit.CircuitNode;
import circuit.CircuitSolver;
import circuit.CircuitValue;
import circuit.NodeType;
import solver.paralell.nodes.ParallelIfNode;
import solver.paralell.nodes.ParallelNode;

public class ParallelCircuitSolver implements CircuitSolver {
  private final AtomicBoolean stop = new AtomicBoolean(false);
  private final Set<ParallelCircuitValue> activeValues = ConcurrentHashMap.newKeySet();

  private final ForkJoinPool pool;

  public ParallelCircuitSolver() {
    this.pool = new ForkJoinPool();
  }

  public ParallelCircuitSolver(int parallelism) {
    this.pool = new ForkJoinPool(parallelism);
  }

  @Override
  public void stop() {
    for (ParallelCircuitValue val :  activeValues) {
      val.stop();
    }
    activeValues.clear();
    stop.set(true);

    pool.shutdownNow();
  }

  @Override
  public CircuitValue solve(Circuit c) {
    ParallelCircuitValue result = new ParallelCircuitValue();
    activeValues.add(result);
    
    if (stop.get()) {
      result.stop();
      return result;
    }

    ParallelNode rootNode = ParallelNode.mk(c.getRoot(), null);


    // Submit the task to the ForkJoinPool
    ForkJoinTask<Void> rootTask = new InterruptibleTask(rootNode, result, null);

    pool.submit(rootTask);

    // Return immediately after submitting the task
    return result;
  }

  private class InterruptibleTask extends ForkJoinTask<Void> 
                                    implements RunnableFuture<Void> {
    private ParallelNode eNode;
    private ParallelCircuitValue result;
    private ConcurrentLinkedQueue<ForkJoinTask<Void>> childTasks;
    private InterruptibleTask parentTask;
    private AtomicBoolean wasCancelled = new AtomicBoolean(false);

    // cancelLock is needed to prevent stray interrupts in cancel
    private final Object cancelLock = new Object();
    volatile Thread runner;

    public InterruptibleTask(ParallelNode eNode, ParallelCircuitValue result,
                             InterruptibleTask parentTask) {
      this.eNode = eNode;
      this.result = result;
      this.parentTask = parentTask;
      childTasks = new ConcurrentLinkedQueue<>();
    }
    
    public final Void getRawResult() { return null; }
    public final void setRawResult(Void v) {}

    public final boolean exec() {
      Thread.interrupted();
      runner = Thread.currentThread();
      try {
        if (!isDone()) { // recheck
            call();
        }
        return true;
      } catch (RuntimeException rex) {
        throw rex;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      } finally {
        synchronized (cancelLock) {
          runner = null;
        }
          
        Thread.interrupted();
      }
    }

    public final void run() { invoke(); }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
      if (wasCancelled.getAndSet(true)) {
        return true;
      }

      Thread t = null;
      boolean stat = super.cancel(false);

      synchronized (cancelLock) {
        // without cancelLock it could happen that
        // the cancelling thread fetches non-null runner
        // but before it manages to interrupt it, the executing thread
        // finishes the task and switches to another one. 
        if (mayInterruptIfRunning && (t = runner) != null) {
          try {
            t.interrupt(); 
            
          }  catch (Throwable ignore) {}
        }
      }

      ForkJoinTask<Void> child;
      while ((child = childTasks.poll()) != null) {
        child.cancel(mayInterruptIfRunning);
      }
      return stat;
      
    }
        
    public Void call() throws InterruptedException {
      if (result.isDone() || isCancelled()) {
        return null;
      }
      if (stop.get()) {
        result.stop();
        return null; // Stop task if stopped is true
      }

      
      // If the value of the node does not depend on its children,
      // propagate its value up to the parent
      if (eNode.isValueTriviallyKnown()) {
        propagateUp(); // Propagate the result up
      } else if (eNode.isMyParentDetermined() == null) {
        // Otherwise, process internal nodes (AND, OR, etc.)
        CircuitNode[] children = eNode.getChildren();
        
        // Helper tasks list is required because for some nodes the translation 
        // to Array may be needed to cancel specific branches.
        // Adding and forking at once may lead to the problem where array
        // toArray() will result in a smaller array.
        // (The standard DOES NOT guarantee atomicity of addAll and toArray)

        List<ForkJoinTask<Void>> tasks = new ArrayList<>(children.length);

        for (CircuitNode child : children) {
          tasks.add(new InterruptibleTask(ParallelNode.mk(child, eNode), result, this));
        }

        childTasks.addAll(tasks);
        
        if (!wasCancelled.get()) {
          for (var task : tasks) {
            task.fork();
          } 
        }
      }
      return null;
    }

    private void propagateUp() throws InterruptedException {
      // This method propagates the determined value up the tree
      Boolean value = eNode.isDetermined();
      
      InterruptibleTask temp = this;
      InterruptibleTask prev = null;
      while (temp.parentTask != null && value != null) {
        value = temp.eNode.getParentNode().registerChild(value, temp.eNode);
        prev = temp;
        temp = temp.parentTask;
      }

      if (temp.parentTask == null && value != null) {
        // we reached the root node and the root value is determined
        // if not stopped set the root valeu
        if (!stop.get()) {
          result.setValue(value);
          activeValues.remove(result);  
        }
        propagateDown(temp);
      } else if (temp.eNode.getType() == NodeType.IF) {
        // IF is special because if the first value is determined, we can also 
        // cancel one of the branches 
        ifNodeBranchCancel(temp);
        propagateDown(prev);
      } else { // we have not reached the root
        // // temp is not determined
        // // prev was determined
        propagateDown(prev);
      }
    }
    private void propagateDown(InterruptibleTask task) {
      if (task == null) return;
      task.cancel(true);
    }
    
    private void ifNodeBranchCancel(InterruptibleTask ifTask) {
      ParallelIfNode n = (ParallelIfNode) ifTask.eNode;

      int branchToCancel = n.canBranchBeCancelled();


      if (branchToCancel == -1) return;
      
      @SuppressWarnings("unchecked")
      ForkJoinTask<Void>[] tasks = ifTask.childTasks.toArray(new ForkJoinTask[0]);

      // If the length is less than 3, the node has already been computed and is being canceled.
      if (tasks.length < 3) return;
      
      tasks[branchToCancel].cancel(true);
    }
  }
}