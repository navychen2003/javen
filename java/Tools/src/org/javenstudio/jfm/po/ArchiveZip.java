package org.javenstudio.jfm.po;

import java.io.IOException; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.CRC32;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.javenstudio.jfm.filesystems.JFMFile;


public class ArchiveZip extends ArchiveBase {

  public ArchiveZip() {
  }

  public void extract(JFMFile file, JFMFile dir) throws IOException, ActionCancelledException {
    unZip(file, dir); 
  }

  public void archive(JFMFile[] files, JFMFile dest) throws IOException, ActionCancelledException {
    zip(files, dest); 
  }

  private static final int BUFFER = 102400; 

  public void zip(JFMFile[] files, JFMFile dest) throws IOException, ActionCancelledException {
    if (isCanceled())
      throw new ActionCancelledException();

    if (checkOverwriteFile(dest) == false)
      return;
    if (isCanceled()) 
      throw new ActionCancelledException();

    try {
      OutputStream fos = dest.getOutputStream(); 
      CheckedOutputStream cos = new CheckedOutputStream(fos, new CRC32());
      ZipOutputStream out = new ZipOutputStream(cos);

      compress(files, out, dest.getName()); 

      out.flush(); out.close(); 
      cos.close(); fos.close(); 

    } catch (IOException ie) {
      try { dest.delete(); } catch (IOException e) {}
      throw ie; 
    } catch (ActionCancelledException ae) {
      try { dest.delete(); } catch (IOException e) {}
      throw ae; 
    }
  }

  private void compress(JFMFile[] files, ZipOutputStream out, String zipname) 
      throws IOException, ActionCancelledException {   
    if (isCanceled())
      throw new ActionCancelledException();

    String basedir = ""; 
    for (int i=0; files != null && i < files.length; i++) {
      if (isCanceled())
        throw new ActionCancelledException();

      if (progress != null) {
        progress.setCopyingFile(files[i].getPath());
        progress.setCopyingTo(zipname+":"+basedir);
        updateProgress(0, 0); 
      }

      compress(files[i], out, basedir, zipname); 
    }
  }

  private void compress(JFMFile file, ZipOutputStream out, String basedir, String zipname) 
      throws IOException, ActionCancelledException {   
    if (isCanceled())
      throw new ActionCancelledException();

    if (file == null) return; 
    if (file.isDirectory()) {   
      compressDirectory(file, out, basedir, zipname); 
    } else { 
      compressFile(file, out, basedir, zipname); 
    } 
  } 

  private void compressDirectory(JFMFile dir, ZipOutputStream out, String basedir, String zipname) 
      throws IOException, ActionCancelledException {
    if (isCanceled())
      throw new ActionCancelledException();

    if (dir == null || !dir.exists()) return; 

    JFMFile[] files = dir.listFiles(); 
    for (int i = 0; files != null && i < files.length; i++) { 
      if (isCanceled())
        throw new ActionCancelledException();

      if (progress != null) {
        progress.setCopyingFile(files[i].getPath());
        progress.setCopyingTo(zipname+":"+basedir);
        updateProgress(0, 0); 
      }

      compress(files[i], out, basedir + dir.getName() + "/", zipname); 
    } 
  } 

  private void compressFile(JFMFile file, ZipOutputStream out, String basedir, String zipname) 
      throws IOException, ActionCancelledException { 
    if (isCanceled())
      throw new ActionCancelledException();

    if (file == null || !file.exists()) return; 
    long filelength = file.length(); 
    long filepos = 0; 

    if (progress != null) {
      progress.setCopyingFile(file.getPath());
      progress.setCopyingTo(zipname+":"+basedir);
      updateProgress(filelength, 0); 
    }

    InputStream fis = file.getInputStream(); 
    try { 
      BufferedInputStream bis = new BufferedInputStream(fis); 
      ZipEntry entry = new ZipEntry(basedir + file.getName()); 
      entry.setSize(file.length()); 
      entry.setTime(file.lastModified()); 
      out.putNextEntry(entry); 
      int count = 0; 
      byte data[] = new byte[BUFFER]; 
      while ((count = bis.read(data, 0, BUFFER)) >= 0) { 
        if (isCanceled())
          throw new ActionCancelledException();

        if (count > 0) {
          out.write(data, 0, count); 
          filepos += count; 

          addTotalArchivedSize(count); 
          addTotalFinishedSize(count); 
          updateProgress(filelength, filepos); 
        }
      } 
      bis.close(); 
    } catch (IOException ie) { 
      throw ie; 
    } catch (ActionCancelledException ae) { 
      throw ae; 
    } finally {
      try { fis.close(); } catch (Exception e) {}
    }

    if (filelength > filepos) 
      addTotalFinishedSize(filelength - filepos); 
    addTotalArchivedFiles(1); 
    updateProgress(0, 0); 
  } 

