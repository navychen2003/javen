package org.javenstudio.raptor.fs;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;

import java.util.StringTokenizer;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.util.Shell;

/** Filesystem disk space usage statistics.  Uses the unix 'df' program.
 * Tested on Linux, FreeBSD, Cygwin. */
public class DF extends Shell {
  public static final long DF_INTERVAL_DEFAULT = 3 * 1000; // default DF refresh interval 
  
  private String  dirPath;
  private String filesystem;
  private long capacity;
  private long used;
  private long available;
  private int percentUsed;
  private String mount;
  
  public DF(File path, Configuration conf) throws IOException {
    this(path, conf.getLong("dfs.df.interval", DF.DF_INTERVAL_DEFAULT));
  }

  public DF(File path, long dfInterval) throws IOException {
    super(dfInterval);
    this.dirPath = path.getCanonicalPath();
    run();
  }
  
  /// ACCESSORS

  public String getDirPath() {
    return dirPath;
  }
  
  public String getFilesystem() throws IOException { 
    run(); 
    return filesystem; 
  }
  
  public long getCapacity() throws IOException { 
    run(); 
    return capacity; 
  }
  
  public long getUsed() throws IOException { 
    run(); 
    return used;
  }
  
  public long getAvailable() throws IOException { 
    run(); 
    return available;
  }
  
  public int getPercentUsed() throws IOException {
    run();
    return percentUsed;
  }

  public String getMount() throws IOException {
    run();
    return mount;
  }
  
  @Override
  public String toString() {
    return
      "df -k " + mount +"\n" +
      filesystem + "\t" +
      capacity / 1024 + "\t" +
      used / 1024 + "\t" +
      available / 1024 + "\t" +
      percentUsed + "%\t" +
      mount;
  }

  @Override
  protected String[] getExecString() {
    // ignoring the error since the exit code it enough
	if (Shell.WINDOWS) return null;
    return new String[] {"bash","-c","exec 'df' '-k' '" + dirPath 
                         + "' 2>/dev/null"};
  }
  
  @Override
  protected void runNoCommand() {
	File file = new File(dirPath);
	long totalSpace = file.getTotalSpace();
	long usableSpace = file.getUsableSpace();
	long usedSpace = totalSpace > usableSpace ? (totalSpace - usableSpace) : 0;
	float usedRate = totalSpace > 0 ? ((float)usedSpace / (float)totalSpace) : 100.0f;
	
	this.capacity = totalSpace;
	this.used = usedSpace;
	this.available = usableSpace;
	this.percentUsed = totalSpace > 0 ? (int)(usedRate * 100.0f) : 100;
	this.mount = Path.getWindowsDrive(file.getAbsolutePath(), false);
  }
  
  @Override
  protected void parseExecResult(BufferedReader lines) throws IOException {
    lines.readLine();                         // skip headings
  
    String line = lines.readLine();
    if (line == null) {
      throw new IOException( "Expecting a line not the end of stream" );
    }
    StringTokenizer tokens =
      new StringTokenizer(line, " \t\n\r\f%");
    
    this.filesystem = tokens.nextToken();
    if (!tokens.hasMoreTokens()) {            // for long filesystem name
      line = lines.readLine();
      if (line == null) {
        throw new IOException( "Expecting a line not the end of stream" );
      }
      tokens = new StringTokenizer(line, " \t\n\r\f%");
    }
    this.capacity = Long.parseLong(tokens.nextToken()) * 1024;
    this.used = Long.parseLong(tokens.nextToken()) * 1024;
    this.available = Long.parseLong(tokens.nextToken()) * 1024;
    this.percentUsed = Integer.parseInt(tokens.nextToken());
    this.mount = tokens.nextToken();
  }

  public static void main(String[] args) throws Exception {
    String path = ".";
    if (args.length > 0)
      path = args[0];

    System.out.println(new DF(new File(path), DF_INTERVAL_DEFAULT).toString());
  }
}

