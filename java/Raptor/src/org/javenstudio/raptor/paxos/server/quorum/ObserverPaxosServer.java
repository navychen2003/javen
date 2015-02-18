package org.javenstudio.raptor.paxos.server.quorum;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.server.FinalRequestProcessor;
import org.javenstudio.raptor.paxos.server.Request;
import org.javenstudio.raptor.paxos.server.RequestProcessor;
import org.javenstudio.raptor.paxos.server.SyncRequestProcessor;
import org.javenstudio.raptor.paxos.server.PaxosDatabase;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnSnapLog;

/**
 * A PaxosServer for the Observer node type. Not much is different, but
 * we anticipate specializing the request processors in the future. 
 *
 */
public class ObserverPaxosServer extends LearnerPaxosServer {
    private static final Logger LOG =
    		Logger.getLogger(ObserverPaxosServer.class);        
    
    /*
     * Request processors
     */
    private CommitProcessor commitProcessor;
    private SyncRequestProcessor syncProcessor;
    
    /*
     * Pending sync requests
     */
    ConcurrentLinkedQueue<Request> pendingSyncs = 
        new ConcurrentLinkedQueue<Request>();
        
    ObserverPaxosServer(FileTxnSnapLog logFactory, QuorumPeer self,
            DataTreeBuilder treeBuilder, PaxosDatabase zkDb) throws IOException {
        super(logFactory, self.tickTime, self.minSessionTimeout,
                self.maxSessionTimeout, treeBuilder, zkDb, self);
    }
    
    public Observer getObserver() {
        return self.observer;
    }
    
    @Override
    public Learner getLearner() {
        return self.observer;
    }       
    
    /**
     * Unlike a Follower, which sees a full request only during the PROPOSAL
     * phase, Observers get all the data required with the INFORM packet. 
     * This method commits a request that has been unpacked by from an INFORM
     * received from the Leader. 
     *      
     * @param request
     */
    public void commitRequest(Request request) {     
        commitProcessor.commit(request);        
    }
    
    /**
     * Set up the request processors for an Observer:
     * firstProcesor->commitProcessor->finalProcessor
     */
    @Override
    protected void setupRequestProcessors() {      
        // We might consider changing the processor behaviour of 
        // Observers to, for example, remove the disk sync requirements.
        // Currently, they behave almost exactly the same as followers.
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        commitProcessor = new CommitProcessor(finalProcessor,
                Long.toString(getServerId()), true);
        commitProcessor.start();
        firstProcessor = new ObserverRequestProcessor(this, commitProcessor);
        ((ObserverRequestProcessor) firstProcessor).start();
        syncProcessor = new SyncRequestProcessor(this,
                new SendAckRequestProcessor(getObserver()));
        syncProcessor.start();
    }
    
    /*
     * Process a sync request
     */
    synchronized public void sync(){
        if(pendingSyncs.size() ==0){
            LOG.warn("Not expecting a sync.");
            return;
        }
                
        Request r = pendingSyncs.remove();
        commitProcessor.commit(r);
    }
    
    @Override
    public String getState() {
        return "observer";
    };    
}