  public void unZip(JFMFile file, JFMFile dir) throws IOException, ActionCancelledException {
    final InputStream fis = file.getInputStream(); 
    try {
      unZip0(file, dir, fis); 
      fis.close(); 
    } catch (IOException ie) {
      try { fis.close(); } catch (Exception e) {}
      throw ie; 
    } catch (ActionCancelledException ae) {
      try { fis.close(); } catch (Exception e) {}
      throw ae; 
    }
  }

  @SuppressWarnings("resource")
  private void unZip0(JFMFile file, JFMFile dir, final InputStream fis) 
      throws IOException, ActionCancelledException {
    boolean canceled = isCanceled(); 
    if (canceled)
      throw new ActionCancelledException();

    long finishedSize = getTotalFinishedSize(); 
    long fileSize = file.length(); 
    String zipName = file.getName(); 

    InputStream is = new InputStream() {
      public int available() throws IOException { return fis.available(); }
      public void close() throws IOException { fis.close(); }
      public void mark(int readlimit) { fis.mark(readlimit); } 
      public boolean markSupported() { return fis.markSupported(); }
      public int read() throws IOException { 
        int b = fis.read(); if (b != -1) addTotalFinishedSize(1); 
        return b; 
      }
      public int read(byte[] b) throws IOException {
        int n = fis.read(b); if (n > 0) addTotalFinishedSize(n); 
        return n; 
      }
      public int read(byte[] b, int off, int len) throws IOException {
        int n = fis.read(b, off, len); if (n > 0) addTotalFinishedSize(n); 
        return n; 
      }
      public void reset() throws IOException { fis.reset(); } 
      public long skip(long n) throws IOException {
        long s = fis.skip(n); if (s > 0) addTotalFinishedSize(s); 
        return s; 
      }
    }; 

    java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is); 
    java.util.zip.ZipEntry entry = null; 

    while ((entry = zis.getNextEntry()) != null) {
      if ((canceled = isCanceled())) break; 

      if (entry.isDirectory()) {
        dir.mkdir(entry.getName(), entry.getName()); 
        addTotalExtractedFiles(1); 

      } else {
        ArchiveFile afile = new ArchiveFile(); 
        afile.entry = entry; 
        afile.name = entry.getName(); 
        afile.length = entry.getSize(); 
        afile.lastModified = entry.getTime(); 

        JFMFile newfile = dir.createFile(entry.getName(), entry.getName()); 
        if (checkOverwriteFile(afile, newfile) == false) 
          continue; 
        if ((canceled = isCanceled())) break; 

        long filesize = afile.length, writedsize = 0; 

        if (progress != null) {
          progress.setCopyingFile(zipName+":"+afile.name);
          progress.setCopyingTo(newfile.getParent());
          updateProgress(filesize, 0); 
        }

        JFMFile parentDst = newfile.getParentFile(); 
        if (parentDst != null) parentDst.mkdir(); 

        try {
          OutputStream fos = newfile.getOutputStream(); 
          BufferedOutputStream bos = new BufferedOutputStream(fos,BUFFER);           

          int count = -1;
          byte data[] = new byte[BUFFER];
          while ((count = zis.read(data, 0, BUFFER)) >= 0) {
            if ((canceled = isCanceled())) break; 

            if (count > 0) {
              bos.write(data, 0, count); 
              writedsize += count; 

              addTotalExtractedSize(count); 
              updateProgress(filesize, writedsize); 
            }
          }
          bos.flush();
          bos.close();
        } catch (IOException ex) {
          try { newfile.delete(); is.close(); } catch (Exception e) {}
          throw ex; 
        }

        if (canceled) { 
          try { newfile.delete(); } catch (Exception e) {}
          break; 
        }

        addTotalExtractedFiles(1); 
        updateProgress(filesize, writedsize); 
      }
    }

    try { zis.close(); } catch (Exception e) {}
    try { is.close(); } catch (Exception e) {}

    long fileSizeRead = getTotalFinishedSize() - finishedSize; 
    if (fileSizeRead < fileSize) 
      addTotalFinishedSize(fileSize - fileSizeRead); 

    updateProgress(0, 0); 

    if (canceled)
      throw new ActionCancelledException();
  }

}
