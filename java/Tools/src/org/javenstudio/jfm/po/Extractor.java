package org.javenstudio.jfm.po; 

import java.io.IOException; 
import org.javenstudio.jfm.filesystems.JFMFile; 

public interface Extractor {
  void setAction(ExtractAction action); 
  void setProgress(ProgressActionDialog dialog); 
  void extract(JFMFile file, JFMFile dir) throws IOException, ActionCancelledException; 
  boolean checkOverwriteFile(ArchiveFile fin, JFMFile fout) throws IOException, ActionCancelledException; 
  long getTotalArchivesSize();
  void addTotalFinishedSize(long size);
  void setTotalFinishedSize(long size);
  void addTotalExtractedSize(long size);
  void setTotalExtractedSize(long size);
  long getTotalExtractedSize();
  int getTotalArchivesFiles();
  int getTotalExtractedFiles();
  void addTotalExtractedFiles(int count);
}
