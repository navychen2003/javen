package org.javenstudio.raptor.paxos.server;

/**
 * RequestProcessors are chained together to process transactions. Requests are
 * always processed in order. The standalone server, follower, and leader all
 * have slightly different RequestProcessors changed together.
 * 
 * Requests always move forward through the chain of RequestProcessors. Requests
 * are passed to a RequestProcessor through processRequest(). Generally method
 * will always be invoked by a single thread.
 * 
 * When shutdown is called, the request RequestProcessor should also shutdown
 * any RequestProcessors that it is connected to.
 */
public interface RequestProcessor {
    void processRequest(Request request);

    void shutdown();
}

