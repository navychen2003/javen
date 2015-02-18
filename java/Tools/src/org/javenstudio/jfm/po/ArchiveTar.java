package org.javenstudio.jfm.po;

import java.io.BufferedInputStream; 
import java.io.BufferedOutputStream; 
import java.io.IOException; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.util.zip.GZIPInputStream; 
import java.util.zip.GZIPOutputStream; 

import org.apache.tools.tar.TarEntry; 
import org.apache.tools.tar.TarInputStream; 
import org.apache.tools.tar.TarOutputStream; 
import org.apache.tools.bzip2.CBZip2InputStream; 
import org.apache.tools.bzip2.CBZip2OutputStream; 

import org.javenstudio.jfm.filesystems.JFMFile; 


public class ArchiveTar extends ArchiveBase {

  public ArchiveTar() {
  }

  public void extract(JFMFile file, JFMFile dir) throws IOException, ActionCancelledException {
    if (file == null || dir == null) return; 
    String name = file.getName().toLowerCase(); 
    if (name.endsWith(".tar")) {
      unTar(file, dir);
    } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
      unTarGz(file, dir); 
    } else if (name.endsWith(".tar.bz2")) {
      unTarBz2(file, dir); 
    }
  }

  public void archive(JFMFile[] files, JFMFile dest) throws IOException, ActionCancelledException {
    if (files == null || files.length == 0 || dest == null) 
      return; 
    String name = dest.getName().toLowerCase(); 
    if (name.endsWith(".tar")) {
      tar(files, dest);
    } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
      tarGz(files, dest);
    } else if (name.endsWith(".tar.bz2")) {
      tarBz2(files, dest);
    }
  }

  public void tarGz(JFMFile[] files, JFMFile dest) throws IOException, ActionCancelledException {
    if (isCanceled())
      throw new ActionCancelledException();

    if (checkOverwriteFile(dest) == false)
      return;

    if (isCanceled())
      throw new ActionCancelledException();

    OutputStream fout = dest.getOutputStream(); 
    GZIPOutputStream gout = new GZIPOutputStream(fout, BUFFER_SIZE); 
    tar0(files, dest, gout); 
    fout.flush(); fout.close(); 
  }

  public void tarBz2(JFMFile[] files, JFMFile dest) throws IOException, ActionCancelledException {
    if (isCanceled())
      throw new ActionCancelledException();

    if (checkOverwriteFile(dest) == false)
      return;

    if (isCanceled())
      throw new ActionCancelledException();

    OutputStream fout = dest.getOutputStream(); 
    BufferedOutputStream bout = new BufferedOutputStream(fout); 
    bout.write('B');
    bout.write('Z');
    CBZip2OutputStream zout = new CBZip2OutputStream(bout);
    tar0(files, dest, zout); 
    bout.flush(); bout.close(); 
    fout.flush(); fout.close(); 
  }

  public void tar(JFMFile[] files, JFMFile dest) throws IOException, ActionCancelledException {
    if (isCanceled())
      throw new ActionCancelledException();

    if (checkOverwriteFile(dest) == false)
      return;

    if (isCanceled())
      throw new ActionCancelledException();

    OutputStream fout = dest.getOutputStream(); 
    BufferedOutputStream bout = new BufferedOutputStream(fout); 
    tar0(files, dest, bout); 
    fout.flush(); fout.close(); 
  }

  private void tar0(JFMFile[] files, JFMFile dest, OutputStream fos) throws IOException, ActionCancelledException {
    if (isCanceled())
      throw new ActionCancelledException();

    try {
      TarOutputStream out = new TarOutputStream(fos); 
      out.setLongFileMode(TarOutputStream.LONGFILE_GNU); 

      String dir = "", zipname = dest.getName(); 
      for (int i=0; files != null && i < files.length; i++) {
        if (isCanceled())
          throw new ActionCancelledException();

        doTar(files[i], out, dir, zipname, true); 
      }
      out.flush(); out.close(); 
      fos.flush(); fos.close(); 
 
    } catch (IOException ie) {
      try { dest.delete(); } catch (IOException e) {}
      throw ie;
    } catch (ActionCancelledException ae) {
      try { dest.delete(); } catch (IOException e) {}
      throw ae;
    }
  }

  private void doTar(JFMFile file, TarOutputStream out, String dir, String zipname, 
                     boolean includeEmptyDir) throws IOException, ActionCancelledException { 
    if (file == null || out == null) return; 
    if (isCanceled())
      throw new ActionCancelledException();

    if (file.isDirectory()) {
      JFMFile[] listFiles = file.listFiles();
      if (listFiles == null || listFiles.length == 0) {
        if (includeEmptyDir) {
          if (progress != null) {
            progress.setCopyingFile(file.getPath());
            progress.setCopyingTo(zipname+":"+dir);
            updateProgress(0, 0);
          }

          out.putNextEntry(new TarEntry(dir + file.getName() + "/"));
          addTotalArchivedFiles(1);
        }
        return; 

      } else { 
        for (JFMFile cfile: listFiles) { 
          if (isCanceled())
            throw new ActionCancelledException();

          if (progress != null) {
            progress.setCopyingFile(cfile.getPath());
            progress.setCopyingTo(zipname+":"+dir);
            updateProgress(0, 0);
          }

          doTar(cfile, out, dir + file.getName() + "/", zipname, includeEmptyDir);
        } 
      } 

    } else {
      if (file == null || !file.exists()) return;
      long filelength = file.length();
      long filepos = 0;

      if (progress != null) {
        progress.setCopyingFile(file.getPath());
        progress.setCopyingTo(zipname+":"+dir);
        updateProgress(filelength, 0);
      }

      TarEntry ze = new TarEntry(dir+file.getName());
      ze.setSize(file.length()); 
      ze.setModTime(file.lastModified()); 
      //ze.setName(file.getName());
      out.putNextEntry(ze);

      InputStream fis = null; 
      byte[] bt = new byte[BUFFER_SIZE]; 
      try { 
        fis = file.getInputStream(); 
        int count = 0; 
        while ((count = fis.read(bt)) >= 0) {
          if (isCanceled())
            throw new ActionCancelledException();

          if (count > 0) {
            out.write(bt, 0, count); 
            filepos += count;

            addTotalArchivedSize(count);
            addTotalFinishedSize(count);
            updateProgress(filelength, filepos);
          }
        } 
      } catch(IOException ex) { 
        throw ex; 
      } finally { 
        try { 
          if (fis != null) fis.close();
          out.closeEntry(); 
        } catch(IOException ex) { 
          throw ex; 
        } 
      } 

      if (filelength > filepos)
        addTotalFinishedSize(filelength - filepos);
      addTotalArchivedFiles(1);
      updateProgress(0, 0);
    } 
  } 

  private static final int BUFFER_SIZE = 102400; 

  public void unTar(JFMFile file, JFMFile outputDir) throws IOException, ActionCancelledException { 
    boolean canceled = isCanceled();
    if (canceled)
      throw new ActionCancelledException();

    InputStream is = null; 
    try {
      is = openInputStream(file.getInputStream()); 
      TarInputStream tarIn = 
          new TarInputStream(is, BUFFER_SIZE); 
      unTar0(file, outputDir, is, tarIn); 
    } catch (IOException ie) {
      throw ie; 
    } catch (ActionCancelledException ae) {
      throw ae; 
    } finally {
      try { if (is != null) is.close(); } catch (Exception e) {}
    }
  }

  private void unTar0(JFMFile file, JFMFile outputDir, InputStream is, TarInputStream tarIn) 
      throws IOException, ActionCancelledException { 
    boolean canceled = isCanceled();
    if (canceled)
      throw new ActionCancelledException();

    String zipName = file.getName(); 
    try { 
      outputDir.mkdir(); 
      TarEntry entry = null; 
      while ((entry = tarIn.getNextEntry()) != null) { 
        if ((canceled = isCanceled())) break;

        if (entry.isDirectory()) {
          outputDir.mkdir(entry.getName(), entry.getName());
          addTotalExtractedFiles(1);

        } else {
          ArchiveFile afile = new ArchiveFile();
          afile.entry = entry;
          afile.name = entry.getName();
          afile.length = entry.getSize();
          afile.lastModified = entry.getModTime().getTime();

          JFMFile tmpFile = outputDir.createFile(entry.getName(), entry.getName()); 
          if (checkOverwriteFile(afile, tmpFile) == false)
            continue;
          if ((canceled = isCanceled())) break;

          long filesize = afile.length, writedsize = 0;

          if (progress != null) {
            progress.setCopyingFile(zipName+":"+afile.name);
            progress.setCopyingTo(tmpFile.getParent());
            updateProgress(filesize, 0); 
          }

          JFMFile parentDir = tmpFile.getParentFile(); 
          if (parentDir != null) parentDir.mkdir(); 

          try { 
            OutputStream fos = tmpFile.getOutputStream(); 
            BufferedOutputStream bos = new BufferedOutputStream(fos,BUFFER_SIZE);

            int length = 0; 
            byte[] b = new byte[BUFFER_SIZE]; 
            while ((length = tarIn.read(b)) >= 0) { 
              if ((canceled = isCanceled())) break;

              if (length > 0) {
                bos.write(b, 0, length); 
                writedsize += length; 

                addTotalExtractedSize(length);
                updateProgress(filesize, writedsize); 
              }
            } 
            bos.flush(); 
            bos.close(); 
          } catch(IOException ex) { 
            throw ex; 
          } finally { 
            try { tmpFile.delete(); } catch (Exception e) {}
          } 

          if (canceled) {
            try { tmpFile.delete(); } catch (Exception e) {}
            break;
          }

          addTotalExtractedFiles(1);
          updateProgress(filesize, writedsize); 
        } 
      } 
    } catch(IOException ex) { 
      throw ex;   
    } finally { 
      try { 
        if (tarIn != null) 
          tarIn.close(); 
      } catch(IOException ex) { 
        throw ex; 
      } 
    } 

    updateProgress(0, 0); 

    if (canceled)
      throw new ActionCancelledException();
  } 

  public void unTarGz(JFMFile file, JFMFile outputDir) throws IOException, ActionCancelledException {
    boolean canceled = isCanceled();
    if (canceled)
      throw new ActionCancelledException();

    InputStream is = null; 
    try {
      is = openInputStream(file.getInputStream()); 
      TarInputStream tarIn = 
              new TarInputStream(new GZIPInputStream( 
                new BufferedInputStream(is)), BUFFER_SIZE); 
      unTar0(file, outputDir, is, tarIn); 
    } catch (IOException ie) {
      throw ie; 
    } catch (ActionCancelledException ae) {
      throw ae; 
    } finally {
      try { if (is != null) is.close(); } catch (Exception e) {}
    }
  } 

  public void unTarBz2(JFMFile file, JFMFile outputDir) throws IOException, ActionCancelledException {
    boolean canceled = isCanceled();
    if (canceled)
      throw new ActionCancelledException();

    InputStream is = null; 
    try {
      is = openInputStream(file.getInputStream()); 
      int b = is.read();
      if (b != 'B') 
        throw new IOException("Invalid bz2 file: " + file.getPath());
      b = is.read();
      if (b != 'Z') 
        throw new IOException("Invalid bz2 file: " + file.getPath());

      TarInputStream tarIn = 
              new TarInputStream(new CBZip2InputStream( 
                new BufferedInputStream(is)), BUFFER_SIZE); 
      unTar0(file, outputDir, is, tarIn); 
    } catch (IOException ie) {
      throw ie; 
    } catch (ActionCancelledException ae) {
      throw ae; 
    } finally {
      try { if (is != null) is.close(); } catch (Exception e) {}
    }
  } 

  private InputStream openInputStream(final InputStream fis) {
    return new InputStream() {
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
  }
}
