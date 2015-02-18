package org.javenstudio.raptor.bigdb;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.javenstudio.raptor.io.VersionedWritable;

/**
 * Status information on the bigdb cluster.
 * <p>
 * <tt>ClusterStatus</tt> provides clients with information such as:
 * <ul>
 * <li>The count and names of region servers in the cluster.</li>
 * <li>The count and names of dead region servers in the cluster.</li>
 * <li>The average cluster load.</li>
 * <li>The number of regions deployed on the cluster.</li>
 * <li>The number of requests since last report.</li>
 * <li>Detailed region server loading and resource usage information,
 *  per server and per region.</li>
 *  <li>Regions in transition at master</li>
 * </ul>
 */
public class ClusterStatus extends VersionedWritable {
  private static final byte VERSION = 0;

  private String bigdbVersion;
  private Collection<DBServerInfo> liveServerInfo;
  private Collection<String> deadServers;
  private NavigableMap<String, String> intransition;

  /**
   * Constructor, for Writable
   */
  public ClusterStatus() {
    super();
  }

  /**
   * @return the names of region servers on the dead list
   */
  public Collection<String> getDeadServerNames() {
    return Collections.unmodifiableCollection(deadServers);
  }

  /**
   * @return the number of region servers in the cluster
   */
  public int getLiveServerCount() {
    return liveServerInfo.size();
  }

  /**
   * @return the number of dead region servers in the cluster
   */
  public int getDeadServerCount() {
    return deadServers.size();
  }

  /**
   * @return the average cluster load
   */
  public double getAverageLoad() {
    int load = 0;
    for (DBServerInfo server: liveServerInfo) {
      load += server.getLoad().getLoad();
    }
    return (double)load / (double)liveServerInfo.size();
  }

  /**
   * @return the number of regions deployed on the cluster
   */
  public int getRegionsCount() {
    int count = 0;
    for (DBServerInfo server: liveServerInfo) {
      count += server.getLoad().getNumberOfRegions();
    }
    return count;
  }

  /**
   * @return the number of requests since last report
   */
  public int getRequestsCount() {
    int count = 0;
    for (DBServerInfo server: liveServerInfo) {
      count += server.getLoad().getNumberOfRequests();
    }
    return count;
  }

  /**
   * @return the bigdb version string as reported by the HMaster
   */
  public String getDBVersion() {
    return bigdbVersion;
  }

  /**
   * @param version the bigdb version string
   */
  public void setDBVersion(String version) {
    bigdbVersion = version;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClusterStatus)) {
      return false;
    }
    return (getVersion() == ((ClusterStatus)o).getVersion()) &&
      getDBVersion().equals(((ClusterStatus)o).getDBVersion()) &&
      liveServerInfo.equals(((ClusterStatus)o).liveServerInfo) &&
      deadServers.equals(((ClusterStatus)o).deadServers);
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return VERSION + bigdbVersion.hashCode() + liveServerInfo.hashCode() +
      deadServers.hashCode();
  }

  /** @return the object version number */
  public byte getVersion() {
    return VERSION;
  }

  //
  // Getters
  //

  /**
   * Returns detailed region server information: A list of
   * {@link DBServerInfo}, containing server load and resource usage
   * statistics as {@link HServerLoad}, containing per-region
   * statistics as {@link HServerLoad.RegionLoad}.
   * @return region server information
   */
  public Collection<DBServerInfo> getServerInfo() {
    return Collections.unmodifiableCollection(liveServerInfo);
  }

  //
  // Setters
  //

  public void setServerInfo(Collection<DBServerInfo> serverInfo) {
    this.liveServerInfo = serverInfo;
  }

  public void setDeadServers(Collection<String> deadServers) {
    this.deadServers = deadServers;
  }

  public Map<String, String> getRegionsInTransition() {
    return this.intransition;
  }

  public void setRegionsInTransition(final NavigableMap<String, String> m) {
    this.intransition = m;
  }

  //
  // Writable
  //

  public void write(DataOutput out) throws IOException {
    super.write(out);
    out.writeUTF(bigdbVersion);
    out.writeInt(liveServerInfo.size());
    for (DBServerInfo server: liveServerInfo) {
      server.write(out);
    }
    out.writeInt(deadServers.size());
    for (String server: deadServers) {
      out.writeUTF(server);
    }
    out.writeInt(this.intransition.size());
    for (Map.Entry<String, String> e: this.intransition.entrySet()) {
      out.writeUTF(e.getKey());
      out.writeUTF(e.getValue());
    }
  }

  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    bigdbVersion = in.readUTF();
    int count = in.readInt();
    liveServerInfo = new ArrayList<DBServerInfo>(count);
    for (int i = 0; i < count; i++) {
      DBServerInfo info = new DBServerInfo();
      info.readFields(in);
      liveServerInfo.add(info);
    }
    count = in.readInt();
    deadServers = new ArrayList<String>(count);
    for (int i = 0; i < count; i++) {
      deadServers.add(in.readUTF());
    }
    count = in.readInt();
    this.intransition = new TreeMap<String, String>();
    for (int i = 0; i < count; i++) {
      String key = in.readUTF();
      String value = in.readUTF();
      this.intransition.put(key, value);
    }
  }
}

