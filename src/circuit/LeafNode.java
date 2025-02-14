package circuit;

public abstract non-sealed class LeafNode extends CircuitNode {
    protected LeafNode() {
        super(NodeType.LEAF, new CircuitNode[0]);
    }

    abstract public boolean getValue() throws InterruptedException;
}
