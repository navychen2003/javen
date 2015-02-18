package org.javenstudio.raptor.dfs.protocol;

public final class NSQuotaExceededException extends QuotaExceededException {
  protected static final long serialVersionUID = 1L;
  
  public NSQuotaExceededException(String msg) {
    super(msg);
  }
  
  public NSQuotaExceededException(long quota, long count) {
    super(quota, count);
  }

  public String getMessage() {
    String msg = super.getMessage();
    if (msg == null) {
      return "The NameSpace quota (directories and files)" + 
      (pathName==null?"":(" of directory " + pathName)) + 
          " is exceeded: quota=" + quota + " file count=" + count; 
    } else {
      return msg;
    }
  }
}

