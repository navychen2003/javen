package org.javenstudio.cocoka.worker.job;

public interface FutureListener<T> {
    public void onFutureDone(Future<T> future);
}
