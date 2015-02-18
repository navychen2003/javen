package org.javenstudio.raptor.paxos.server.quorum;

import java.io.IOException;
import java.io.PrintWriter;

import org.javenstudio.raptor.paxos.server.PaxosDatabase;
import org.javenstudio.raptor.paxos.server.PaxosServer;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnSnapLog;

/**
 * Abstract base class for all PaxosServers that participate in
 * a quorum.
 */
public abstract class QuorumPaxosServer extends PaxosServer {
    protected final QuorumPeer self;

    protected QuorumPaxosServer(FileTxnSnapLog logFactory, int tickTime,
            int minSessionTimeout, int maxSessionTimeout,
            DataTreeBuilder treeBuilder, PaxosDatabase zkDb, QuorumPeer self)
        throws IOException
    {
        super(logFactory, tickTime, minSessionTimeout, maxSessionTimeout,
                treeBuilder, zkDb);
        this.self = self;
    }

    @Override
    public void dumpConf(PrintWriter pwriter) {
        super.dumpConf(pwriter);

        pwriter.print("initLimit=");
        pwriter.println(self.getInitLimit());
        pwriter.print("syncLimit=");
        pwriter.println(self.getSyncLimit());
        pwriter.print("electionAlg=");
        pwriter.println(self.getElectionType());
        pwriter.print("electionPort=");
        pwriter.println(self.quorumPeers.get(self.getId()).electionAddr
                .getPort());
        pwriter.print("quorumPort=");
        pwriter.println(self.quorumPeers.get(self.getId()).addr.getPort());
        pwriter.print("peerType=");
        pwriter.println(self.getLearnerType().ordinal());
    }
}

