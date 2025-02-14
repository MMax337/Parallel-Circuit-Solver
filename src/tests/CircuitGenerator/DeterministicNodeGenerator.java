package tests.CircuitGenerator;

import java.util.Random;

import circuit.CircuitNode;
import circuit.NodeType;

public class DeterministicNodeGenerator extends BalancedRandomCircuitGenerator {
  private NodeType type;
  private boolean leafValue;
  
  public DeterministicNodeGenerator(Random rand, int desirableHeight,
                                    NodeType type, boolean leafValue) {
    super(desirableHeight, rand);
    this.type = type;

    this.leafValue = leafValue;
  }

  @Override
  protected CircuitNode LeafCreator() {
    return CircuitNode.mk(leafValue);
  }

  @Override
  protected NodeType generateType(int currentDepth) {
    return currentDepth >= desirableHeight ? NodeType.LEAF : type;
  }
}
