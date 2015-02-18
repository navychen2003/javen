package org.javenstudio.raptor.paxos.server;

import java.io.DataInput; 
import java.io.DataOutput; 
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.PaxosException;
import org.javenstudio.raptor.paxos.Quotas;
import org.javenstudio.raptor.paxos.StatsTrack;
import org.javenstudio.raptor.paxos.WatchedEvent;
import org.javenstudio.raptor.paxos.Watcher;
import org.javenstudio.raptor.paxos.PaxosException.Code;
import org.javenstudio.raptor.paxos.PaxosException.NoNodeException;
import org.javenstudio.raptor.paxos.Watcher.Event;
import org.javenstudio.raptor.paxos.Watcher.Event.EventType;
import org.javenstudio.raptor.paxos.Watcher.Event.PaxosState;
import org.javenstudio.raptor.paxos.PaxosDefs.Ids;
import org.javenstudio.raptor.paxos.PaxosDefs.OpCode;
import org.javenstudio.raptor.paxos.common.PathTrie;
import org.javenstudio.raptor.paxos.data.ACL;
import org.javenstudio.raptor.paxos.data.Stat;
import org.javenstudio.raptor.paxos.data.StatPersisted;
import org.javenstudio.raptor.paxos.txn.CreateTxn;
import org.javenstudio.raptor.paxos.txn.DeleteTxn;
import org.javenstudio.raptor.paxos.txn.ErrorTxn;
import org.javenstudio.raptor.paxos.txn.SetACLTxn;
import org.javenstudio.raptor.paxos.txn.SetDataTxn;
import org.javenstudio.raptor.paxos.txn.TxnHeader;

/**
 * This class maintains the tree data structure. It doesn't have any networking
 * or client connection code in it so that it can be tested in a stand alone
 * way.
 * <p>
 * The tree maintains two parallel data structures: a hashtable that maps from
 * full paths to DataNodes and a tree of DataNodes. All accesses to a path is
 * through the hashtable. The tree is traversed only when serializing to disk.
 */
@SuppressWarnings("deprecation")
public class DataTree {
    private static final Logger LOG = Logger.getLogger(DataTree.class);

    /**
     * This hashtable provides a fast lookup to the datanodes. The tree is the
     * source of truth and is where all the locking occurs
     */
    private final ConcurrentHashMap<String, DataNode> nodes =
        new ConcurrentHashMap<String, DataNode>();

    private final WatchManager dataWatches = new WatchManager();

    private final WatchManager childWatches = new WatchManager();

    /** the root of paxos tree */
    private static final String rootPaxos = "/";

    /** the paxos nodes that acts as the management and status node **/
    private static final String procPaxos = Quotas.procPaxos;

    /** this will be the string thats stored as a child of root */
    private static final String procChildPaxos = procPaxos.substring(1);

    /**
     * the paxos quota node that acts as the quota management node for
     * paxos
     */
    private static final String quotaPaxos = Quotas.quotaPaxos;

    /** this will be the string thats stored as a child of /paxos */
    private static final String quotaChildPaxos = quotaPaxos
            .substring(procPaxos.length() + 1);

    /**
     * the path trie that keeps track fo the quota nodes in this datatree
     */
    private final PathTrie pTrie = new PathTrie();

    /**
     * This hashtable lists the paths of the ephemeral nodes of a session.
     */
    private final Map<Long, HashSet<String>> ephemerals =
        new ConcurrentHashMap<Long, HashSet<String>>();

    /**
     * this is map from longs to acl's. It saves acl's being stored for each
     * datanode.
     */
    public final Map<Long, List<ACL>> longKeyMap =
        new HashMap<Long, List<ACL>>();

    /**
     * this a map from acls to long.
     */
    public final Map<List<ACL>, Long> aclKeyMap =
        new HashMap<List<ACL>, Long>();

    /**
     * these are the number of acls that we have in the datatree
     */
    protected long aclIndex = 0;

    @SuppressWarnings("unchecked")
    public HashSet<String> getEphemerals(long sessionId) {
        HashSet<String> retv = ephemerals.get(sessionId);
        if (retv == null) {
            return new HashSet<String>();
        }
        HashSet<String> cloned = null;
        synchronized (retv) {
            cloned = (HashSet<String>) retv.clone();
        }
        return cloned;
    }

    public Map<Long, HashSet<String>> getEphemeralsMap() {
        return ephemerals;
    }

    private long incrementIndex() {
        return ++aclIndex;
    }

