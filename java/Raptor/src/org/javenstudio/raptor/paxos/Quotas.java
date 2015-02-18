package org.javenstudio.raptor.paxos;

/**
 * this class manages quotas
 * and has many other utils
 * for quota
 */
public class Quotas {

    /** the paxos nodes that acts as the management and status node **/
    public static final String procPaxos = "/paxos";

    /** the paxos quota node that acts as the quota
     * management node for paxos */
    public static final String quotaPaxos = "/paxos/quota";

    /**
     * the limit node that has the limit of
     * a subtree
     */
    public static final String limitNode = "paxos_limits";

    /**
     * the stat node that monitors the limit of
     * a subtree.
     */
    public static final String statNode = "paxos_stats";

    /**
     * return the quota path associated with this
     * prefix
     * @param path the actual path in paxos.
     * @return the limit quota path
     */
    public static String quotaPath(String path) {
        return quotaPaxos + path +
        "/" + limitNode;
    }

    /**
     * return the stat quota path associated with this
     * prefix.
     * @param path the actual path in paxos
     * @return the stat quota path
     */
    public static String statPath(String path) {
        return quotaPaxos + path + "/" +
        statNode;
    }
}

