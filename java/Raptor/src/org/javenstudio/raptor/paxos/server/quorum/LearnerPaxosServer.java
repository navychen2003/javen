package org.javenstudio.raptor.paxos.server.quorum;

import java.io.IOException;
import java.util.HashMap;

import org.javenstudio.raptor.paxos.jmx.MBeanRegistry;
import org.javenstudio.raptor.paxos.server.DataTreeBean;
import org.javenstudio.raptor.paxos.server.ServerCnxn;
import org.javenstudio.raptor.paxos.server.PaxosDatabase;
import org.javenstudio.raptor.paxos.server.PaxosServerBean;
import org.javenstudio.raptor.paxos.server.persistence.FileTxnSnapLog;

/**
 * Parent class for all PaxosServers for Learners 
 */
public abstract class LearnerPaxosServer extends QuorumPaxosServer {    
    public LearnerPaxosServer(FileTxnSnapLog logFactory, int tickTime,
            int minSessionTimeout, int maxSessionTimeout,
            DataTreeBuilder treeBuilder, PaxosDatabase zkDb, QuorumPeer self)
        throws IOException
    {
        super(logFactory, tickTime, minSessionTimeout, maxSessionTimeout,
                treeBuilder, zkDb, self);
    }

    /**
     * Abstract method to return the learner associated with this server.
     * Since the Learner may change under our feet (when QuorumPeer reassigns
     * it) we can't simply take a reference here. Instead, we need the 
     * subclasses to implement this.     
     */
    abstract public Learner getLearner();        
    
    /**
     * Returns the current state of the session tracker. This is only currently
     * used by a Learner to build a ping response packet.
     * 
     */
    protected HashMap<Long, Integer> getTouchSnapshot() {
        if (sessionTracker != null) {
            return ((LearnerSessionTracker) sessionTracker).snapshot();
        }
        return new HashMap<Long, Integer>();
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
    protected void createSessionTracker() {
        sessionTracker = new LearnerSessionTracker(this, getPaxosDatabase().getSessionWithTimeOuts(),
                self.getId());
    }
    
    @Override
    protected void revalidateSession(ServerCnxn cnxn, long sessionId,
            int sessionTimeout) throws IOException, InterruptedException {
        getLearner().validateSession(cnxn, sessionId, sessionTimeout);
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

    public void registerJMX(PaxosServerBean serverBean,
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
            jmxServerBean = serverBean;
            MBeanRegistry.getInstance().register(serverBean, localPeerBean);
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

    protected void unregisterJMX(Learner peer) {
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
}

