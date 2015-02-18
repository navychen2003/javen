package org.javenstudio.raptor.paxos.server.persistence;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.javenstudio.raptor.paxos.server.DataTree;

/**
 * snapshot interface for the persistence layer.
 * implement this interface for implementing 
 * snapshots.
 */
public interface SnapShot {
    
    /**
     * deserialize a data tree from the last valid snapshot and 
     * return the last zxid that was deserialized
     * @param dt the datatree to be deserialized into
     * @param sessions the sessions to be deserialized into
     * @return the last zxid that was deserialized from the snapshot
     * @throws IOException
     */
    long deserialize(DataTree dt, Map<Long, Integer> sessions) 
        throws IOException;
    
    /**
     * persist the datatree and the sessions into a persistence storage
     * @param dt the datatree to be serialized
     * @param sessions 
     * @throws IOException
     */
    void serialize(DataTree dt, Map<Long, Integer> sessions, 
            File name) 
        throws IOException;
    
    /**
     * find the most recent snapshot file
     * @return the most recent snapshot file
     * @throws IOException
     */
    File findMostRecentSnapshot() throws IOException;
    
    /**
     * free resources from this snapshot immediately
     * @throws IOException
     */
    void close() throws IOException;
} 
