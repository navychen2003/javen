package org.javenstudio.raptor.util;

import java.io.Writer; 
import java.io.InputStream; 
import java.io.OutputStreamWriter; 
import java.io.FileWriter; 
import java.io.File; 
import java.io.IOException; 
import java.io.Closeable; 
import java.util.Map; 
import java.util.Iterator; 
import java.util.List; 
import java.util.ArrayList; 
import java.util.Vector; 
import java.util.Date; 
import java.text.SimpleDateFormat; 

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration; 
import org.javenstudio.raptor.conf.ConfigurationFactory; 


public class ProcessRunner implements Closeable {
  public static final Logger LOG = Logger.getLogger(ProcessRunner.class);

  private Configuration conf = null; 
  private Process process = null; 
  private boolean killed = false; 
  private String logfile = null; 

  private Object lockStdErr = new Object(); 
  private File logStdErrWriterFile = null; 
  private Writer logStdErrWriter = null; 
  private Writer stderrWriter = null; 

  private Object lockStdOut = new Object(); 
  private File logStdOutWriterFile = null; 
  private Writer logStdOutWriter = null; 
  private Writer stdoutWriter = null; 


  public ProcessRunner(Configuration conf) {
    this(conf, null); 
  }

  public ProcessRunner(Configuration conf, String logfile) {
    setConf(conf); 
    this.process = null; 
    this.killed = false; 
    this.logfile = logfile; 
    this.logStdErrWriter = null; 
    this.logStdOutWriter = null; 
  }

  /* ----------------------------- *
   * <implementation:Configurable> *
   * ----------------------------- */

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  /* ------------------------------ *
   * </implementation:Configurable> *
   * ------------------------------ */

  public void setStdoutWriter(Writer writer) { 
    synchronized (lockStdOut) { 
      if (stdoutWriter == null) 
        stdoutWriter = writer; 
    }
  }

  public void setStderrWriter(Writer writer) { 
    synchronized (lockStdErr) { 
      if (stderrWriter == null) 
        stderrWriter = writer; 
    }
  }

  public void close() throws IOException {
    synchronized (lockStdErr) {
      if (logStdErrWriter != null) {
        logStdErrWriter.flush(); 
        logStdErrWriter.close(); 
        logStdErrWriter = null; 
      }
    }
    synchronized (lockStdOut) {
      if (logStdOutWriter != null) {
        logStdOutWriter.flush(); 
        logStdOutWriter.close(); 
        logStdOutWriter = null; 
      }
    }
  }

  @SuppressWarnings("resource")
  public static void main(String[] args) throws Exception {
    if (args == null || args.length == 0) {
      System.out.println("Usage: ProcessRunner <commands...>"); 
      return; 
    }
    ProcessRunner pt = new ProcessRunner(ConfigurationFactory.create()); 
    pt.run(args); 
  }

  public void run(String cmd) throws IOException, InterruptedException {
    run(getJvmArgs(conf, new String[]{cmd})); 
  }

  public void run(String[] args) throws IOException, InterruptedException {
    run(args, getCurrentEnvp(conf), null); 
  }

  public void run(String[] args, File dir) throws IOException, InterruptedException {
    run(args, getCurrentEnvp(conf), dir); 
  }

  public void run(String[] args, String[] envp) throws IOException, InterruptedException {
    run(args, envp, null); 
  }

  public void run(String[] args, String[] envp, File dir) throws IOException, InterruptedException {
    runChild(getJvmArgs(conf, args), envp, dir); 
  }

  public static String[] getJvmArgs(Configuration conf, String[] args) {
    //  Build exec child jmv args.
    Vector<String> vargs = new Vector<String>(8);

/*
    File jvm =                                  // use same jvm as parent
      new File(new File(System.getProperty("java.home"), "bin"), "java");

    vargs.add(jvm.toString());

    Properties p = System.getProperties();
    for (Iterator it = p.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      String val = (String) p.getProperty(key);
      if (key != null && val != null)
        vargs.add("-D"+key+"="+val);
    }
*/

    if (args != null) {
      for (int i=0; i < args.length; i++) {
        String[] subarr = null; //ToolBase.buildArgs(args[i]); 
        for (int j=0; subarr != null && j<subarr.length; j++) 
          vargs.add(subarr[j]); 
      }
    }

    return vargs.toArray(new String[vargs.size()]);
  }

