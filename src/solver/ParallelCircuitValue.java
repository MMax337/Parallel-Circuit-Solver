package solver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import circuit.CircuitValue;

public class ParallelCircuitValue implements CircuitValue {
    private volatile Boolean value;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public boolean isDone() {
        return value != null;
    }

    public void setValue(boolean value) {
        this.value = value;
        latch.countDown(); // Releases all waiting threads once value is set
    }

    public void stop() {
        stopped.set(true);
        latch.countDown(); // Ensure any waiting threads are released
    }

    @Override
    public boolean getValue() throws InterruptedException {
        // Block until value is set or stop is called
        latch.await();

        if (stopped.get()) { 
            throw new InterruptedException();
        }

        return this.value;
    }
}
