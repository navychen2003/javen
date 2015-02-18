package org.javenstudio.raptor.paxos.server.quorum;

import java.util.concurrent.LinkedBlockingQueue;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.PaxosDefs.OpCode;
import org.javenstudio.raptor.paxos.server.RequestProcessor;
import org.javenstudio.raptor.paxos.server.Request;
import org.javenstudio.raptor.paxos.server.PaxosTrace;

/**
 * This RequestProcessor forwards any requests that modify the state of the
 * system to the Leader.
 */
public class ObserverRequestProcessor extends Thread implements
        RequestProcessor {
    private static final Logger LOG = Logger.getLogger(ObserverRequestProcessor.class);

    ObserverPaxosServer zks;

    RequestProcessor nextProcessor;

    // We keep a queue of requests. As requests get submitted they are 
    // stored here. The queue is drained in the run() method. 
    LinkedBlockingQueue<Request> queuedRequests = new LinkedBlockingQueue<Request>();

    boolean finished = false;

    /**
     * Constructor - takes an ObserverPaxosServer to associate with
     * and the next processor to pass requests to after we're finished. 
     * @param zks
     * @param nextProcessor
     */
    public ObserverRequestProcessor(ObserverPaxosServer zks,
            RequestProcessor nextProcessor) {
        super("ObserverRequestProcessor:" + zks.getServerId());
        this.zks = zks;
        this.nextProcessor = nextProcessor;
    }

    @Override
    public void run() {
        try {
            while (!finished) {
                Request request = queuedRequests.take();
                if (LOG.isTraceEnabled()) {
                    PaxosTrace.logRequest(LOG, PaxosTrace.CLIENT_REQUEST_TRACE_MASK,
                            'F', request, "");
                }
                if (request == Request.requestOfDeath) {
                    break;
                }
                // We want to queue the request to be processed before we submit
                // the request to the leader so that we are ready to receive
                // the response
                nextProcessor.processRequest(request);
                
                // We now ship the request to the leader. As with all
                // other quorum operations, sync also follows this code
                // path, but different from others, we need to keep track
                // of the sync operations this Observer has pending, so we
                // add it to pendingSyncs.
                switch (request.type) {
                case OpCode.sync:
                    zks.pendingSyncs.add(request);
                    zks.getObserver().request(request);
                    break;
                case OpCode.create:
                case OpCode.delete:
                case OpCode.setData:
                case OpCode.setACL:
                case OpCode.createSession:
                case OpCode.closeSession:
                    zks.getObserver().request(request);
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("Unexpected exception causing exit", e);
        }
        LOG.info("ObserverRequestProcessor exited loop!");
    }

    /**
     * Simply queue the request, which will be processed in FIFO order. 
     */
    public void processRequest(Request request) {
        if (!finished) {
            queuedRequests.add(request);
        }
    }

    /**
     * Shutdown the processor.
     */
    public void shutdown() {
        finished = true;
        queuedRequests.clear();
        queuedRequests.add(Request.requestOfDeath);
        nextProcessor.shutdown();
    }

}

