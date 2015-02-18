package org.javenstudio.raptor.paxos;

import org.javenstudio.raptor.paxos.proto.WatcherEvent;
import org.javenstudio.raptor.paxos.Watcher.Event.EventType;
import org.javenstudio.raptor.paxos.Watcher.Event.PaxosState;

/**
 *  A WatchedEvent represents a change on the Paxos that a Watcher
 *  is able to respond to.  The WatchedEvent includes exactly what happened,
 *  the current state of the Paxos, and the path of the znode that
 *  was involved in the event.
 */
public class WatchedEvent {
    final private PaxosState keeperState;
    final private EventType eventType;
    private String path;
    
    /**
     * Create a WatchedEvent with specified type, state and path
     */
    public WatchedEvent(EventType eventType, PaxosState keeperState, String path) {
        this.keeperState = keeperState;
        this.eventType = eventType;
        this.path = path;
    }
    
    /**
     * Convert a WatcherEvent sent over the wire into a full-fledged WatcherEvent
     */
    public WatchedEvent(WatcherEvent eventMessage) {
        keeperState = PaxosState.fromInt(eventMessage.getState());
        eventType = EventType.fromInt(eventMessage.getType());
        path = eventMessage.getPath();
    }
    
    public PaxosState getState() {
        return keeperState;
    }
    
    public EventType getType() {
        return eventType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String val) {
        this.path = val; 
    }

    @Override
    public String toString() {
        return "WatchedEvent state:" + keeperState
            + " type:" + eventType + " path:" + path;
    }

    /**
     *  Convert WatchedEvent to type that can be sent over network
     */
    public WatcherEvent getWrapper() {
        return new WatcherEvent(eventType.getIntValue(), 
                                keeperState.getIntValue(), 
                                path);
    }
}

