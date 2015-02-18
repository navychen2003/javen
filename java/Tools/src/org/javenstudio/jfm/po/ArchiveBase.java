package org.javenstudio.jfm.po;

import java.io.IOException; 
import org.javenstudio.jfm.filesystems.JFMFile;


public abstract class ArchiveBase implements Extractor, Archiver {

  protected ArchiveAction archiveAct = null; 
  protected ExtractAction extractAct = null; 
  protected ProgressActionDialog progress = null;

  public ArchiveBase() {
  }

  public void setAction(ExtractAction action) {
    this.extractAct = action; 
    this.archiveAct = null; 
  }
  public void setAction(ArchiveAction action) {
    this.archiveAct = action; 
    this.extractAct = null; 
  }

  public void setProgress(ProgressActionDialog dialog) {
    this.progress = dialog; 
  }

  public boolean isCanceled() {
    if (extractAct != null) 
      return extractAct.isCanceled(); 
    else if (archiveAct != null) 
      return archiveAct.isCanceled(); 
    return false; 
  }

  public void extract(JFMFile file, JFMFile dir) throws IOException, ActionCancelledException {
    throw new IOException("not implemented"); 
  }

  public void archive(JFMFile[] files, JFMFile dir) throws IOException, ActionCancelledException {
    throw new IOException("not implemented"); 
  }

  public boolean checkOverwriteFile(ArchiveFile fin, JFMFile fout) throws IOException, ActionCancelledException {
    if (extractAct != null) 
      return extractAct.checkOverwriteFile(fin, fout); 
    else
      return true; 
  }
  public boolean checkOverwriteFile(JFMFile fout) throws IOException, ActionCancelledException {
    if (archiveAct != null) 
      return archiveAct.checkOverwriteFile(fout); 
    else
      return true; 
  }

  public long getTotalArchivesSize() { 
    return extractAct != null ? extractAct.totalArchivesSize : 0; 
  } 

  public long getTotalFilesSize() {
    return archiveAct != null ? archiveAct.totalFilesSize : 0; 
  }

  public synchronized void addTotalFinishedSize(long size) {
    if (extractAct != null) 
      extractAct.totalFinishedSize += size;
    else if (archiveAct != null) 
      archiveAct.totalFinishedSize += size; 
  }

  public synchronized void setTotalFinishedSize(long size) {
    if (extractAct != null) 
      extractAct.totalFinishedSize = size;
    else if (archiveAct != null) 
      archiveAct.totalFinishedSize = size;
  }

  public long getTotalFinishedSize() {
    if (extractAct != null) 
      return extractAct.totalFinishedSize;
    else if (archiveAct != null) 
      return archiveAct.totalFinishedSize; 
    return 0; 
  }

  public synchronized void addTotalExtractedSize(long size) { 
    if (extractAct != null) 
      extractAct.totalExtractedSize += size; 
  }

  public synchronized void setTotalExtractedSize(long size) { 
    if (extractAct != null) 
      extractAct.totalExtractedSize = size; 
  }

  public long getTotalExtractedSize() { 
    return extractAct != null ? extractAct.totalExtractedSize : 0; 
  }

  public synchronized void addTotalArchivedSize(long size) {
    if (archiveAct != null)
      archiveAct.totalArchivedSize += size;
  }

  public synchronized void setTotalArchivedSize(long size) {
    if (archiveAct != null)
      archiveAct.totalArchivedSize = size;
  }

  public long getTotalArchivedSize() {
    return archiveAct != null ? archiveAct.totalArchivedSize : 0;
  }

  public int getTotalArchivesFiles() {
    return extractAct != null ? extractAct.totalArchivesFiles : 0; 
  }

  public int getTotalSelectedFiles() {
    return archiveAct != null ? archiveAct.totalSelectedFiles : 0; 
  }

  public int getTotalExtractedFiles() {
    return extractAct != null ? extractAct.totalExtractedFiles : 0; 
  }

  public synchronized void addTotalExtractedFiles(int count) {
    if (extractAct != null) extractAct.totalExtractedFiles += count; 
  }

  public int getTotalArchivedFiles() {
    return archiveAct != null ? archiveAct.totalArchivedFiles : 0;
  }

  public synchronized void addTotalArchivedFiles(int count) {
    if (archiveAct != null) archiveAct.totalArchivedFiles += count;
  }

  public synchronized void updateProgress(long filesize, long writedsize) {
    if (progress == null) return; 
    if (extractAct != null) {
      progress.setProgressValue(
          getTotalArchivesSize(), getTotalFinishedSize(), getTotalExtractedSize(), 
          filesize, writedsize,
          getTotalArchivesFiles(), getTotalExtractedFiles());

    } else if (archiveAct != null) {
      progress.setProgressValue(
          getTotalFilesSize(), getTotalFinishedSize(), getTotalArchivedSize(), 
          filesize, writedsize,
          getTotalSelectedFiles(), getTotalArchivedFiles());
    }
  }

}
