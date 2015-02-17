package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.util.Observable;

public class EntitySetObservable extends Observable<EntitySetObserver> {

	/**
     * Invokes onChanged on each observer. Called when the data set being observed has
     * changed, and which when read contains the new state of the data.
     */
    public synchronized void notifyChanged() {
        for (int i=0; i < getObserverCount(); i++) { 
        	EntitySetObserver observer = getObserverAt(i); 
        	if (observer != null)
        		observer.onChanged();
        }
    }

    /**
     * Invokes onInvalidated on each observer. Called when the data set being monitored
     * has changed such that it is no longer valid.
     */
    public synchronized void notifyInvalidated() {
    	for (int i=0; i < getObserverCount(); i++) { 
        	EntitySetObserver observer = getObserverAt(i); 
        	if (observer != null)
                observer.onInvalidated();
        }
    }
    
}
