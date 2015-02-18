package org.javenstudio.raptor.paxos.server.quorum;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.DataInput; 
import java.io.DataInputStream; 
import java.net.InetSocketAddress;

import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.server.ObserverBean;
import org.javenstudio.raptor.paxos.server.Request;
import org.javenstudio.raptor.paxos.server.util.SerializeUtils;
import org.javenstudio.raptor.paxos.txn.TxnHeader;

/**
 * Observers are peers that do not take part in the atomic broadcast protocol.
 * Instead, they are informed of successful proposals by the Leader. Observers
 * therefore naturally act as a relay point for publishing the proposal stream
 * and can relieve Followers of some of the connection load. Observers may
 * submit proposals, but do not vote in their acceptance. 
 *
 * See ZOOKEEPER-368 for a discussion of this feature. 
 */
public class Observer extends Learner{      

    Observer(QuorumPeer self,ObserverPaxosServer observerPaxosServer) {
        this.self = self;
        this.zk=observerPaxosServer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Observer ").append(sock);        
        sb.append(" pendingRevalidationCount:")
            .append(pendingRevalidations.size());
        return sb.toString();
    }
    
    /**
     * the main method called by the observer to observe the leader
     *
     * @throws InterruptedException
     */
    void observeLeader() throws InterruptedException {
        zk.registerJMX(new ObserverBean(this, zk), self.jmxLocalPeerBean);

        try {
            InetSocketAddress addr = findLeader();
            LOG.info("Observing " + addr);
            try {
                connectToLeader(addr);
                long newLeaderZxid = registerWithLeader(Leader.OBSERVERINFO);
                
                syncWithLeader(newLeaderZxid);
                QuorumPacket qp = new QuorumPacket();
                while (self.isRunning()) {
                    readPacket(qp);
                    processPacket(qp);                   
                }
            } catch (IOException e) {
                LOG.warn("Exception when observing the leader", e);
                try {
                    sock.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
    
                synchronized (pendingRevalidations) {
                    // clear pending revalidations
                    pendingRevalidations.clear();
                    pendingRevalidations.notifyAll();
                }
            }
        } finally {
            zk.unregisterJMX(this);
        }
    }
    
    /**
     * Controls the response of an observer to the receipt of a quorumpacket
     * @param qp
     * @throws IOException
     */
    protected void processPacket(QuorumPacket qp) throws IOException{
        switch (qp.getType()) {
        case Leader.PING:
            ping(qp);
            break;
        case Leader.PROPOSAL:
            LOG.warn("Ignoring proposal");
            break;
        case Leader.COMMIT:
            LOG.warn("Ignoring commit");            
            break;            
        case Leader.UPTODATE:
            zk.takeSnapshot();
            self.cnxnFactory.setPaxosServer(zk);
            break;
        case Leader.REVALIDATE:
            revalidate(qp);
            break;
        case Leader.SYNC:
            ((ObserverPaxosServer)zk).sync();
            break;
        case Leader.INFORM:            
            TxnHeader hdr = new TxnHeader();
            DataInput ia = new DataInputStream(new ByteArrayInputStream(qp.getData()));
            Writable txn = SerializeUtils.deserializeTxn(ia, hdr);
            Request request = new Request (null, hdr.getClientId(), 
                                           hdr.getCxid(),
                                           hdr.getType(), null, null);
            request.txn = txn;
            request.hdr = hdr;
            ObserverPaxosServer obs = (ObserverPaxosServer)zk;
            obs.commitRequest(request);            
            break;
        }
    }

    /**
     * Shutdown the Observer.
     */
    public void shutdown() {       
        LOG.info("shutdown called", new Exception("shutdown Observer"));
        super.shutdown();
    }
}
