package org.javenstudio.raptor.paxos.server.quorum;

import java.io.Flushable;
import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.paxos.PaxosDefs.OpCode;
import org.javenstudio.raptor.paxos.server.Request;
import org.javenstudio.raptor.paxos.server.RequestProcessor;

public class SendAckRequestProcessor implements RequestProcessor, Flushable {
    private static final Logger LOG = Logger.getLogger(SendAckRequestProcessor.class);
    
    Learner learner;

    SendAckRequestProcessor(Learner peer) {
        this.learner = peer;
    }

    public void processRequest(Request si) {
        if(si.type != OpCode.sync){
            QuorumPacket qp = new QuorumPacket(Leader.ACK, si.hdr.getZxid(), null,
                null);
            try {
                learner.writePacket(qp, false);
            } catch (IOException e) {
                LOG.warn("Closing connection to leader, exception during packet send", e);
                try {
                    if (!learner.sock.isClosed()) {
                        learner.sock.close();
                    }
                } catch (IOException e1) {
                    // Nothing to do, we are shutting things down, so an exception here is irrelevant
                    LOG.debug("Ignoring error closing the connection", e1);
                }
            }
        }
    }
    
    public void flush() throws IOException {
        try {
            learner.writePacket(null, true);
        } catch(IOException e) {
            LOG.warn("Closing connection to leader, exception during packet send", e);
            try {
                if (!learner.sock.isClosed()) {
                    learner.sock.close();
                }
            } catch (IOException e1) {
                    // Nothing to do, we are shutting things down, so an exception here is irrelevant
                    LOG.debug("Ignoring error closing the connection", e1);
            }
        }
    }

    public void shutdown() {
        // Nothing needed
    }

}

