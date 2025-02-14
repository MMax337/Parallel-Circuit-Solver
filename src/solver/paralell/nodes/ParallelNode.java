package solver.paralell.nodes;

import circuit.CircuitNode;
import circuit.NodeType;

public class ParallelNode {
  protected CircuitNode node;
  // reference to the parent of the node
  protected ParallelNode parent;
  protected CircuitNode[] args;

  protected volatile int trueCount = 0;
  protected volatile int falseCount = 0;
  protected Boolean alreadyDetermined = null;

  @Override
  public synchronized String toString() {
    return node.getType() + "  T:" + trueCount + " F: " + falseCount;
  }

  public static ParallelNode mk(CircuitNode node, ParallelNode parent) {
    return switch (node.getType()) {
      case LEAF -> new ParallelLeafNode(node, parent);
      case IF -> new ParallelIfNode(node, parent);
      case GT -> new ParallelGTNode(node, parent);
      case LT -> new ParallelLTNode(node, parent);
      case AND, OR, NOT -> new ParallelNode(node, parent);
      default -> throw new RuntimeException("Illegal type " + node.getType());
    };
  }

  public final ParallelNode getParentNode() {
    return parent;
  }

  protected ParallelNode(CircuitNode node, ParallelNode parent) {
    this.node = node;
    this.parent = parent;
  }

  public Boolean isMyParentDetermined() {
    if (parent == null) return null;
    return parent.alreadyDetermined;
  }

  public synchronized Boolean wasAlreadyDetermined() {
    return alreadyDetermined;
  }

  /**
   * Updates the node's determination state based on the value of a child node.
   * 
   * <p>If the node is already determined, this method does nothing and returns {@code null}.
   * Otherwise, it increments the count of {@code true} or {@code false} values based on the 
   * given child value. If this update causes the node to become determined, the new determination 
   * state is returned. If the node remains undetermined after the update, {@code null} is returned.
   * 
   * @param childValue the value of the child node ({@code true} or {@code false}).
   * @param child the child node being registered.
   * @return the determination state of the node if it becomes determined as a result 
   *         of this call, or {@code null} if it is already determined or remains undetermined.
   * @throws InterruptedException if {@code isDetermined} throws this exception during evaluation.
   */
  public synchronized Boolean registerChild(boolean childValue, ParallelNode child) throws InterruptedException {
    // if I am determined, I do not want to register any children any more.
    if (alreadyDetermined != null) {
      return null;
    }
    if (childValue) {
      ++trueCount;
    } else {
      ++falseCount;
    }
    return isDetermined();
  }

  protected int getChildCount() throws InterruptedException {
    if (args == null) {
      getChildren();
    }
    return args.length;
  }

  /**
   * 
   * @return the boolean value if the node was determined, null if its value is still undetermined
   * @throws InterruptedException if during the evaluation of the node, the node has thrown InterruptedException
   */
  public synchronized Boolean isDetermined() throws InterruptedException {
    if (alreadyDetermined != null) return alreadyDetermined;

    alreadyDetermined = switch (node.getType()) {
        case AND -> isDeterminedAND();
        case OR -> isDeterminedOR();
        case NOT -> isDeterminedNOT();
        default -> throw new RuntimeException("Illegal type " + node.getType());  // Handle any illegal types
    };

    return alreadyDetermined;
  }

  /**
   * 
   * @return the type of the underlying node 
   */
  public NodeType getType() {
    return node.getType();
  }
  /**
   * 
   * @return boolean representing if the node's value is independent of 
   * its children's values
   */
  public boolean isValueTriviallyKnown() throws InterruptedException {
    return false;
  }
  /**
   * Retrieves the children of the underlying node.
   * 
   * <p>The first call to this method may take a long time, as it invokes 
   * {@code CircuitNode.getArgs()} on the underlying node. After the initial call, 
   * the result is cached, making all subsequent calls fast.
   * 
   * @return the cached list of child nodes
   * @throws InterruptedException if the thread is interrupted while fetching the children
   */
  public CircuitNode[] getChildren() throws InterruptedException {
    if (args != null) return args;
    this.args = node.getArgs();
    return this.args;
  }

  private Boolean isDeterminedAND() throws InterruptedException {
    if (falseCount > 0) return false;
    if (trueCount == getChildCount()) return true;

    return null;
  }

  private Boolean isDeterminedOR() throws InterruptedException {
    if (trueCount > 0) return true;
    if (falseCount == getChildCount()) return false;

    return null;
  }

  private Boolean isDeterminedNOT() {
    if (trueCount == 1) {
      return false;
    }
    if (falseCount == 1) {
      return true;
    }
    return null;
  }
}
