package demo;

import circuit.CircuitValue;

public class BrokenCircuitValue implements CircuitValue {
    @Override
    public boolean getValue() throws InterruptedException {
        throw new InterruptedException();
    }
}
