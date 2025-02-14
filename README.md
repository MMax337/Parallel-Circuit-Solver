# Parallel-Circuit-Solver
## Overview
This is a university project that implements a parallel solver for logical formulas, designed to handle computations efficiently by leveraging parallel execution. The solver accounts for the fact that each node's computation may involve blocking operations and incorporates lazy evaluation to optimize performance.

## Features

* **Parallel Execution**: The solver distributes computations across multiple threads to improve efficiency.

* **Lazy Evaluation**: Nodes are evaluated only when their values are required, reducing unnecessary computations.

* **Handling Blocking Operations**: The implementation ensures that blocking computations at certain nodes do not stall the entire evaluation process.


## Implementation Details

Uses the Java Concurrency API to parallelize recursive evaluations. In particular, it uses an adjusted ForkJoinPool that allows for task cancellation via `Thread.interrupt()`. The implementation is optimized to minimize the need for synchronization between threads.

## Testing
The project includes tests for:
* correctness
* lazy evaluation
* stopping the solver
* perfomance
