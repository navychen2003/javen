package org.javenstudio.jfm.po;

import java.io.IOException; 
import java.io.OutputStream; 
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException; 
import java.io.File; 

import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;
import de.innosystec.unrar.Archive;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.filesystems.JFMFile;


public class ArchiveRar extends ArchiveBase {

  private long zipposition = 0; 

  public ArchiveRar() {
  }

  public void extract(JFMFile file, JFMFile dir) throws IOException, ActionCancelledException {
    File f = file.getLocalFile(); 
    if (f == null) {
      addTotalFinishedSize(file.length()); 
      updateProgress(0, 0); 
      Options.showMessage(Strings.format("Rar can only extract from local file system: %1$s",file.getPath())); 
      return; 
    }
    try {
      unRar(f, dir); 
    } catch (RarException re) {
      re.printStackTrace(); 
      throw new IOException(re.toString()); 
    } catch (FileNotFoundException fe) {
      fe.printStackTrace(); 
      throw new IOException(fe.toString()); 
    }
  }

  private long getZipReadCount(Archive rar) {
    long readsize = rar.getBytesRead(); 
    long size = readsize > zipposition ? readsize - zipposition : 0; 
    zipposition = readsize; 
    return size; 
  }

  private static final int BUFFER = 102400; 

  public void unRar(File file, JFMFile dir) 
      throws IOException, RarException, FileNotFoundException, ActionCancelledException {
    boolean canceled = isCanceled(); 
    if (canceled)
      throw new ActionCancelledException();

    Archive r = null; 
    try {
      r = new Archive(file); 
    } catch (RarException e) {
      throw new IOException(e.toString()); 
    }

    final Archive rar = r; 
    String zipName = file.getName(); 
    zipposition = 0; 

    FileHeader entry = null;
    while ((entry = rar.nextFileHeader()) != null) {
      if ((canceled = isCanceled())) break; 

      if (entry.isDirectory()) {
        dir.mkdir(entry.getFileNameString(), entry.getFileNameString()); 

      } else {
        ArchiveFile afile = new ArchiveFile(); 
        afile.entry = entry; 
        afile.name = entry.getFileNameString(); 
        afile.length = entry.getDataSize(); 
        afile.lastModified = entry.getMTime().getTime(); 

        JFMFile newfile = dir.createFile(afile.name, afile.name); 
        if (checkOverwriteFile(afile, newfile) == false) 
          continue; 
        if ((canceled = isCanceled())) break; 

        final long filesize = afile.length; 

        if (progress != null) {
          progress.setCopyingFile(zipName+":"+afile.name);
          progress.setCopyingTo(newfile.getParent());
          updateProgress(filesize, 0); 
        }

        JFMFile parentDst = newfile.getParentFile(); 
        if (parentDst != null) parentDst.mkdir(); 

        final OutputStream os = newfile.getOutputStream(); 
        OutputStream fos = new OutputStream() {
          private long writedsize = 0; 
          public void close() throws IOException { os.close(); } 
          public void flush() throws IOException { os.flush(); } 
          public void write(byte[] b) throws IOException {
            os.write(b); appendSize(b!=null?b.length:0); 
          }
          public void write(byte[] b, int off, int len) throws IOException {
            os.write(b, off, len); appendSize(len); 
          }
          public void write(int b) throws IOException {
            os.write(b); appendSize(1); 
          }
          private void appendSize(long count) {
            if (count > 0) {
              writedsize += count; 

              addTotalExtractedSize(count); 
              addTotalFinishedSize(getZipReadCount(rar)); 
              updateProgress(filesize, writedsize); 
            }
          }
        }; 

        BufferedOutputStream bos = new BufferedOutputStream(fos,BUFFER);           
        rar.extractFile(entry, bos);
        bos.flush();
        bos.close();

        if (canceled) { 
          newfile.delete(); 
          break; 
        }

        addTotalExtractedFiles(1); 
        updateProgress(filesize, filesize); 
      }
    }

    if (rar.getBytesTotal() > rar.getBytesRead()) 
      addTotalFinishedSize(rar.getBytesTotal() - rar.getBytesRead()); 
    updateProgress(0, 0); 

    if (canceled)
      throw new ActionCancelledException();
  }

}
