package org.javenstudio.raptor.dfs.protocol;

import java.io.IOException;

/** 
 * This exception is thrown when modification to HDFS results in violation
 * of a directory quota. A directory quota might be namespace quota (limit 
 * on number of files and directories) or a diskspace quota (limit on space 
 * taken by all the file under the directory tree). <br> <br>
 * 
 * The message for the exception specifies the directory where the quota
 * was violated and actual quotas. Specific message is generated in the 
 * corresponding Exception class: 
 *  DSQuotaExceededException or
 *  NSQuotaExceededException
 */
public class QuotaExceededException extends IOException {
  protected static final long serialVersionUID = 1L;
  protected String pathName=null;
  protected long quota; // quota
  protected long count; // actual value
  
  protected QuotaExceededException(String msg) {
    super(msg);
  }
  
  protected QuotaExceededException(long quota, long count) {
    this.quota = quota;
    this.count = count;
  }
  
  public void setPathName(String path) {
    this.pathName = path;
  }
  
  public String getMessage() {
    return super.getMessage();
  }
}

