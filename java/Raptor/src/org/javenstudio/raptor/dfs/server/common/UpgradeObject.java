package org.javenstudio.raptor.dfs.server.common;

import java.io.IOException;

import org.javenstudio.raptor.dfs.server.common.UpgradeObjectCollection.UOSignature;

/**
 * Abstract upgrade object.
 * 
 * Contains default implementation of common methods of {@link Upgradeable}
 * interface.
 */
public abstract class UpgradeObject implements Upgradeable {
  protected short status;
  
  public short getUpgradeStatus() {
    return status;
  }

  public String getDescription() {
    return "Upgrade object for " + getType() + " layout version " + getVersion();
  }

  public UpgradeStatusReport getUpgradeStatusReport(boolean details) 
                                                    throws IOException {
    return new UpgradeStatusReport(getVersion(), getUpgradeStatus(), false);
  }

  public int compareTo(Upgradeable o) {
    if(this.getVersion() != o.getVersion())
      return (getVersion() > o.getVersion() ? -1 : 1);
    int res = this.getType().toString().compareTo(o.getType().toString());
    if(res != 0)
      return res;
    return getClass().getCanonicalName().compareTo(
                    o.getClass().getCanonicalName());
  }

  public boolean equals(Object o) {
    if (!(o instanceof UpgradeObject)) {
      return false;
    }
    return this.compareTo((UpgradeObject)o) == 0;
  }

  public int hashCode() {
    return new UOSignature(this).hashCode(); 
  }
}

