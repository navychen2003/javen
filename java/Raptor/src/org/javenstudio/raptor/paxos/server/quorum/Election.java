package org.javenstudio.raptor.paxos.server.quorum;


import org.javenstudio.raptor.paxos.server.quorum.Vote;

public interface Election {
    public Vote lookForLeader() throws InterruptedException;
    public void shutdown();
}
