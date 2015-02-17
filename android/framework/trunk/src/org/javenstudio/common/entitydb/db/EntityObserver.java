package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IEntityObserver;

public abstract class EntityObserver {

	public static interface Handler { 
		public void post(Runnable runable);
	}
	
	private Transport mTransport = null;
	private final Object mLock = new Object();
	
	private final Handler mHandler;
	
	private final class NotificationRunnable implements Runnable {

        private final IEntity<?> mData;
        private final int mChange; 
        private final int mCount; 

        public NotificationRunnable(int count, int change) {
        	this(null, change, count); 
        }
        public NotificationRunnable(IEntity<?> data, int change) {
        	this(data, change, 1); 
        }
        public NotificationRunnable(IEntity<?> data, int change, int count) {
            mData = data;
            mChange = change; 
            mCount = count; 
        }

        public void run() {
        	if (mData != null) 
        		EntityObserver.this.onChange(mData, mChange); 
        	else
        		EntityObserver.this.onChange(mCount, mChange); 
        }
    }
	
	public EntityObserver(Handler handler) {
        mHandler = handler;
    }
	
	public IEntityObserver getEntityObserver() {
        synchronized (mLock) {
            if (mTransport == null) {
                mTransport = new Transport(this);
            }
            return mTransport;
        }
    }
	
	public IEntityObserver releaseEntityObserver() {
        synchronized (mLock) {
            Transport oldTransport = mTransport;
            if (oldTransport != null) {
                oldTransport.releaseEntityObserver();
                mTransport = null;
            }
            return oldTransport;
        }
    }
	
	private boolean deliverSelfNotifications() {
        return false;
    }
	
	public void onChange(IEntity<?> data, int change) {} 
	public void onChange(int count, int change) {} 
	
	public final void dispatchChange(IEntity<?> data, int change) {
        if (mHandler == null) {
            onChange(data, change);
        } else {
            mHandler.post(new NotificationRunnable(data, change));
        }
    }
	
	public final void dispatchChange(int count, int change) {
        if (mHandler == null) {
            onChange(count, change);
        } else {
            mHandler.post(new NotificationRunnable(count, change));
        }
    }
	
	private static final class Transport implements IEntityObserver {
		EntityObserver mEntityObserver;

        public Transport(EntityObserver entityObserver) {
        	mEntityObserver = entityObserver;
        }

        @SuppressWarnings({"unused"})
        public boolean deliverSelfNotifications() {
        	EntityObserver entityObserver = mEntityObserver;
            if (entityObserver != null) {
                return entityObserver.deliverSelfNotifications();
            }
            return false;
        }

        public void onChange(IEntity<?> data, int change) {
        	EntityObserver entityObserver = mEntityObserver;
            if (entityObserver != null) {
            	entityObserver.dispatchChange(data, change);
            }
        }

        public void releaseEntityObserver() {
            mEntityObserver = null;
        }
    }
	
}