  private File getStdErrWriterFile() throws IOException {
    synchronized (lockStdErr) {
      if (logStdErrWriterFile == null) {
        if (logfile != null) 
          logStdErrWriterFile = new File(logfile+"-stderr.log"); 
        else 
          logStdErrWriterFile = null; 
      }
    }
    return logStdErrWriterFile; 
  }

  @SuppressWarnings("unused")
  private Writer getStdErrWriter() throws IOException {
    synchronized (lockStdErr) {
      if (logStdErrWriter == null) {
        File logfile = getStdErrWriterFile(); 
        if (logfile != null) {
          if (LOG.isDebugEnabled()) 
            LOG.debug("stderr logwriter: "+logfile.getAbsolutePath()); 
          logStdErrWriter = new FileWriter(logfile, true); 
        } else {
          if (logfile != null) 
            LOG.warn("cannot open stderr to logfile: "+logfile); 
          logStdErrWriter = new OutputStreamWriter(System.err); 
        }
      }
    }
    return logStdErrWriter; 
  }

  private File getStdOutWriterFile() throws IOException {
    synchronized (lockStdOut) {
      if (logStdOutWriterFile == null) {
        if (logfile != null) 
          logStdOutWriterFile = new File(logfile+"-stdout.log"); 
        else 
          logStdOutWriterFile = null; 
      }
    }
    return logStdOutWriterFile; 
  }

  @SuppressWarnings("unused")
  private Writer getStdOutWriter() throws IOException {
    synchronized (lockStdOut) {
      if (logStdOutWriter == null) {
        File logfile = getStdOutWriterFile(); 
        if (logfile != null) {
          if (LOG.isDebugEnabled()) 
            LOG.debug("stdout logwriter: "+logfile.getAbsolutePath()); 
          logStdOutWriter = new FileWriter(logfile, true); 
        } else {
          if (logfile != null) 
            LOG.warn("cannot open stdout to logfile: "+logfile); 
          logStdOutWriter = new OutputStreamWriter(System.out); 
        }
      }
    }
    return logStdOutWriter; 
  }

  private static final int STDOUT = 0; 
  private static final int STDERR = 1; 

  private Writer getUserWriter(int i) throws IOException {
    if (i == 0) 
      return stdoutWriter; 
    else 
      return stderrWriter; 
  }

  private Writer getWriter(int i) throws IOException {
    if (i == 0) 
      return getStdOutWriter(); 
    else 
      return getStdErrWriter(); 
  }

  private File getWriterFile(int i) throws IOException {
    if (i == 0) 
      return getStdOutWriterFile(); 
    else 
      return getStdErrWriterFile(); 
  }

  private synchronized void resetWriterFile(int i) throws IOException {
    if (i == 0) {
      synchronized (lockStdOut) {
        logStdOutWriterFile = null; 
        logStdOutWriter = null; 
      }
    }
    else {
      synchronized (lockStdErr) {
        logStdErrWriterFile = null; 
        logStdErrWriter = null; 
      }
    }
  }

  private boolean rollingLogFile(int i, long writedsize) throws IOException {
    if (writedsize < 1024 * 1024 * 100) return false; 
    File f = getWriterFile(i); 
    if (f == null) return false; 
    String filename = f.getAbsolutePath() + "." + getDateString(); 
    File newFile = new File(filename); 
    f.renameTo(newFile); 
    resetWriterFile(i); 
    LOG.info("rolling log file: "+filename); 
    return true;  
  }

