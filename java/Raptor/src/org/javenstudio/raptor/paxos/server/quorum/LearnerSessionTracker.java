package org.javenstudio.raptor.paxos.server.quorum;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.javenstudio.raptor.paxos.server.SessionTracker;
import org.javenstudio.raptor.paxos.server.SessionTrackerImpl;

/**
 * This is really just a shell of a SessionTracker that tracks session activity
 * to be forwarded to the Leader using a PING.
 */
public class LearnerSessionTracker implements SessionTracker {
    SessionExpirer expirer;

    HashMap<Long, Integer> touchTable = new HashMap<Long, Integer>();
    long serverId = 1;
    long nextSessionId=0;
    
    private ConcurrentHashMap<Long, Integer> sessionsWithTimeouts;

    public LearnerSessionTracker(SessionExpirer expirer,
            ConcurrentHashMap<Long, Integer> sessionsWithTimeouts, long id) {
        this.expirer = expirer;
        this.sessionsWithTimeouts = sessionsWithTimeouts;
        this.serverId = id;
        nextSessionId = SessionTrackerImpl.initializeNextSession(this.serverId);
        
    }

    synchronized public void removeSession(long sessionId) {
        sessionsWithTimeouts.remove(sessionId);
        touchTable.remove(sessionId);
    }

    public void shutdown() {
    }

    synchronized public void addSession(long sessionId, int sessionTimeout) {
        sessionsWithTimeouts.put(sessionId, sessionTimeout);
        touchTable.put(sessionId, sessionTimeout);
    }

    synchronized public boolean touchSession(long sessionId, int sessionTimeout) {
        touchTable.put(sessionId, sessionTimeout);
        return true;
    }

    synchronized HashMap<Long, Integer> snapshot() {
        HashMap<Long, Integer> oldTouchTable = touchTable;
        touchTable = new HashMap<Long, Integer>();
        return oldTouchTable;
    }


    synchronized public long createSession(int sessionTimeout) {
        return (nextSessionId++);
    }

    public void checkSession(long sessionId, Object owner)  {
        // Nothing to do here. Sessions are checked at the Leader
    }
    
    public void setOwner(long sessionId, Object owner) {
        // Nothing to do here. Sessions are checked at the Leader
    }

    public void dumpSessions(PrintWriter pwriter) {
    	// the original class didn't have tostring impl, so just
    	// dup what we had before
    	pwriter.println(toString());
    }
}

