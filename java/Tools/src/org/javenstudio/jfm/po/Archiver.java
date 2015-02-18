package org.javenstudio.jfm.po; 

import java.io.IOException; 
import org.javenstudio.jfm.filesystems.JFMFile; 

public interface Archiver {
  void setAction(ArchiveAction action); 
  void setProgress(ProgressActionDialog dialog); 
  void archive(JFMFile[] files, JFMFile dir) throws IOException, ActionCancelledException; 
  boolean checkOverwriteFile(JFMFile fout) throws IOException, ActionCancelledException; 
  long getTotalFilesSize();
  void addTotalFinishedSize(long size);
  void setTotalFinishedSize(long size);
  void addTotalArchivedSize(long size);
  void setTotalArchivedSize(long size);
  long getTotalArchivedSize();
  int getTotalSelectedFiles();
  int getTotalArchivedFiles();
  void addTotalArchivedFiles(int count);
}
