package org.javenstudio.raptor.paxos.server;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.server.quorum.QuorumPacket;

/**
 * This class encapsulates and centralizes tracing for the Paxos server.
 * Trace messages go to the log with TRACE level.
 * <p>
 * Log4j must be correctly configured to capture the TRACE messages.
 */
public class PaxosTrace {
    final static public long CLIENT_REQUEST_TRACE_MASK = 1 << 1;

    final static public long CLIENT_DATA_PACKET_TRACE_MASK = 1 << 2;

    final static public long CLIENT_PING_TRACE_MASK = 1 << 3;

    final static public long SERVER_PACKET_TRACE_MASK = 1 << 4;

    final static public long SESSION_TRACE_MASK = 1 << 5;

    final static public long EVENT_DELIVERY_TRACE_MASK = 1 << 6;

    final static public long SERVER_PING_TRACE_MASK = 1 << 7;

    final static public long WARNING_TRACE_MASK = 1 << 8;

    final static public long JMX_TRACE_MASK = 1 << 9;

    private static long traceMask = CLIENT_REQUEST_TRACE_MASK
            | SERVER_PACKET_TRACE_MASK | SESSION_TRACE_MASK
            | WARNING_TRACE_MASK;

    public static long getTextTraceLevel() {
        return traceMask;
    }

    public static void setTextTraceLevel(long mask) {
        traceMask = mask;
        Logger LOG = Logger.getLogger(PaxosTrace.class);
        LOG.info("Set text trace mask to 0x" + Long.toHexString(mask));
    }

    public static boolean isTraceEnabled(Logger log, long mask) {
        return log.isTraceEnabled() && (mask & traceMask) != 0;
    }

    public static void logTraceMessage(Logger log, long mask, String msg) {
        if (isTraceEnabled(log, mask)) {
            log.trace(msg);
        }
    }

    static public void logQuorumPacket(Logger log, long mask,
            char direction, QuorumPacket qp) {
        return;

        // if (isTraceEnabled(log, mask)) {
        // logTraceMessage(LOG, mask, direction + " "
        // + FollowerHandler.packetToString(qp));
        // }
    }

    static public void logRequest(Logger log, long mask,
            char rp, Request request, String header) {
        if (isTraceEnabled(log, mask)) {
            log.trace(header + ":" + rp + request.toString());
        }
    }
}

