package org.javenstudio.jfm.views.list; 

import java.util.Vector; 
import org.javenstudio.jfm.filesystems.JFMFile;

public interface JFMModel {
  public int getRowCount();
  public JFMFile getFileAt(int row);
  public JFMFile getWorkingDirectory(); 
  public void browseDirectory(JFMFile file); 
  @SuppressWarnings("rawtypes")
  public Vector getCurrentFiles();  
}
