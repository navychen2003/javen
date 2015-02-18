package org.javenstudio.raptor.fs;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.util.Shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/** Filesystem disk space usage statistics.  Uses the unix 'du' program*/
public class DU extends Shell {
  private String  dirPath;

  private AtomicLong used = new AtomicLong();
  private volatile boolean shouldRun = true;
  private Thread refreshUsed;
  private IOException duException = null;
  private long refreshInterval;
  
  /**
   * Keeps track of disk usage.
   * @param path the path to check disk usage in
   * @param interval refresh the disk usage at this interval
   * @throws IOException if we fail to refresh the disk usage
   */
  public DU(File path, long interval) throws IOException {
    super(0);
    
    //we set the Shell interval to 0 so it will always run our command
    //and use this one to set the thread sleep interval
    this.refreshInterval = interval;
    this.dirPath = path.getCanonicalPath();
    
    //populate the used variable
    run();
  }
  
  /**
   * Keeps track of disk usage.
   * @param path the path to check disk usage in
   * @param conf configuration object
   * @throws IOException if we fail to refresh the disk usage
   */
  public DU(File path, Configuration conf) throws IOException {
    this(path, 600000L);
    //10 minutes default refresh interval
  }

  /**
   * This thread refreshes the "used" variable.
   * 
   * Future improvements could be to not permanently
   * run this thread, instead run when getUsed is called.
   **/
  class DURefreshThread implements Runnable {
    @Override
    public void run() {
      while(shouldRun) {
        try {
          Thread.sleep(refreshInterval);
          
          try {
            //update the used variable
            DU.this.run();
          } catch (IOException e) {
            synchronized (DU.this) {
              //save the latest exception so we can return it in getUsed()
              duException = e;
            }
            
            LOG.warn("Could not get disk usage information", e);
          }
        } catch (InterruptedException e) {
        }
      }
    }
  }
  
  /**
   * Decrease how much disk space we use.
   * @param value decrease by this value
   */
  public void decDfsUsed(long value) {
    used.addAndGet(-value);
  }

  /**
   * Increase how much disk space we use.
   * @param value increase by this value
   */
  public void incDfsUsed(long value) {
    used.addAndGet(value);
  }
  
  /**
   * @return disk space used 
   * @throws IOException if the shell command fails
   */
  public long getUsed() throws IOException {
    //if the updating thread isn't started, update on demand
    if (refreshUsed == null) {
      run();
    } else {
      synchronized (DU.this) {
        //if an exception was thrown in the last run, rethrow
        if(duException != null) {
          IOException tmp = duException;
          duException = null;
          throw tmp;
        }
      }
    }
    
    return used.longValue();
  }

  /**
   * @return the path of which we're keeping track of disk usage
   */
  public String getDirPath() {
    return dirPath;
  }
  
  /**
   * Start the disk usage checking thread.
   */
  public void start() {
    //only start the thread if the interval is sane
    if(refreshInterval > 0) {
      refreshUsed = new Thread(new DURefreshThread(), 
          "refreshUsed-"+dirPath);
      refreshUsed.setDaemon(true);
      refreshUsed.start();
    }
  }
  
  /**
   * Shut down the refreshing thread.
   */
  public void shutdown() {
    this.shouldRun = false;
    
    if(this.refreshUsed != null) {
      this.refreshUsed.interrupt();
    }
  }
  
  @Override
  public String toString() {
    return
      "du -sk " + dirPath +"\n" +
      used + "\t" + dirPath;
  }

  @Override
  protected String[] getExecString() {
	if (Shell.WINDOWS) return null;
    return new String[] {"du", "-sk", dirPath};
  }
  
  @Override
  protected void runNoCommand() {
	File dir = new File(dirPath);
	this.used.set(countSize(dir, 0));
  }
  
  private long countSize(File dir, long size) {
	if (dir == null || !dir.isDirectory())
	  return size;
	
	File[] files = dir.listFiles();
	if (files != null) { 
	  for (File file : files) { 
		if (file.isFile())
		  size += file.length();
		else
		  size = countSize(file, size);
	  }
	}
	
	return size;
  }
  
  @Override
  protected void parseExecResult(BufferedReader lines) throws IOException {
    String line = lines.readLine();
    if (line == null) {
      throw new IOException("Expecting a line not the end of stream");
    }
    String[] tokens = line.split("\t");
    if(tokens.length == 0) {
      throw new IOException("Illegal du output");
    }
    this.used.set(Long.parseLong(tokens[0])*1024);
  }

  public static void main(String[] args) throws Exception {
    String path = ".";
    if (args.length > 0) {
      path = args[0];
    }

    System.out.println(new DU(new File(path), ConfigurationFactory.create(true)).toString());
  }
}

