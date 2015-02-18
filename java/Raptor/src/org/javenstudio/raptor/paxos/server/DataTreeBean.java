package org.javenstudio.raptor.paxos.server;

import java.util.HashSet;
import java.util.Map;

import org.javenstudio.raptor.paxos.jmx.PaxosMBeanInfo;
import org.javenstudio.raptor.paxos.server.DataTree;

/**
 * This class implements the data tree MBean.
 */
public class DataTreeBean implements DataTreeMXBean, PaxosMBeanInfo {
    DataTree dataTree;
    
    public DataTreeBean(DataTree dataTree){
        this.dataTree = dataTree;
    }
    
    public int getNodeCount() {
        return dataTree.getNodeCount();
    }

    public long approximateDataSize() {
        return dataTree.approximateDataSize();
    }

    public int countEphemerals() {
        Map<Long, HashSet<String>> map = dataTree.getEphemeralsMap();
        int result = 0;
        for (HashSet<String> set : map.values()) {
            result += set.size();
        }
        return result;
    }

    public int getWatchCount() {
        return dataTree.getWatchCount();
    }

    public String getName() {
        return "InMemoryDataTree";
    }

    public boolean isHidden() {
        return false;
    }

    public String getLastZxid() {
        return "0x" + Long.toHexString(dataTree.lastProcessedZxid);
    }

}

