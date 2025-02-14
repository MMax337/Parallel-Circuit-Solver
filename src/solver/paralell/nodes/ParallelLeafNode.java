package solver.paralell.nodes;

import circuit.CircuitNode;
import circuit.LeafNode;

public class ParallelLeafNode extends ParallelNode {
  
  public ParallelLeafNode(CircuitNode node, ParallelNode parent) {
    super(node, parent);
  }

  @Override
  public synchronized Boolean isDetermined() throws InterruptedException {
    if (alreadyDetermined != null) return alreadyDetermined;

    alreadyDetermined = ((LeafNode) node).getValue();

    return alreadyDetermined;
  }

  @Override
  public CircuitNode[] getChildren() {
    return (CircuitNode[]) new CircuitNode[0];
  }

  @Override
  public synchronized Boolean registerChild(boolean childValue, ParallelNode child) throws InterruptedException {
    return null;
  }

  @Override
  public boolean isValueTriviallyKnown() {
    return true;
  }

  @Override
  public synchronized String toString() {
    return "LEAF: " + alreadyDetermined;
  }
}
