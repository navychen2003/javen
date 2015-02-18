package org.javenstudio.raptor.paxos.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.data.Stat;
import org.javenstudio.raptor.paxos.data.StatPersisted;

/**
 * This class contains the data for a node in the data tree.
 * <p>
 * A data node contains a reference to its parent, a byte array as its data, an
 * array of ACLs, a stat object, and a set of its children's paths.
 * 
 */
public class DataNode implements Writable {
    /** the parent of this datanode */
    DataNode parent;

    /** the data for this datanode */
    byte data[];

    /**
     * the acl map long for this datanode. the datatree has the map
     */
    Long acl;

    /**
     * the stat for this node that is persisted to disk.
     */
    public StatPersisted stat;

    /**
     * the list of children for this node. note that the list of children string
     * does not contain the parent path -- just the last part of the path. This
     * should be synchronized on except deserializing (for speed up issues).
     */
    private Set<String> children = null;

    /**
     * default constructor for the datanode
     */
    DataNode() {
        // default constructor
    }

    /**
     * create a DataNode with parent, data, acls and stat
     * 
     * @param parent
     *            the parent of this DataNode
     * @param data
     *            the data to be set
     * @param acl
     *            the acls for this node
     * @param stat
     *            the stat for this node.
     */
    public DataNode(DataNode parent, byte data[], Long acl, StatPersisted stat) {
        this.parent = parent;
        this.data = data;
        this.acl = acl;
        this.stat = stat;
    }

    /**
     * Method that inserts a child into the children set
     * 
     * @param child
     *            to be inserted
     * @return true if this set did not already contain the specified element
     */
    public synchronized boolean addChild(String child) {
        if (children == null) {
            // let's be conservative on the typical number of children
            children = new HashSet<String>(8);
        }
        return children.add(child);
    }

    /**
     * Method that removes a child from the children set
     * 
     * @param child
     * @return true if this set contained the specified element
     */
    public synchronized boolean removeChild(String child) {
        if (children == null) {
            return false;
        }
        return children.remove(child);
    }

    /**
     * convenience method for setting the children for this datanode
     * 
     * @param children
     */
    public synchronized void setChildren(HashSet<String> children) {
        this.children = children;
    }

    /**
     * convenience methods to get the children
     * 
     * @return the children of this datanode
     */
    public synchronized Set<String> getChildren() {
        return children;
    }

    synchronized public void copyStat(Stat to) {
        to.setAversion(stat.getAversion());
        to.setCtime(stat.getCtime());
        to.setCversion(stat.getCversion());
        to.setCzxid(stat.getCzxid());
        to.setMtime(stat.getMtime());
        to.setMzxid(stat.getMzxid());
        to.setPzxid(stat.getPzxid());
        to.setVersion(stat.getVersion());
        to.setEphemeralOwner(stat.getEphemeralOwner());
        to.setDataLength(data == null ? 0 : data.length);
        if (this.children == null) {
            to.setNumChildren(0);
        } else {
            to.setNumChildren(children.size());
        }
    }

    /////////////////////////////////////////////////
    // Writable
    /////////////////////////////////////////////////
    /** {@inheritDoc} */
    public synchronized void write(DataOutput out) throws IOException {
        out.writeInt(data != null ? data.length : 0);
        if (data != null) out.write(data);
        out.writeLong(acl != null ? acl.longValue() : 0); 
        out.writeBoolean(stat != null); 
        if (stat != null) stat.write(out); 
    }

    /** {@inheritDoc} */
    public synchronized void readFields(DataInput in) throws IOException {
        int size = in.readInt();
        if (size > 0) {
            data = new byte[size];
            in.readFully(data, 0, size);
        } else
            data = null;
        acl = new Long(in.readLong()); 
        if (in.readBoolean()) {
            stat = StatPersisted.read(in); 
        } else
            stat = null; 
    }

    public static DataNode read(DataInput in) throws IOException {
        DataNode result = new DataNode();
        result.readFields(in);
        return result;
    }

}
