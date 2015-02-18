package org.javenstudio.raptor.paxos.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;

import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.data.Id;
import org.javenstudio.raptor.paxos.proto.ReplyHeader;

/**
 * Interface to a Server connection - represents a connection from a client
 * to the server.
 */
public interface ServerCnxn extends Watcher {
    // This is just an arbitrary object to represent requests issued by
    // (aka owned by) this class
    final public static Object me = new Object();

    int getSessionTimeout();

    void sendResponse(ReplyHeader h, Writable r, String tag) throws IOException;

    /* notify the client the session is closing and close/cleanup socket */
    void sendCloseSession();

    void finishSessionInit(boolean valid);

    void process(WatchedEvent event);

    long getSessionId();

    void setSessionId(long sessionId);

    ArrayList<Id> getAuthInfo();

    InetSocketAddress getRemoteAddress();

    /**
     * Statistics on the ServerCnxn
     */
    interface Stats {
        /** Date/time the connection was established
         * @since 3.3.0 */
        Date getEstablished();

        /**
         * The number of requests that have been submitted but not yet
         * responded to.
         */
        long getOutstandingRequests();
        /** Number of packets received */
        long getPacketsReceived();
        /** Number of packets sent (incl notifications) */
        long getPacketsSent();
        /** Min latency in ms
         * @since 3.3.0 */
        long getMinLatency();
        /** Average latency in ms
         * @since 3.3.0 */
        long getAvgLatency();
        /** Max latency in ms
         * @since 3.3.0 */
        long getMaxLatency();
        /** Last operation performed by this connection
         * @since 3.3.0 */
        String getLastOperation();
        /** Last cxid of this connection
         * @since 3.3.0 */
        long getLastCxid();
        /** Last zxid of this connection
         * @since 3.3.0 */
        long getLastZxid();
        /** Last time server sent a response to client on this connection
         * @since 3.3.0 */
        long getLastResponseTime();
        /** Latency of last response to client on this connection in ms
         * @since 3.3.0 */
        long getLastLatency();

        /** Reset counters
         * @since 3.3.0 */
        void reset();
    }

    Stats getStats();
}

