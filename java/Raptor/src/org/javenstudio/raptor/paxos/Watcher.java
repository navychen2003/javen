package org.javenstudio.raptor.paxos;

/**
 * This interface specifies the public interface an event handler class must
 * implement. A Paxos client will get various events from the ZooKeepr
 * server it connects to. An application using such a client handles these
 * events by registering a callback object with the client. The callback object
 * is expected to be an instance of a class that implements Watcher interface.
 * 
 */
public interface Watcher {

    /**
     * This interface defines the possible states an Event may represent
     */
    public interface Event {
        /**
         * Enumeration of states the Paxos may be at the event
         */
        public enum PaxosState {
            /** Unused, this state is never generated by the server */
            @Deprecated
            Unknown (-1),

            /** The client is in the disconnected state - it is not connected
             * to any server in the ensemble. */
            Disconnected (0),

            /** Unused, this state is never generated by the server */
            @Deprecated
            NoSyncConnected (1),

            /** The client is in the connected state - it is connected
             * to a server in the ensemble (one of the servers specified
             * in the host connection parameter during Paxos client
             * creation). */
            SyncConnected (3),

            /** The serving cluster has expired this session. The Paxos
             * client connection (the session) is no longer valid. You must
             * create a new client connection (instantiate a new Paxos
             * instance) if you with to access the ensemble. */
            Expired (-112);

            private final int intValue;     // Integer representation of value
                                            // for sending over wire

            PaxosState(int intValue) {
                this.intValue = intValue;
            }

            public int getIntValue() {
                return intValue;
            }

            public static PaxosState fromInt(int intValue) {
                switch(intValue) {
                    case   -1: return PaxosState.Unknown;
                    case    0: return PaxosState.Disconnected;
                    case    1: return PaxosState.NoSyncConnected;
                    case    3: return PaxosState.SyncConnected;
                    case -112: return PaxosState.Expired;

                    default:
                        throw new RuntimeException("Invalid integer value for conversion to PaxosState");
                }
            }
        }

        /**
         * Enumeration of types of events that may occur on the Paxos
         */
        public enum EventType {
            None (-1),
            NodeCreated (1),
            NodeDeleted (2),
            NodeDataChanged (3),
            NodeChildrenChanged (4);

            private final int intValue;     // Integer representation of value
                                            // for sending over wire

            EventType(int intValue) {
                this.intValue = intValue;
            }

            public int getIntValue() {
                return intValue;
            }

            public static EventType fromInt(int intValue) {
                switch(intValue) {
                    case -1: return EventType.None;
                    case  1: return EventType.NodeCreated;
                    case  2: return EventType.NodeDeleted;
                    case  3: return EventType.NodeDataChanged;
                    case  4: return EventType.NodeChildrenChanged;

                    default:
                        throw new RuntimeException("Invalid integer value for conversion to EventType");
                }
            }           
        }
    }

    abstract public void process(WatchedEvent event);
}