    /**
     * compare two list of acls. if there elements are in the same order and the
     * same size then return true else return false
     *
     * @param lista
     *            the list to be compared
     * @param listb
     *            the list to be compared
     * @return true if and only if the lists are of the same size and the
     *         elements are in the same order in lista and listb
     */
    @SuppressWarnings("unused")
	private boolean listACLEquals(List<ACL> lista, List<ACL> listb) {
        if (lista.size() != listb.size()) {
            return false;
        }
        for (int i = 0; i < lista.size(); i++) {
            ACL a = lista.get(i);
            ACL b = listb.get(i);
            if (!a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    /**
     * converts the list of acls to a list of longs.
     *
     * @param acls
     * @return a list of longs that map to the acls
     */
    public synchronized Long convertAcls(List<ACL> acls) {
        if (acls == null)
            return -1L;
        // get the value from the map
        Long ret = aclKeyMap.get(acls);
        // could not find the map
        if (ret != null)
            return ret;
        long val = incrementIndex();
        longKeyMap.put(val, acls);
        aclKeyMap.put(acls, val);
        return val;
    }

    /**
     * converts a list of longs to a list of acls.
     *
     * @param longs
     *            the list of longs
     * @return a list of ACLs that map to longs
     */
    public synchronized List<ACL> convertLong(Long longVal) {
        if (longVal == null)
            return null;
        if (longVal == -1L)
            return Ids.OPEN_ACL_UNSAFE;
        List<ACL> acls = longKeyMap.get(longVal);
        if (acls == null) {
            LOG.error("ERROR: ACL not available for long " + longVal);
            throw new RuntimeException("Failed to fetch acls for " + longVal);
        }
        return acls;
    }

    public Collection<Long> getSessions() {
        return ephemerals.keySet();
    }

    /**
     * just an accessor method to allow raw creation of datatree's from a bunch
     * of datanodes
     *
     * @param path
     *            the path of the datanode
     * @param node
     *            the datanode corresponding to this path
     */
    public void addDataNode(String path, DataNode node) {
        nodes.put(path, node);
    }

    public DataNode getNode(String path) {
        return nodes.get(path);
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getWatchCount() {
        return dataWatches.size() + childWatches.size();
    }

    /**
     * Get the size of the nodes based on path and data length.
     *
     * @return size of the data
     */
    public long approximateDataSize() {
        long result = 0;
        for (Map.Entry<String, DataNode> entry : nodes.entrySet()) {
            DataNode value = entry.getValue();
            synchronized (value) {
                result += entry.getKey().length();
                result += (value.data == null ? 0
                        : value.data.length);
            }
        }
        return result;
    }

    /**
     * This is a pointer to the root of the DataTree. It is the source of truth,
     * but we usually use the nodes hashmap to find nodes in the tree.
     */
    private DataNode root = new DataNode(null, new byte[0], -1L,
            new StatPersisted());

    /**
     * create a /paxos filesystem that is the proc filesystem of paxos
     */
    private DataNode procDataNode = new DataNode(root, new byte[0], -1L,
            new StatPersisted());

    /**
     * create a /paxos/quota node for maintaining quota properties for
     * paxos
     */
    private DataNode quotaDataNode = new DataNode(procDataNode, new byte[0],
            -1L, new StatPersisted());

    public DataTree() {
        /* Rather than fight it, let root have an alias */
        nodes.put("", root);
        nodes.put(rootPaxos, root);

        /** add the proc node and quota node */
        root.addChild(procChildPaxos);
        nodes.put(procPaxos, procDataNode);

        procDataNode.addChild(quotaChildPaxos);
        nodes.put(quotaPaxos, quotaDataNode);
    }

    /**
     * is the path one of the special paths owned by paxos.
     *
     * @param path
     *            the path to be checked
     * @return true if a special path. false if not.
     */
    boolean isSpecialPath(String path) {
        if (rootPaxos.equals(path) || procPaxos.equals(path)
                || quotaPaxos.equals(path)) {
            return true;
        }
        return false;
    }

    static public void copyStatPersisted(StatPersisted from, StatPersisted to) {
        to.setAversion(from.getAversion());
        to.setCtime(from.getCtime());
        to.setCversion(from.getCversion());
        to.setCzxid(from.getCzxid());
        to.setMtime(from.getMtime());
        to.setMzxid(from.getMzxid());
        to.setPzxid(from.getPzxid());
        to.setVersion(from.getVersion());
        to.setEphemeralOwner(from.getEphemeralOwner());
    }

    static public void copyStat(Stat from, Stat to) {
        to.setAversion(from.getAversion());
        to.setCtime(from.getCtime());
        to.setCversion(from.getCversion());
        to.setCzxid(from.getCzxid());
        to.setMtime(from.getMtime());
        to.setMzxid(from.getMzxid());
        to.setPzxid(from.getPzxid());
        to.setVersion(from.getVersion());
        to.setEphemeralOwner(from.getEphemeralOwner());
        to.setDataLength(from.getDataLength());
        to.setNumChildren(from.getNumChildren());
    }

    /**
     * update the count of this stat datanode
     *
     * @param lastPrefix
     *            the path of the node that is quotaed.
     * @param diff
     *            the diff to be added to the count
     */
    public void updateCount(String lastPrefix, int diff) {
        String statNode = Quotas.statPath(lastPrefix);
        DataNode node = nodes.get(statNode);
        StatsTrack updatedStat = null;
        if (node == null) {
            // should not happen
            LOG.error("Missing count node for stat " + statNode);
            return;
        }
        synchronized (node) {
            updatedStat = new StatsTrack(new String(node.data));
            updatedStat.setCount(updatedStat.getCount() + diff);
            node.data = updatedStat.toString().getBytes();
        }
        // now check if the counts match the quota
        String quotaNode = Quotas.quotaPath(lastPrefix);
        node = nodes.get(quotaNode);
        StatsTrack thisStats = null;
        if (node == null) {
            // should not happen
            LOG.error("Missing count node for quota " + quotaNode);
            return;
        }
        synchronized (node) {
            thisStats = new StatsTrack(new String(node.data));
        }
        if (thisStats.getCount() < updatedStat.getCount()) {
            LOG
                    .warn("Quota exceeded: " + lastPrefix + " count="
                            + updatedStat.getCount() + " limit="
                            + thisStats.getCount());
        }
    }

    /**
     * update the count of bytes of this stat datanode
     *
     * @param lastPrefix
     *            the path of the node that is quotaed
     * @param diff
     *            the diff to added to number of bytes
     * @throws IOException
     *             if path is not found
     */
    public void updateBytes(String lastPrefix, long diff) {
        String statNode = Quotas.statPath(lastPrefix);
        DataNode node = nodes.get(statNode);
        if (node == null) {
            // should never be null but just to make
            // findbugs happy
            LOG.error("Missing stat node for bytes " + statNode);
            return;
        }
        StatsTrack updatedStat = null;
        synchronized (node) {
            updatedStat = new StatsTrack(new String(node.data));
            updatedStat.setBytes(updatedStat.getBytes() + diff);
            node.data = updatedStat.toString().getBytes();
        }
        // now check if the bytes match the quota
        String quotaNode = Quotas.quotaPath(lastPrefix);
        node = nodes.get(quotaNode);
        if (node == null) {
            // should never be null but just to make
            // findbugs happy
            LOG.error("Missing quota node for bytes " + quotaNode);
            return;
        }
        StatsTrack thisStats = null;
        synchronized (node) {
            thisStats = new StatsTrack(new String(node.data));
        }
        if (thisStats.getBytes() < updatedStat.getBytes()) {
            LOG
                    .warn("Quota exceeded: " + lastPrefix + " bytes="
                            + updatedStat.getBytes() + " limit="
                            + thisStats.getBytes());
        }
    }

    /**
     * @param path
     * @param data
     * @param acl
     * @param ephemeralOwner
     *            the session id that owns this node. -1 indicates this is not
     *            an ephemeral node.
     * @param zxid
     * @param time
     * @return the patch of the created node
     * @throws PaxosException
     */
    public String createNode(String path, byte data[], List<ACL> acl,
            long ephemeralOwner, long zxid, long time)
            throws PaxosException.NoNodeException,
            PaxosException.NodeExistsException {
        int lastSlash = path.lastIndexOf('/');
        String parentName = path.substring(0, lastSlash);
        String childName = path.substring(lastSlash + 1);
        StatPersisted stat = new StatPersisted();
        stat.setCtime(time);
        stat.setMtime(time);
        stat.setCzxid(zxid);
        stat.setMzxid(zxid);
        stat.setPzxid(zxid);
        stat.setVersion(0);
        stat.setAversion(0);
        stat.setEphemeralOwner(ephemeralOwner);
        DataNode parent = nodes.get(parentName);
        if (parent == null) {
            throw new PaxosException.NoNodeException();
        }
        synchronized (parent) {
            Set<String> children = parent.getChildren();
            if (children != null) {
                if (children.contains(childName)) {
                    throw new PaxosException.NodeExistsException();
                }
            }
            int cver = parent.stat.getCversion();
            cver++;
            parent.stat.setCversion(cver);
            parent.stat.setPzxid(zxid);
            Long longval = convertAcls(acl);
            DataNode child = new DataNode(parent, data, longval, stat);
            parent.addChild(childName);
            nodes.put(path, child);
            if (ephemeralOwner != 0) {
                HashSet<String> list = ephemerals.get(ephemeralOwner);
                if (list == null) {
                    list = new HashSet<String>();
                    ephemerals.put(ephemeralOwner, list);
                }
                synchronized (list) {
                    list.add(path);
                }
            }
        }
        // now check if its one of the paxos node child
        if (parentName.startsWith(quotaPaxos)) {
            // now check if its the limit node
            if (Quotas.limitNode.equals(childName)) {
                // this is the limit node
                // get the parent and add it to the trie
                pTrie.addPath(parentName.substring(quotaPaxos.length()));
            }
            if (Quotas.statNode.equals(childName)) {
                updateQuotaForPath(parentName
                        .substring(quotaPaxos.length()));
            }
        }
        // also check to update the quotas for this node
        String lastPrefix = pTrie.findMaxPrefix(path);
        if (!rootPaxos.equals(lastPrefix) && !("".equals(lastPrefix))) {
            // ok we have some match and need to update
            updateCount(lastPrefix, 1);
            updateBytes(lastPrefix, data == null ? 0 : data.length);
        }
        dataWatches.triggerWatch(path, Event.EventType.NodeCreated);
        childWatches.triggerWatch(parentName.equals("") ? "/" : parentName,
                Event.EventType.NodeChildrenChanged);
        return path;
    }

    /**
     * remove the path from the datatree
     *
     * @param path
     *            the path to of the node to be deleted
     * @param zxid
     *            the current zxid
     * @throws PaxosException.NoNodeException
     */
    public void deleteNode(String path, long zxid)
            throws PaxosException.NoNodeException {
        int lastSlash = path.lastIndexOf('/');
        String parentName = path.substring(0, lastSlash);
        String childName = path.substring(lastSlash + 1);
        DataNode node = nodes.get(path);
        if (node == null) {
            throw new PaxosException.NoNodeException();
        }
        nodes.remove(path);
        DataNode parent = nodes.get(parentName);
        if (parent == null) {
            throw new PaxosException.NoNodeException();
        }
        synchronized (parent) {
            parent.removeChild(childName);
            parent.stat.setCversion(parent.stat.getCversion() + 1);
            parent.stat.setPzxid(zxid);
            long eowner = node.stat.getEphemeralOwner();
            if (eowner != 0) {
                HashSet<String> nodes = ephemerals.get(eowner);
                if (nodes != null) {
                    synchronized (nodes) {
                        nodes.remove(path);
                    }
                }
            }
            node.parent = null;
        }
        if (parentName.startsWith(procPaxos)) {
            // delete the node in the trie.
            if (Quotas.limitNode.equals(childName)) {
                // we need to update the trie
                // as well
                pTrie.deletePath(parentName.substring(quotaPaxos.length()));
            }
        }

        // also check to update the quotas for this node
        String lastPrefix = pTrie.findMaxPrefix(path);
        if (!rootPaxos.equals(lastPrefix) && !("".equals(lastPrefix))) {
            // ok we have some match and need to update
            updateCount(lastPrefix, -1);
            int bytes = 0;
            synchronized (node) {
                bytes = (node.data == null ? 0 : -(node.data.length));
            }
            updateBytes(lastPrefix, bytes);
        }
        if (LOG.isTraceEnabled()) {
            PaxosTrace.logTraceMessage(LOG, PaxosTrace.EVENT_DELIVERY_TRACE_MASK,
                    "dataWatches.triggerWatch " + path);
            PaxosTrace.logTraceMessage(LOG, PaxosTrace.EVENT_DELIVERY_TRACE_MASK,
                    "childWatches.triggerWatch " + parentName);
        }
        Set<Watcher> processed = dataWatches.triggerWatch(path,
                EventType.NodeDeleted);
        childWatches.triggerWatch(path, EventType.NodeDeleted, processed);
        childWatches.triggerWatch(parentName.equals("") ? "/" : parentName,
                EventType.NodeChildrenChanged);
    }

    public Stat setData(String path, byte data[], int version, long zxid,
            long time) throws PaxosException.NoNodeException {
        Stat s = new Stat();
        DataNode n = nodes.get(path);
        if (n == null) {
            throw new PaxosException.NoNodeException();
        }
        byte lastdata[] = null;
        synchronized (n) {
            lastdata = n.data;
            n.data = data;
            n.stat.setMtime(time);
            n.stat.setMzxid(zxid);
            n.stat.setVersion(version);
            n.copyStat(s);
        }
        // now update if the path is in a quota subtree.
        String lastPrefix = pTrie.findMaxPrefix(path);
        // do nothing for the root.
        // we are not keeping a quota on the paxos
        // root node for now.
        if (!rootPaxos.equals(lastPrefix) && !("".equals(lastPrefix))) {
            this.updateBytes(lastPrefix, (data == null ? 0 : data.length)
                    - (lastdata == null ? 0 : lastdata.length));
        }
        dataWatches.triggerWatch(path, EventType.NodeDataChanged);
        return s;
    }

    public byte[] getData(String path, Stat stat, Watcher watcher)
            throws PaxosException.NoNodeException {
        DataNode n = nodes.get(path);
        if (n == null) {
            throw new PaxosException.NoNodeException();
        }
        synchronized (n) {
            n.copyStat(stat);
            if (watcher != null) {
                dataWatches.addWatch(path, watcher);
            }
            return n.data;
        }
    }

    public Stat statNode(String path, Watcher watcher)
            throws PaxosException.NoNodeException {
        Stat stat = new Stat();
        DataNode n = nodes.get(path);
        if (watcher != null) {
            dataWatches.addWatch(path, watcher);
        }
        if (n == null) {
            throw new PaxosException.NoNodeException();
        }
        synchronized (n) {
            n.copyStat(stat);
            return stat;
        }
    }

    public List<String> getChildren(String path, Stat stat, Watcher watcher)
            throws PaxosException.NoNodeException {
        DataNode n = nodes.get(path);
        if (n == null) {
            throw new PaxosException.NoNodeException();
        }
        synchronized (n) {
            if (stat != null) {
                n.copyStat(stat);
            }
            ArrayList<String> children;
            Set<String> childs = n.getChildren();
            if (childs != null) {
                children = new ArrayList<String>(childs.size());
                children.addAll(childs);
            } else {
                children = new ArrayList<String>(0);
            }

            if (watcher != null) {
                childWatches.addWatch(path, watcher);
            }
            return children;
        }
    }

    public Stat setACL(String path, List<ACL> acl, int version)
            throws PaxosException.NoNodeException {
        Stat stat = new Stat();
        DataNode n = nodes.get(path);
        if (n == null) {
            throw new PaxosException.NoNodeException();
        }
        synchronized (n) {
            n.stat.setAversion(version);
            n.acl = convertAcls(acl);
            n.copyStat(stat);
            return stat;
        }
    }

    public List<ACL> getACL(String path, Stat stat)
            throws PaxosException.NoNodeException {
        DataNode n = nodes.get(path);
        if (n == null) {
            throw new PaxosException.NoNodeException();
        }
        synchronized (n) {
            n.copyStat(stat);
            return new ArrayList<ACL>(convertLong(n.acl));
        }
    }

    static public class ProcessTxnResult {
        public long clientId;
        public int cxid;
        public long zxid;
        public int err;
        public int type;
        public String path;
        public Stat stat;

        /**
         * Equality is defined as the clientId and the cxid being the same. This
         * allows us to use hash tables to track completion of transactions.
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof ProcessTxnResult) {
                ProcessTxnResult other = (ProcessTxnResult) o;
                return other.clientId == clientId && other.cxid == cxid;
            }
            return false;
        }

        /**
         * See equals() to find the rational for how this hashcode is generated.
         *
         * @see ProcessTxnResult#equals(Object)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return (int) ((clientId ^ cxid) % Integer.MAX_VALUE);
        }
    }

    public volatile long lastProcessedZxid = 0;

    public ProcessTxnResult processTxn(TxnHeader header, Writable txn) {
        ProcessTxnResult rc = new ProcessTxnResult();

        String debug = "";
        try {
            rc.clientId = header.getClientId();
            rc.cxid = header.getCxid();
            rc.zxid = header.getZxid();
            rc.type = header.getType();
            rc.err = 0;
            if (rc.zxid > lastProcessedZxid) {
                lastProcessedZxid = rc.zxid;
            }
            switch (header.getType()) {
                case OpCode.create:
                    CreateTxn createTxn = (CreateTxn) txn;
                    debug = "Create transaction for " + createTxn.getPath();
                    createNode(
                            createTxn.getPath(),
                            createTxn.getData(),
                            createTxn.getAcl(),
                            createTxn.getEphemeral() ? header.getClientId() : 0,
                            header.getZxid(), header.getTime());
                    rc.path = createTxn.getPath();
                    break;
                case OpCode.delete:
                    DeleteTxn deleteTxn = (DeleteTxn) txn;
                    debug = "Delete transaction for " + deleteTxn.getPath();
                    deleteNode(deleteTxn.getPath(), header.getZxid());
                    break;
                case OpCode.setData:
                    SetDataTxn setDataTxn = (SetDataTxn) txn;
                    debug = "Set data for  transaction for "
                            + setDataTxn.getPath();
                    rc.stat = setData(setDataTxn.getPath(), setDataTxn
                            .getData(), setDataTxn.getVersion(), header
                            .getZxid(), header.getTime());
                    break;
                case OpCode.setACL:
                    SetACLTxn setACLTxn = (SetACLTxn) txn;
                    debug = "Set ACL for  transaction for "
                            + setACLTxn.getPath();
                    rc.stat = setACL(setACLTxn.getPath(), setACLTxn.getAcl(),
                            setACLTxn.getVersion());
                    break;
                case OpCode.closeSession:
                    killSession(header.getClientId(), header.getZxid());
                    break;
                case OpCode.error:
                    ErrorTxn errTxn = (ErrorTxn) txn;
                    rc.err = errTxn.getErr();
                    break;
            }
        } catch (PaxosException e) {
            // These are expected errors since we take a lazy snapshot
            if (initialized
                    || (e.code() != Code.NONODE && e.code() != Code.NODEEXISTS)) {
                LOG.warn("Failed:" + debug, e);
            }
        }
        return rc;
    }

    void killSession(long session, long zxid) {
        // the list is already removed from the ephemerals
        // so we do not have to worry about synchronizing on
        // the list. This is only called from FinalRequestProcessor
        // so there is no need for synchronization. The list is not
        // changed here. Only create and delete change the list which
        // are again called from FinalRequestProcessor in sequence.
        HashSet<String> list = ephemerals.remove(session);
        if (list != null) {
            for (String path : list) {
                try {
                    deleteNode(path, zxid);
                    if (LOG.isDebugEnabled()) {
                        LOG
                                .debug("Deleting ephemeral node " + path
                                        + " for session 0x"
                                        + Long.toHexString(session));
                    }
                } catch (NoNodeException e) {
                    LOG.warn("Ignoring NoNodeException for path " + path
                            + " while removing ephemeral for dead session 0x"
                            + Long.toHexString(session));
                }
            }
        }
    }

    /**
     * a encapsultaing class for return value
     */
    private static class Counts {
        long bytes;
        int count;
    }

    /**
     * this method gets the count of nodes and the bytes under a subtree
     *
     * @param path
     *            the path to be used
     * @param bytes
     *            the long bytes
     * @param count
     *            the int count
     */
    private void getCounts(String path, Counts counts) {
        DataNode node = getNode(path);
        if (node == null) {
            return;
        }
        String[] children = null;
        int len = 0;
        synchronized (node) {
            Set<String> childs = node.getChildren();
            if (childs != null) {
                children = childs.toArray(new String[childs.size()]);
            }
            len = (node.data == null ? 0 : node.data.length);
        }
        // add itself
        counts.count += 1;
        counts.bytes += len;
        if (children == null || children.length == 0) {
            return;
        }
        for (String child : children) {
            getCounts(path + "/" + child, counts);
        }
    }

    /**
     * update the quota for the given path
     *
     * @param path
     *            the path to be used
     */
    private void updateQuotaForPath(String path) {
        Counts c = new Counts();
        getCounts(path, c);
        StatsTrack strack = new StatsTrack();
        strack.setBytes(c.bytes);
        strack.setCount(c.count);
        String statPath = Quotas.quotaPaxos + path + "/" + Quotas.statNode;
        DataNode node = getNode(statPath);
        // it should exist
        if (node == null) {
            LOG.warn("Missing quota stat node " + statPath);
            return;
        }
        synchronized (node) {
            node.data = strack.toString().getBytes();
        }
    }

    /**
     * this method traverses the quota path and update the path trie and sets
     *
     * @param path
     */
    private void traverseNode(String path) {
        DataNode node = getNode(path);
        String children[] = null;
        synchronized (node) {
            Set<String> childs = node.getChildren();
            if (childs != null) {
                children = childs.toArray(new String[childs.size()]);
            }
        }
        if (children != null) {
            if (children.length == 0) {
                // this node does not have a child
                // is the leaf node
                // check if its the leaf node
                String endString = "/" + Quotas.limitNode;
                if (path.endsWith(endString)) {
                    // ok this is the limit node
                    // get the real node and update
                    // the count and the bytes
                    String realPath = path.substring(Quotas.quotaPaxos
                            .length(), path.indexOf(endString));
                    updateQuotaForPath(realPath);
                    this.pTrie.addPath(realPath);
                }
                return;
            }
            for (String child : children) {
                traverseNode(path + "/" + child);
            }
        }
    }

    /**
     * this method sets up the path trie and sets up stats for quota nodes
     */
    private void setupQuota() {
        String quotaPath = Quotas.quotaPaxos;
        DataNode node = getNode(quotaPath);
        if (node == null) {
            return;
        }
        traverseNode(quotaPath);
    }

    /**
     * this method uses a stringbuilder to create a new path for children. This
     * is faster than string appends ( str1 + str2).
     *
     * @param out
     *            OutputArchive to write to.
     * @param path
     *            a string builder.
     * @throws IOException
     * @throws InterruptedException
     */
    void serializeNode(DataOutput out, StringBuilder path) throws IOException {
        String pathString = path.toString();
        DataNode node = getNode(pathString);
        if (node == null) {
            return;
        }
        String children[] = null;
        synchronized (node) {
            scount++;
            //oa.writeString(pathString, "path");
            //oa.writeRecord(node, "node");
            UTF8.writeString(out, pathString); 
            node.write(out); 
            Set<String> childs = node.getChildren();
            if (childs != null) {
                children = childs.toArray(new String[childs.size()]);
            }
        }
        path.append('/');
        int off = path.length();
        if (children != null) {
            for (String child : children) {
                // since this is single buffer being resused
                // we need
                // to truncate the previous bytes of string.
                path.delete(off, Integer.MAX_VALUE);
                path.append(child);
                serializeNode(out, path);
            }
        }
    }

    int scount;
    public boolean initialized = false;

    private void deserializeList(Map<Long, List<ACL>> longKeyMap, DataInput in) throws IOException {
        //int i = ia.readInt("map");
        int i = in.readInt();
        while (i > 0) {
            //Long val = ia.readLong("long");
            Long val = new Long(in.readLong()); 
            if (aclIndex < val) {
                aclIndex = val;
            }
            List<ACL> aclList = new ArrayList<ACL>();
            //Index j = ia.startVector("acls");
            int size = in.readInt(); 
            //while (!j.done()) {
            for (int j=0; j < size; j++) {
                //ACL acl = new ACL();
                ACL acl = ACL.read(in); 
                //acl.deserialize(ia, "acl");
                aclList.add(acl);
                //j.incr();
            }
            longKeyMap.put(val, aclList);
            aclKeyMap.put(aclList, val);
            i--;
        }
    }

    private synchronized void serializeList(Map<Long, List<ACL>> longKeyMap, 
    		DataOutput out) throws IOException {
        //oa.writeInt(longKeyMap.size(), "map");
        out.writeInt(longKeyMap.size()); 
        Set<Map.Entry<Long, List<ACL>>> set = longKeyMap.entrySet();
        for (Map.Entry<Long, List<ACL>> val : set) {
            //oa.writeLong(val.getKey(), "long");
            out.writeLong(val.getKey().longValue()); 
            List<ACL> aclList = val.getValue();
            //oa.startVector(aclList, "acls");
            out.writeInt(aclList.size()); 
            for (ACL acl : aclList) {
                //acl.serialize(oa, "acl");
                acl.write(out); 
            }
            //oa.endVector(aclList, "acls");
        }
    }

    public void serialize(DataOutput out) throws IOException {
        scount = 0;
        serializeList(longKeyMap, out);
        serializeNode(out, new StringBuilder(""));
        // / marks end of stream
        // we need to check if clear had been called in between the snapshot.
        if (root != null) {
            //oa.writeString("/", "path");
            UTF8.writeString(out, "/"); 
        }
    }

    public void deserialize(DataInput in) throws IOException {
        deserializeList(longKeyMap, in);
        nodes.clear();
        //String path = ia.readString("path");
        String path = UTF8.readString(in);
        while (!path.equals("/")) {
            //DataNode node = new DataNode();
            //ia.readRecord(node, "node");
            DataNode node = DataNode.read(in); 
            nodes.put(path, node);
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash == -1) {
                root = node;
            } else {
                String parentPath = path.substring(0, lastSlash);
                node.parent = nodes.get(parentPath);
                if (node.parent == null) {
                    throw new IOException("Invalid Datatree, unable to find " +
                            "parent " + parentPath + " of path " + path);
                }
                node.parent.addChild(path.substring(lastSlash + 1));
                long eowner = node.stat.getEphemeralOwner();
                if (eowner != 0) {
                    HashSet<String> list = ephemerals.get(eowner);
                    if (list == null) {
                        list = new HashSet<String>();
                        ephemerals.put(eowner, list);
                    }
                    list.add(path);
                }
            }
            //path = ia.readString("path");
            path = UTF8.readString(in); 
        }
        nodes.put("/", root);
        // we are done with deserializing the
        // the datatree
        // update the quotas - create path trie
        // and also update the stat nodes
        setupQuota();
    }

    /**
     * Summary of the watches on the datatree.
     * @param pwriter the output to write to
     */
    public synchronized void dumpWatchesSummary(PrintWriter pwriter) {
        pwriter.print(dataWatches.toString());
    }

    /**
     * Write a text dump of all the watches on the datatree.
     * Warning, this is expensive, use sparingly!
     * @param pwriter the output to write to
     */
    public synchronized void dumpWatches(PrintWriter pwriter, boolean byPath) {
        dataWatches.dumpWatches(pwriter, byPath);
    }

    /**
     * Write a text dump of all the ephemerals in the datatree.
     * @param pwriter the output to write to
     */
    public void dumpEphemerals(PrintWriter pwriter) {
        Set<Long> keys = ephemerals.keySet();
        pwriter.println("Sessions with Ephemerals ("
                + keys.size() + "):");
        for (long k : keys) {
            pwriter.print("0x" + Long.toHexString(k));
            pwriter.println(":");
            HashSet<String> tmp = ephemerals.get(k);
            synchronized (tmp) {
                for (String path : tmp) {
                    pwriter.println("\t" + path);
                }
            }
        }
    }

    public void removeCnxn(Watcher watcher) {
        dataWatches.removeWatcher(watcher);
        childWatches.removeWatcher(watcher);
    }

    public void clear() {
        root = null;
        nodes.clear();
        ephemerals.clear();
        // dataWatches = null;
        // childWatches = null;
    }

    public void setWatches(long relativeZxid, List<String> dataWatches,
            List<String> existWatches, List<String> childWatches,
            Watcher watcher) {
        for (String path : dataWatches) {
            DataNode node = getNode(path);
            WatchedEvent e = null;
            if (node == null) {
                e = new WatchedEvent(EventType.NodeDeleted,
                        PaxosState.SyncConnected, path);
            } else if (node.stat.getCzxid() > relativeZxid) {
                e = new WatchedEvent(EventType.NodeCreated,
                        PaxosState.SyncConnected, path);
            } else if (node.stat.getMzxid() > relativeZxid) {
                e = new WatchedEvent(EventType.NodeDataChanged,
                        PaxosState.SyncConnected, path);
            }
            if (e != null) {
                watcher.process(e);
            } else {
                this.dataWatches.addWatch(path, watcher);
            }
        }
        for (String path : existWatches) {
            DataNode node = getNode(path);
            WatchedEvent e = null;
            if (node == null) {
                // This is the case when the watch was registered
            } else if (node.stat.getMzxid() > relativeZxid) {
                e = new WatchedEvent(EventType.NodeDataChanged,
                        PaxosState.SyncConnected, path);
            } else {
                e = new WatchedEvent(EventType.NodeCreated,
                        PaxosState.SyncConnected, path);
            }
            if (e != null) {
                watcher.process(e);
            } else {
                this.dataWatches.addWatch(path, watcher);
            }
        }
        for (String path : childWatches) {
            DataNode node = getNode(path);
            WatchedEvent e = null;
            if (node == null) {
                e = new WatchedEvent(EventType.NodeDeleted,
                        PaxosState.SyncConnected, path);
            } else if (node.stat.getPzxid() > relativeZxid) {
                e = new WatchedEvent(EventType.NodeChildrenChanged,
                        PaxosState.SyncConnected, path);
            }
            if (e != null) {
                watcher.process(e);
            } else {
                this.childWatches.addWatch(path, watcher);
            }
        }
    }
}

