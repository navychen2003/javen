package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.util.Observable;

public class EntityObservable extends Observable<EntityObserver> {

    /**
     * invokes dispatchUpdate on each observer, unless the observer doesn't want
     * self-notifications and the update is from a self-notification
     * @param data
     */
    public synchronized void dispatchChange(IEntity<?> data, int change) {
    	for (int i=0; i < getObserverCount(); i++) { 
    		EntityObserver observer = getObserverAt(i); 
        	if (observer != null)
                observer.dispatchChange(data, change);
        }
    }

    public synchronized void dispatchChange(int count, int change) {
    	for (int i=0; i < getObserverCount(); i++) { 
    		EntityObserver observer = getObserverAt(i); 
        	if (observer != null)
                observer.dispatchChange(count, change);
        }
    }
    
    /**
     * invokes onChange on each observer
     * @param selfChange
     */
    public synchronized void notifyChange(IEntity<?> data, int change) {
    	for (int i=0; i < getObserverCount(); i++) { 
    		EntityObserver observer = getObserverAt(i); 
        	if (observer != null)
                observer.onChange(data, change);
        }
    }
    
    public synchronized void notifyChange(int count, int change) {
    	for (int i=0; i < getObserverCount(); i++) { 
    		EntityObserver observer = getObserverAt(i); 
        	if (observer != null)
                observer.onChange(count, change);
        }
    }
    
}
