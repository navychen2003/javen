package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.server.Request;
import org.javenstudio.raptor.paxos.server.RequestProcessor;


/**
 * This is a very simple RequestProcessor that simply forwards a request from a
 * previous stage to the leader as an ACK.
 */
class AckRequestProcessor implements RequestProcessor {
    private static final Logger LOG = Logger.getLogger(AckRequestProcessor.class);
    Leader leader;

    AckRequestProcessor(Leader leader) {
        this.leader = leader;
    }

    /**
     * Forward the request as an ACK to the leader
     */
    public void processRequest(Request request) {
        QuorumPeer self = leader.self;
        if(self != null)
            leader.processAck(self.getId(), request.zxid, null);
        else
            LOG.error("Null QuorumPeer");
    }

    public void shutdown() {
        // XXX No need to do anything
    }
}

