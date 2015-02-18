package org.javenstudio.raptor.paxos.server;

import java.io.PrintWriter;

import org.javenstudio.raptor.paxos.PaxosException;
import org.javenstudio.raptor.paxos.PaxosException.SessionExpiredException;
import org.javenstudio.raptor.paxos.PaxosException.SessionMovedException;

/**
 * This is the basic interface that PaxosServer uses to track sessions. The
 * standalone and leader PaxosServer use the same SessionTracker. The
 * FollowerPaxosServer uses a SessionTracker which is basically a simple
 * shell to track information to be forwarded to the leader.
 */
public interface SessionTracker {
    public static interface Session {
        long getSessionId();
        int getTimeout();
    }
    public static interface SessionExpirer {
        void expire(Session session);

        long getServerId();
    }

    long createSession(int sessionTimeout);

    void addSession(long id, int to);

    /**
     * @param sessionId
     * @param sessionTimeout
     * @return false if session is no longer active
     */
    boolean touchSession(long sessionId, int sessionTimeout);

    /**
     * 
     */
    void shutdown();

    /**
     * @param sessionId
     */
    void removeSession(long sessionId);

    void checkSession(long sessionId, Object owner) throws PaxosException.SessionExpiredException, SessionMovedException;

    void setOwner(long id, Object owner) throws SessionExpiredException;

    /**
     * Text dump of session information, suitable for debugging.
     * @param pwriter the output writer
     */
    void dumpSessions(PrintWriter pwriter);
}

