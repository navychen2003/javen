package org.javenstudio.raptor.fs.shell;

import java.io.*;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.Configured;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.ipc.RemoteException;

/**
 * An abstract class for the execution of a file system command
 */
abstract public class Command extends Configured {
  protected String[] args;
  
  /** Constructor */
  protected Command(Configuration conf) {
    super(conf);
  }
  
  /** Return the command's name excluding the leading character - */
  abstract public String getCommandName();
  
  /** 
   * Execute the command on the input path
   * 
   * @param path the input path
   * @throws IOException if any error occurs
   */
  abstract protected void run(Path path) throws IOException;
  
  /** 
   * For each source path, execute the command
   * 
   * @return 0 if it runs successfully; -1 if it fails
   */
  public int runAll() {
    int exitCode = 0;
    for (String src : args) {
      try {
        Path srcPath = new Path(src);
        FileSystem fs = srcPath.getFileSystem(getConf());
        FileStatus[] statuses = fs.globStatus(srcPath);
        if (statuses == null) {
          System.err.println("Can not find listing for " + src);
          exitCode = -1;
        } else {
          for(FileStatus s : statuses) {
            run(s.getPath());
          }
        }
      } catch (RemoteException re) {
        exitCode = -1;
        String content = re.getLocalizedMessage();
        int eol = content.indexOf('\n');
        if (eol>=0) {
          content = content.substring(0, eol);
        }
        System.err.println(getCommandName() + ": " + content);
      } catch (IOException e) {
        exitCode = -1;
        System.err.println(getCommandName() + ": " + e.getLocalizedMessage());
      }
    }
    return exitCode;
  }
}

