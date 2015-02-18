package org.javenstudio.raptor.paxos.server.quorum;

import java.io.IOException;

import org.javenstudio.raptor.paxos.PaxosException.SessionExpiredException;
import org.javenstudio.raptor.paxos.jmx.MBeanRegistry;
import org.javenstudio.raptor.paxos.server.DataTreeBean;
import org.javenstudio.raptor.paxos.server.FinalRequestProcessor;
import org.javenstudio.raptor.paxos.server.PrepRequestProcessor;
import org.javenstudio.raptor.paxos.server.RequestProcessor;
import org.javenstudio.raptor.paxos.server.ServerCnxn;
import org.javenstudio.raptor.paxos.server.SessionTrackerImpl;
import org.javenstudio.raptor.paxos.server.PaxosDatabase;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnSnapLog;

/**
 * 
 * Just like the standard PaxosServer. We just replace the request
 * processors: PrepRequestProcessor -> ProposalRequestProcessor ->
 * CommitProcessor -> Leader.ToBeAppliedRequestProcessor ->
 * FinalRequestProcessor
 */
public class LeaderPaxosServer extends QuorumPaxosServer {
    CommitProcessor commitProcessor;

    /**
     * @param port
     * @param dataDir
     * @throws IOException
     */
    LeaderPaxosServer(FileTxnSnapLog logFactory, QuorumPeer self,
            DataTreeBuilder treeBuilder, PaxosDatabase zkDb) throws IOException {
        super(logFactory, self.tickTime, self.minSessionTimeout,
                self.maxSessionTimeout, treeBuilder, zkDb, self);
    }

    public Leader getLeader(){
        return self.leader;
    }
    
    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        RequestProcessor toBeAppliedProcessor = new Leader.ToBeAppliedRequestProcessor(
                finalProcessor, getLeader().toBeApplied);
        commitProcessor = new CommitProcessor(toBeAppliedProcessor,
                Long.toString(getServerId()), false);
        commitProcessor.start();
        ProposalRequestProcessor proposalProcessor = new ProposalRequestProcessor(this,
                commitProcessor);
        proposalProcessor.initialize();
        firstProcessor = new PrepRequestProcessor(this, proposalProcessor);
        ((PrepRequestProcessor)firstProcessor).start();
    }

    @Override
    public int getGlobalOutstandingLimit() {
        return super.getGlobalOutstandingLimit() / (self.getQuorumSize() - 1);
    }
    
    @Override
    protected void createSessionTracker() {
        sessionTracker = new SessionTrackerImpl(this, getPaxosDatabase().getSessionWithTimeOuts(),
                tickTime, self.getId());
        ((SessionTrackerImpl)sessionTracker).start();
    }


    public boolean touch(long sess, int to) {
        return sessionTracker.touchSession(sess, to);
    }

    @Override
    protected void registerJMX() {
        // register with JMX
        try {
            jmxDataTreeBean = new DataTreeBean(getPaxosDatabase().getDataTree());
            MBeanRegistry.getInstance().register(jmxDataTreeBean, jmxServerBean);
        } catch (Exception e) {
            LOG.warn("Failed to register with JMX", e);
            jmxDataTreeBean = null;
        }
    }

    public void registerJMX(LeaderBean leaderBean,
            LocalPeerBean localPeerBean)
    {
        // register with JMX
        if (self.jmxLeaderElectionBean != null) {
            try {
                MBeanRegistry.getInstance().unregister(self.jmxLeaderElectionBean);
            } catch (Exception e) {
                LOG.warn("Failed to register with JMX", e);
            }
            self.jmxLeaderElectionBean = null;
        }

        try {
            jmxServerBean = leaderBean;
            MBeanRegistry.getInstance().register(leaderBean, localPeerBean);
        } catch (Exception e) {
            LOG.warn("Failed to register with JMX", e);
            jmxServerBean = null;
        }
    }

    @Override
    protected void unregisterJMX() {
        // unregister from JMX
        try {
            if (jmxDataTreeBean != null) {
                MBeanRegistry.getInstance().unregister(jmxDataTreeBean);
            }
        } catch (Exception e) {
            LOG.warn("Failed to unregister with JMX", e);
        }
        jmxDataTreeBean = null;
    }

    protected void unregisterJMX(Leader leader) {
        // unregister from JMX
        try {
            if (jmxServerBean != null) {
                MBeanRegistry.getInstance().unregister(jmxServerBean);
            }
        } catch (Exception e) {
            LOG.warn("Failed to unregister with JMX", e);
        }
        jmxServerBean = null;
    }
    
    @Override
    public String getState() {
        return "leader";
    }

    /**
     * Returns the id of the associated QuorumPeer, which will do for a unique
     * id of this server. 
     */
    @Override
    public long getServerId() {
        return self.getId();
    }    
    
    @Override
    protected void revalidateSession(ServerCnxn cnxn, long sessionId,
        int sessionTimeout) throws IOException, InterruptedException {
        super.revalidateSession(cnxn, sessionId, sessionTimeout);
        try {
            // setowner as the leader itself, unless updated
            // via the follower handlers
            setOwner(sessionId, ServerCnxn.me);
        } catch (SessionExpiredException e) {
            // this is ok, it just means that the session revalidation failed.
        }
    }
}

