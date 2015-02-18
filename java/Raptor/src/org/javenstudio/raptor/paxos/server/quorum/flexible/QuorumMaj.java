package org.javenstudio.raptor.paxos.server.quorum.flexible;

import java.util.HashSet;


/**
 * This class implements a validator for majority quorums. The 
 * implementation is straightforward.
 *
 */
public class QuorumMaj implements QuorumVerifier {
    int half;
    
    /**
     * Defines a majority to avoid computing it every time.
     * 
     * @param n number of servers
     */
    public QuorumMaj(int n){
        this.half = n/2;
    }
    
    /**
     * Returns weight of 1 by default.
     * 
     * @param id 
     */
    public long getWeight(long id){
        return (long) 1;
    }
    
    /**
     * Verifies if a set is a majority.
     */
    public boolean containsQuorum(HashSet<Long> set){
        return (set.size() > half);
    }
    
}
