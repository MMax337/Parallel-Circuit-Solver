package solver.paralell.nodes;

import circuit.CircuitNode;
import circuit.ThresholdNode;

public class ParallelLTNode extends ParallelNode {
  final int threshold;

  public ParallelLTNode(CircuitNode node, ParallelNode parent) {
    super(node, parent);
    threshold = ((ThresholdNode) node).getThreshold();
  }
  
  @Override
  public synchronized Boolean isDetermined() throws InterruptedException {
    if (alreadyDetermined != null) return alreadyDetermined;   

    // LT node is false if trueCount >= threshold
    // Since trueCount + falseCount = childCount
    // it follows that if
    // falseCount > childCount - threshold then it is true
    
    if (falseCount > getChildCount() - threshold) alreadyDetermined = true;
    if (trueCount >= threshold) alreadyDetermined = false;

    return alreadyDetermined;
  }

  @Override
  public boolean isValueTriviallyKnown() throws InterruptedException {
    return threshold == 0 || threshold > getChildCount();
  }
}
