package org.javenstudio.falcon.util.job;

public interface FutureListener<T> {
    public void onFutureDone(Future<T> future);
}
