package org.javenstudio.common.util;

import java.util.ArrayList;

/**
 * Provides methods for (un)registering arbitrary observers in an ArrayList.
 */
public abstract class Observable<T> {
	
    /**
     * The list of observers.  An observer can be in the list at most
     * once and will never be null.
     */
    private final ArrayList<T> mObservers = new ArrayList<T>();

    public synchronized int getObserverCount() { 
    	synchronized(mObservers) {
    		return mObservers.size();
    	}
    }
    
    public synchronized T getObserverAt(int index) { 
    	synchronized(mObservers) {
    		return index >= 0 && index < mObservers.size() ? mObservers.get(index) : null;
    	}
    }
    
    /**
     * Adds an observer to the list. The observer cannot be null and it must not already
     * be registered.
     * @param observer the observer to register
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is already registered
     */
    public synchronized void registerObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }
    }

    /**
     * Removes a previously registered observer. The observer must not be null and it
     * must already have been registered.
     * @param observer the observer to unregister
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is not yet registered
     */
    public synchronized void unregisterObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            int index = mObservers.indexOf(observer);
            if (index == -1) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
            mObservers.remove(index);
        }
    }
    
    /**
     * Remove all registered observer
     */
    public synchronized void unregisterAll() {
        synchronized(mObservers) {
            mObservers.clear();
        }        
    }
}
