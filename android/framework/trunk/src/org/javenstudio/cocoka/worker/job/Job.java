package org.javenstudio.cocoka.worker.job;

// A Job is like a Callable, but it has an addition JobContext parameter.
public interface Job<T> {
    public T run(JobContext jc);
}
