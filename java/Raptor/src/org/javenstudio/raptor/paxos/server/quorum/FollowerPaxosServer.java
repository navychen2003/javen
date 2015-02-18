package org.javenstudio.raptor.paxos.server.quorum;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.server.FinalRequestProcessor;
import org.javenstudio.raptor.paxos.server.Request;
import org.javenstudio.raptor.paxos.server.RequestProcessor;
import org.javenstudio.raptor.paxos.server.SyncRequestProcessor;
import org.javenstudio.raptor.paxos.server.PaxosDatabase;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnSnapLog;
import org.javenstudio.raptor.paxos.txn.TxnHeader;

/**
 * Just like the standard PaxosServer. We just replace the request
 * processors: FollowerRequestProcessor -> CommitProcessor ->
 * FinalRequestProcessor
 * 
 * A SyncRequestProcessor is also spawned off to log proposals from the leader.
 */
public class FollowerPaxosServer extends LearnerPaxosServer {
    private static final Logger LOG =
    		Logger.getLogger(FollowerPaxosServer.class);

    CommitProcessor commitProcessor;

    SyncRequestProcessor syncProcessor;

    /*
     * Pending sync requests
     */
    ConcurrentLinkedQueue<Request> pendingSyncs;
    
    /**
     * @param port
     * @param dataDir
     * @throws IOException
     */
    FollowerPaxosServer(FileTxnSnapLog logFactory,QuorumPeer self,
            DataTreeBuilder treeBuilder, PaxosDatabase zkDb) throws IOException {
        super(logFactory, self.tickTime, self.minSessionTimeout,
                self.maxSessionTimeout, treeBuilder, zkDb, self);
        this.pendingSyncs = new ConcurrentLinkedQueue<Request>();
    }

    public Follower getFollower(){
        return self.follower;
    }      

    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        commitProcessor = new CommitProcessor(finalProcessor,
                Long.toString(getServerId()), true);
        commitProcessor.start();
        firstProcessor = new FollowerRequestProcessor(this, commitProcessor);
        ((FollowerRequestProcessor) firstProcessor).start();
        syncProcessor = new SyncRequestProcessor(this,
                new SendAckRequestProcessor((Learner)getFollower()));
        syncProcessor.start();
    }
    
    LinkedBlockingQueue<Request> pendingTxns = new LinkedBlockingQueue<Request>();

    public void logRequest(TxnHeader hdr, Writable txn) {
        Request request = new Request(null, hdr.getClientId(), hdr.getCxid(),
                hdr.getType(), null, null);
        request.hdr = hdr;
        request.txn = txn;
        request.zxid = hdr.getZxid();
        if ((request.zxid & 0xffffffffL) != 0) {
            pendingTxns.add(request);
        }
        syncProcessor.processRequest(request);
    }

    /**
     * When a COMMIT message is received, eventually this method is called, 
     * which matches up the zxid from the COMMIT with (hopefully) the head of
     * the pendingTxns queue and hands it to the commitProcessor to commit.
     * @param zxid - must correspond to the head of pendingTxns if it exists
     */
    public void commit(long zxid) {
        if (pendingTxns.size() == 0) {
            LOG.warn("Committing " + Long.toHexString(zxid)
                    + " without seeing txn");
            return;
        }
        long firstElementZxid = pendingTxns.element().zxid;
        if (firstElementZxid != zxid) {
            LOG.fatal("Committing zxid 0x" + Long.toHexString(zxid)
                    + " but next pending txn 0x"
                    + Long.toHexString(firstElementZxid));
            System.exit(12);
        }
        Request request = pendingTxns.remove();
        commitProcessor.commit(request);
    }
    
    synchronized public void sync(){
        if(pendingSyncs.size() ==0){
            LOG.warn("Not expecting a sync.");
            return;
        }
                
        Request r = pendingSyncs.remove();
		commitProcessor.commit(r);
    }
             
    @Override
    public int getGlobalOutstandingLimit() {
        return super.getGlobalOutstandingLimit() / (self.getQuorumSize() - 1);
    }
    
    @Override
    public void shutdown() {
        try {
            super.shutdown();
        } catch (Exception e) {
            LOG.warn("Ignoring unexpected exception during shutdown", e);
        }
        try {
            if (syncProcessor != null) {
                syncProcessor.shutdown();
            }
        } catch (Exception e) {
            LOG.warn("Ignoring unexpected exception in syncprocessor shutdown",
                    e);
        }
    }
    
    @Override
    public String getState() {
        return "follower";
    }

    @Override
    public Learner getLearner() {
        return getFollower();
    }
}

