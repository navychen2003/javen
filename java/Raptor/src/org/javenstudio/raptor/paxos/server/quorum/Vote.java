package org.javenstudio.raptor.paxos.server.quorum;

import org.javenstudio.raptor.paxos.server.quorum.QuorumPeer.ServerState;


public class Vote {
    public Vote(long id, long zxid) {
        this.id = id;
        this.zxid = zxid;
    }

    public Vote(long id, long zxid, long epoch) {
        this.id = id;
        this.zxid = zxid;
        this.epoch = epoch;
    }
    
    public Vote(long id, long zxid, long epoch, ServerState state) {
        this.id = id;
        this.zxid = zxid;
        this.epoch = epoch;
        this.state = state;
    }
    
    public long id;
    
    public long zxid;
    
    public long epoch = -1;
    
    public ServerState state = ServerState.LOOKING;
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vote)) {
            return false;
        }
        Vote other = (Vote) o;
        return (id == other.id && zxid == other.zxid && epoch == other.epoch);

    }

    @Override
    public int hashCode() {
        return (int) (id & zxid);
    }

    public String toString() {
        return "(" + id + ", " + Long.toHexString(zxid) + ")";
    }
}
