package org.javenstudio.raptor.dfs.server.common;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.javenstudio.raptor.dfs.protocol.FSConstants;
import org.javenstudio.raptor.util.StringUtils;

/**
 * Collection of upgrade objects.
 *
 * Upgrade objects should be registered here before they can be used. 
 */
public class UpgradeObjectCollection {
  static {
    initialize();
    // Registered distributed upgrade objects here
    // registerUpgrade(new UpgradeObject());
  }

  static class UOSignature implements Comparable<UOSignature> {
    int version;
    DfsConstants.NodeType type;
    String className;

    UOSignature(Upgradeable uo) {
      this.version = uo.getVersion();
      this.type = uo.getType();
      this.className = uo.getClass().getCanonicalName();
    }

    int getVersion() {
      return version;
    }

    DfsConstants.NodeType getType() {
      return type;
    }

    String getClassName() {
      return className;
    }

    Upgradeable instantiate() throws IOException {
      try {
        return (Upgradeable)Class.forName(getClassName()).newInstance();
      } catch(ClassNotFoundException e) {
        throw new IOException(StringUtils.stringifyException(e));
      } catch(InstantiationException e) {
        throw new IOException(StringUtils.stringifyException(e));
      } catch(IllegalAccessException e) {
        throw new IOException(StringUtils.stringifyException(e));
      }
    }

    public int compareTo(UOSignature o) {
      if(this.version != o.version)
        return (version < o.version ? -1 : 1);
      int res = this.getType().toString().compareTo(o.getType().toString());
      if(res != 0)
        return res;
      return className.compareTo(o.className);
    }

    public boolean equals(Object o) {
        if (!(o instanceof UOSignature)) {
          return false;
        }
        return this.compareTo((UOSignature)o) == 0;
      }

      public int hashCode() {
        return version ^ ((type==null)?0:type.hashCode()) 
                       ^ ((className==null)?0:className.hashCode());
      }
  }

  /**
   * Static collection of upgrade objects sorted by version.
   * Layout versions are negative therefore newer versions will go first.
   */
  static SortedSet<UOSignature> upgradeTable;

  static final void initialize() {
    upgradeTable = new TreeSet<UOSignature>();
  }

  static void registerUpgrade(Upgradeable uo) {
    // Registered distributed upgrade objects here
    upgradeTable.add(new UOSignature(uo));
  }

  public static SortedSet<Upgradeable> getDistributedUpgrades(int versionFrom, 
                                                       DfsConstants.NodeType type
                                                       ) throws IOException {
    assert FSConstants.LAYOUT_VERSION <= versionFrom : "Incorrect version " 
      + versionFrom + ". Expected to be <= " + FSConstants.LAYOUT_VERSION;
    SortedSet<Upgradeable> upgradeObjects = new TreeSet<Upgradeable>();
    for(UOSignature sig : upgradeTable) {
      if(sig.getVersion() < FSConstants.LAYOUT_VERSION)
        continue;
      if(sig.getVersion() > versionFrom)
        break;
      if(sig.getType() != type )
        continue;
      upgradeObjects.add(sig.instantiate());
    }
    if(upgradeObjects.size() == 0)
      return null;
    return upgradeObjects;
  }
}

