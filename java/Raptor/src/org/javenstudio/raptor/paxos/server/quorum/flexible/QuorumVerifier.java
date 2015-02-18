package org.javenstudio.raptor.paxos.server.quorum.flexible;

import java.util.HashSet;

/**
 * All quorum validators have to implement a method called
 * containsQuorum, which verifies if a HashSet of server 
 * identifiers constitutes a quorum.
 *
 */

public interface QuorumVerifier {
    long getWeight(long id);
    boolean containsQuorum(HashSet<Long> set);
}
