package circuit;

public interface CircuitSolver {
    public CircuitValue solve(Circuit c);

    public void stop();
}
