package org.javenstudio.raptor.paxos.server;

import org.javenstudio.raptor.paxos.server.quorum.Observer;
import org.javenstudio.raptor.paxos.server.quorum.ObserverMXBean;

/**
 * ObserverBean
 *
 */
public class ObserverBean extends PaxosServerBean implements ObserverMXBean{

    private Observer observer;
    
    public ObserverBean(Observer observer, PaxosServer zks) {
        super(zks);        
        this.observer = observer;
    }

    public int getPendingRevalidationCount() {
       return this.observer.getPendingRevalidationsCount(); 
    }

    public String getQuorumAddress() {
        return observer.getSocket().toString();
    }

}

