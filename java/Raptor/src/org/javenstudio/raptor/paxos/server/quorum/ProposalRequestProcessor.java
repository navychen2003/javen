package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.Request;
import org.javenstudio.raptor.paxos.server.RequestProcessor;
import org.javenstudio.raptor.paxos.server.SyncRequestProcessor;

/**
 * This RequestProcessor simply forwards requests to an AckRequestProcessor and
 * SyncRequestProcessor.
 */
public class ProposalRequestProcessor implements RequestProcessor {
    LeaderPaxosServer zks;
    
    RequestProcessor nextProcessor;

    SyncRequestProcessor syncProcessor;

    public ProposalRequestProcessor(LeaderPaxosServer zks,
            RequestProcessor nextProcessor) {
        this.zks = zks;
        this.nextProcessor = nextProcessor;
        AckRequestProcessor ackProcessor = new AckRequestProcessor(zks.getLeader());
        syncProcessor = new SyncRequestProcessor(zks, ackProcessor);
    }
    
    /**
     * initialize this processor
     */
    public void initialize() {
        syncProcessor.start();
    }
    
    public void processRequest(Request request) {
        // LOG.warn("Ack>>> cxid = " + request.cxid + " type = " +
        // request.type + " id = " + request.sessionId);
        // request.addRQRec(">prop");
                
        
        /* In the following IF-THEN-ELSE block, we process syncs on the leader. 
         * If the sync is coming from a follower, then the follower
         * handler adds it to syncHandler. Otherwise, if it is a client of
         * the leader that issued the sync command, then syncHandler won't 
         * contain the handler. In this case, we add it to syncHandler, and 
         * call processRequest on the next processor.
         */
        
        if(request instanceof LearnerSyncRequest){
            zks.getLeader().processSync((LearnerSyncRequest)request);
        } else {
                nextProcessor.processRequest(request);
            if (request.hdr != null) {
                // We need to sync and get consensus on any transactions
                zks.getLeader().propose(request);
                syncProcessor.processRequest(request);
            }
        }
    }

    public void shutdown() {
        nextProcessor.shutdown();
        syncProcessor.shutdown();
    }

}

