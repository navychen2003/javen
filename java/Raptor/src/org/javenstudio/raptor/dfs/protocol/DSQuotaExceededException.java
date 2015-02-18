package org.javenstudio.raptor.dfs.protocol;

import org.javenstudio.raptor.util.StringUtils;

public class DSQuotaExceededException extends QuotaExceededException {
  protected static final long serialVersionUID = 1L;

  public DSQuotaExceededException(String msg) {
    super(msg);
  }

  public DSQuotaExceededException(long quota, long count) {
    super(quota, count);
  }

  public String getMessage() {
    String msg = super.getMessage();
    if (msg == null) {
      return "The DiskSpace quota" + (pathName==null?"":(" of " + pathName)) + 
          " is exceeded: quota=" + quota + " diskspace consumed=" + StringUtils.humanReadableInt(count);
    } else {
      return msg;
    }
  }
}

