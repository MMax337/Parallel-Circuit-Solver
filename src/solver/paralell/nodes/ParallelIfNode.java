package solver.paralell.nodes;

import circuit.CircuitNode;

public class ParallelIfNode extends ParallelNode {
  final Boolean[] childrenValues = {null, null, null};

  public ParallelIfNode(CircuitNode node, ParallelNode parent) {
    super(node, parent);
  }
  
  @Override
  public synchronized String toString() {
    return "IF " + childrenValues[0] + " " + childrenValues[1] + " " + childrenValues[2];
  }

  @Override
  public synchronized Boolean registerChild(boolean childValue, ParallelNode child) throws InterruptedException {
    if (alreadyDetermined != null) {
      return null;
    }
    getChildren();
    // note that args.length == 3.
    for (int i = 0; i < args.length; ++i) {
      if (args[i] == child.node) {
        childrenValues[i] = childValue;
        return isDetermined();
      }
    }
    return null;
  }

  @Override
  public Boolean isDetermined() throws InterruptedException {
    if (childrenValues[0] != null) {
      alreadyDetermined = childrenValues[0] ? childrenValues[1] 
                                            : childrenValues[2];
    } else if (childrenValues[1] != null && childrenValues[2] != null 
              && childrenValues[1] == childrenValues[2]) {
        alreadyDetermined = childrenValues[1];
    }
    return alreadyDetermined;
  }

  /**
   * 
   * @return the index of the branch that can be cancelled. If neither returns -1.
   */
  public int canBranchBeCancelled() {
    if (childrenValues[0] != null) {
      return childrenValues[0] ? 2 : 1;
    } else if (childrenValues[1] != null && childrenValues[2] != null 
              && childrenValues[1] == childrenValues[2]) {
      return 0;
    }
    return -1;
  }


}
