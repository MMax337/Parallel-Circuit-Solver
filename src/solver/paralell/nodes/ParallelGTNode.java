package solver.paralell.nodes;

import circuit.CircuitNode;
import circuit.ThresholdNode;

public class ParallelGTNode extends ParallelNode {
  final int threshold;

  public ParallelGTNode(CircuitNode node, ParallelNode parent) {
    super(node, parent);
    threshold = ((ThresholdNode) node).getThreshold();
  }
  
  @Override
  public synchronized Boolean isDetermined() throws InterruptedException {
    if (alreadyDetermined != null) return alreadyDetermined;

    // GT node is true if trueCount > threshold
    // Since trueCount + falseCount = childCount
    // it follows that if
    // childCount - false_count <= threshold then it is false 
    
    if (trueCount > threshold) alreadyDetermined = true;
    if (getChildCount() - falseCount <= threshold) alreadyDetermined = false;
    
    return alreadyDetermined;
  }

  @Override
  public boolean isValueTriviallyKnown() throws InterruptedException {
    return threshold >= getChildCount();
  }
}
