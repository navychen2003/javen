package org.javenstudio.cocoka.widget.model;

import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;

import java.util.LinkedList;

/**
 * Queue of things to run on a looper thread.  Items posted with {@link #post} will not
 * be actually enqued on the handler until after the last one has run, to keep from
 * starving the thread.
 *
 * This class is fifo.
 */
public class DeferredHandler {
    private final LinkedList<Runnable> mQueue = new LinkedList<Runnable>();
    private final MessageQueue mMessageQueue; 
    private final Impl mHandler = new Impl();

    private class Impl extends Handler implements MessageQueue.IdleHandler {
        public void handleMessage(Message msg) {
            Runnable r;
            synchronized (mQueue) {
                if (mQueue.size() == 0) {
                    return;
                }
                r = mQueue.removeFirst();
            }
            r.run();
            synchronized (mQueue) {
                scheduleNextLocked();
            }
        }

        public boolean queueIdle() {
            handleMessage(null);
            return false;
        }
    }

    private class IdleRunnable implements Runnable {
        Runnable mRunnable;

        IdleRunnable(Runnable r) {
            mRunnable = r;
        }

        public void run() {
            mRunnable.run();
        }
    }

    public DeferredHandler(MessageQueue queue) {
    	mMessageQueue = queue; 
    }

    /** Schedule runnable to run after everything that's on the queue right now. */
    public void post(Runnable runnable) {
        synchronized (mQueue) {
            mQueue.add(runnable);
            if (mQueue.size() == 1) {
                scheduleNextLocked();
            }
        }
    }

    /** Schedule runnable to run when the queue goes idle. */
    public void postIdle(final Runnable runnable) {
        post(new IdleRunnable(runnable));
    }

    public void cancelRunnable(Runnable runnable) {
        synchronized (mQueue) {
            while (mQueue.remove(runnable)) { }
        }
    }

    public void cancel() {
        synchronized (mQueue) {
            mQueue.clear();
        }
    }

    void scheduleNextLocked() {
        if (mQueue.size() > 0) {
            Runnable peek = mQueue.getFirst();
            if (peek instanceof IdleRunnable) {
            	mMessageQueue.addIdleHandler(mHandler);
            } else {
                mHandler.sendEmptyMessage(1);
            }
        }
    }
}