  public static String getDateString() {
    return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()));
  }


  private void runChild(String[] args, String[] envp, File dir) throws IOException, InterruptedException {
    if (this.process != null)  
      throw new IOException("Process is already running."); 
    if (args == null || args.length == 0) 
      throw new IOException("Process command array has not input."); 
    envp = getCurrentEnvp(getConf(), envp); 

    if (dir != null) 
      this.process = Runtime.getRuntime().exec(args, envp, dir);
    else 
      this.process = Runtime.getRuntime().exec(args, envp);

    Thread logStdErrThread = null;
    Thread logStdOutThread = null;
    try {
      // Copy stderr of the child-process via a thread
      logStdErrThread = logStream("process-stderr",
                                  process.getErrorStream(), 
                                  STDERR); 

      // Copy stdout of the child-process via a thread
      logStdOutThread = logStream("process-stdout",
                                  process.getInputStream(), 
                                  STDOUT); 

      int exit_code = process.waitFor();

      if (!killed && exit_code != 0) {
        StringBuffer sbuf = new StringBuffer(); 
        for (int i=0; i<args.length; i++) {
          sbuf.append(args[i]+" "); 
        }
        throw new IOException("Process exit with nonzero status of " +
                              exit_code + ": " + sbuf.toString());
      }

    } catch (InterruptedException e) {
      throw e;
    } finally {
      try { Thread.sleep(500); } catch (Exception e) {} 
      kill();

      // Kill both stdout/stderr copying threads
      if (logStdErrThread != null) {
        logStdErrThread.interrupt();
        try {
          logStdErrThread.join();
        } catch (InterruptedException ie) {}
      }

      if (logStdOutThread != null) {
        logStdOutThread.interrupt();
        try {
          logStdOutThread.join();
        } catch (InterruptedException ie) {}
      }
    }

  }

  /**
   * Kill the child process
   */
  public void kill() {
    if (process != null) {
      process.destroy();
      process = null; 
    }
    killed = true;
  }

  public boolean isRunning() { 
    if (process != null) 
      return true; 
    else
      return false; 
  }

  /**
   * Spawn a new thread to copy the child-jvm's stdout/stderr streams
   * via a {@link TaskLog.Writer}
   *
   * @param threadName thread name
   * @param stream child-jvm's stdout/stderr stream
   * @param writer {@link TaskLog.Writer} used to copy the child-jvm's data
   * @return Return the newly created thread
   */
  private Thread logStream(final String threadName,
                           final InputStream stream, 
                           final int outerr) {
    Thread loggerThread = new Thread() {
      public void run() {
        Writer taskLog = null; 
        Writer userOut = null; 
        try {
          long writedsize = 0; 
          byte[] buf = new byte[512];
          taskLog = getWriter(outerr); 
          userOut = getUserWriter(outerr); 
          while (!Thread.interrupted()) {
            if (process == null) break; 
            while (stream.available() > 0) {
              int alen = stream.available(); 
              int rlength = alen > buf.length ? buf.length : alen; 
              int n = stream.read(buf, 0, rlength);
              if (n > 0) {
                String data = new String(buf, 0, n); 
                taskLog.write(data); 
                taskLog.flush(); 
                if (userOut != null) {
                  userOut.write(data); 
                  userOut.flush(); 
                }
                writedsize += n; 
                if (rollingLogFile(outerr, writedsize)) {
                  try { taskLog.flush(); taskLog.close(); } catch (Exception e) {} 
                  taskLog = getWriter(outerr); 
                  writedsize = 0; 
                }
              }
            }
            try { Thread.sleep(500); } catch (Exception ex) {} 
          }
        } catch (IOException e) {
          LOG.warn("Error reading child output", e);
        //} catch (InterruptedException e) {
        //  // expected
        } finally {
          try {
            stream.close();
            if (taskLog != null) { taskLog.flush(); taskLog.close(); }
            if (userOut != null) { userOut.flush(); userOut.close(); }
            LOG.debug(threadName+" child output stream("+outerr+") thread exited."); 
          } catch (IOException e) {
            LOG.warn("Error closing child output", e);
          }
        }
      }
    };
    loggerThread.setName(threadName);
    loggerThread.setDaemon(true);
    loggerThread.start();

    return loggerThread;
  }

  public static String[] getCurrentEnvp(Configuration conf) {
    return getCurrentEnvp(conf, null); 
  }

  @SuppressWarnings("rawtypes")
  public static String[] getCurrentEnvp(Configuration conf, String[] envp) {
    // export parent env to child
    List<String> result = new ArrayList<String>();

    String libPath = null;

    Map<String,String> envs = System.getenv();
    for (Iterator it = envs.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      String val = (String) envs.get(key);
      if (key != null && val != null) {
        result.add(key+"="+val);
        if (key.equals("LD_LIBRARY_PATH"))
          libPath = val;
      }
    }

/***
    Properties p = System.getProperties();
    for (Iterator it = p.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      String val = (String) p.getProperty(key);
      if (key != null && val != null)
        result.add(key+"="+val);
    }
***/

    for (int i=0; envp != null && i < envp.length; i++) {
      String env = envp[i]; 
      if (env != null) result.add(env); 
    }

    String myLibPath = conf.get("env.ld.library.path");
    if (myLibPath != null) {
      if (libPath != null)
        libPath = libPath + ":" + myLibPath;
      else
        libPath = myLibPath;
    }
    if (libPath != null)
      result.add("LD_LIBRARY_PATH="+libPath);

    return result.toArray(new String[result.size()]);
  }

}

