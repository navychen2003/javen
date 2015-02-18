package org.javenstudio.raptor.paxos.server.quorum;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.DataInput; 
import java.io.DataInputStream; 
import java.net.InetSocketAddress;

import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.server.util.SerializeUtils;
import org.javenstudio.raptor.paxos.txn.TxnHeader;

/**
 * This class has the control logic for the Follower.
 */
public class Follower extends Learner {

    private long lastQueued;
    // This is the same object as this.zk, but we cache the downcast op
    final FollowerPaxosServer fzk;
    
    Follower(QuorumPeer self, FollowerPaxosServer zk) {
        this.self = self;
        this.zk=zk;
        this.fzk = zk;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Follower ").append(sock);
        sb.append(" lastQueuedZxid:").append(lastQueued);
        sb.append(" pendingRevalidationCount:")
            .append(pendingRevalidations.size());
        return sb.toString();
    }

    /**
     * the main method called by the follower to follow the leader
     *
     * @throws InterruptedException
     */
    void followLeader() throws InterruptedException {
        fzk.registerJMX(new FollowerBean(this, zk), self.jmxLocalPeerBean);
        try {            
            InetSocketAddress addr = findLeader();            
            try {
                connectToLeader(addr);
                long newLeaderZxid = registerWithLeader(Leader.FOLLOWERINFO);
                //check to see if the leader zxid is lower than ours
                //this should never happen but is just a safety check
                long lastLoggedZxid = self.getLastLoggedZxid();
                if ((newLeaderZxid >> 32L) < (lastLoggedZxid >> 32L)) {
                    LOG.fatal("Leader epoch " + Long.toHexString(newLeaderZxid >> 32L)
                            + " is less than our epoch " + Long.toHexString(lastLoggedZxid >> 32L));
                    throw new IOException("Error: Epoch of leader is lower");
                }
                syncWithLeader(newLeaderZxid);                
                QuorumPacket qp = new QuorumPacket();
                while (self.isRunning()) {
                    readPacket(qp);
                    processPacket(qp);                   
                }                              
            } catch (IOException e) {
                LOG.warn("Exception when following the leader", e);
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
            zk.unregisterJMX((Learner)this);
        }
    }

    /**
     * Examine the packet received in qp and dispatch based on its contents.
     * @param qp
     * @throws IOException
     */
    protected void processPacket(QuorumPacket qp) throws IOException{
        switch (qp.getType()) {
        case Leader.PING:            
            ping(qp);            
            break;
        case Leader.PROPOSAL:            
            TxnHeader hdr = new TxnHeader();
            DataInput ia = new DataInputStream(new ByteArrayInputStream(qp.getData()));
            Writable txn = SerializeUtils.deserializeTxn(ia, hdr);
            if (hdr.getZxid() != lastQueued + 1) {
                LOG.warn("Got zxid 0x"
                        + Long.toHexString(hdr.getZxid())
                        + " expected 0x"
                        + Long.toHexString(lastQueued + 1));
            }
            lastQueued = hdr.getZxid();
            fzk.logRequest(hdr, txn);
            break;
        case Leader.COMMIT:
            fzk.commit(qp.getZxid());
            break;
        case Leader.UPTODATE:
            fzk.takeSnapshot();
            self.cnxnFactory.setPaxosServer(fzk);
            break;
        case Leader.REVALIDATE:
            revalidate(qp);
            break;
        case Leader.SYNC:
            fzk.sync();
            break;
        }
    }


    /**
     * The zxid of the last operation seen
     * @return zxid
     */
    public long getZxid() {
        try {
            synchronized (fzk) {
                return fzk.getZxid();
            }
        } catch (NullPointerException e) {
            LOG.warn("error getting zxid", e);
        }
        return -1;
    }
    
    /**
     * The zxid of the last operation queued
     * @return zxid
     */
    protected long getLastQueued() {
        return lastQueued;
    }

    @Override
    public void shutdown() {    
        LOG.info("shutdown called", new Exception("shutdown Follower"));
        super.shutdown();
    }
}

