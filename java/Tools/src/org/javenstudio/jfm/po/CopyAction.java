package org.javenstudio.jfm.po;

import java.io.IOException; 
import java.io.InputStream; 
import java.io.OutputStream; 
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.event.*;
import org.javenstudio.jfm.filesystems.JFMFile;


public class CopyAction extends AbstractAction {
  private static final long serialVersionUID = 1L;
  
  private int totalFilesCount = 0; 
  private int copiedFilesCount = 0; 
  private long totalFilesSizes = 0;
  private long totalBytesWritten = 0;
  private boolean overwriteAll = false;
  private boolean skipAll = false;
  private boolean cancel = false;
  private ProgressActionDialog progress = null;

  public CopyAction() {
  }

  public String getName() {
     return Strings.get("Copy to ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_C);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl C"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/edit-copy.png")));
  }

  private void resetAll() {
    totalFilesCount = 0;
    copiedFilesCount = 0;
    totalFilesSizes = 0;
    totalBytesWritten = 0;
    overwriteAll = false;
    skipAll = false;
    cancel = false;
  }

  public boolean isMoveOperation() {
    return false; 
  }

  public void actionPerformed(ActionEvent e) {
     //the vectors should never be null, and should contain at least one element
     //it's the views bussiness to do this and THEY MUST DO THIS,unless it could get ugly
    final JFMFile[] filesSrc = Options.getActivePanel().getSelectedFiles();
    if (filesSrc == null || filesSrc.length == 0)
      return;

    @SuppressWarnings("unused")
	final JFMFile dirSrc = Options.getActivePanel().getCurrentWorkingDirectory();
    final JFMFile dirDst = Options.getInactivePanel().getCurrentWorkingDirectory();

    String fsNameSrc = Options.getActivePanel().getFsName();
    String fsNameDst = Options.getInactivePanel().getFsName();

    String title = "Copy"; 
    //if (isMoveOperation()) title = "Move"; 
    //title = title + " from " + fsNameSrc + " to " + fsNameDst; 

    if (isMoveOperation())
    	title = Strings.format("Move from %1$s to %2$s", fsNameSrc, fsNameDst);
    else
    	title = Strings.format("Copy from %1$s to %2$s", fsNameSrc, fsNameDst);
    
    CopyConfirmDialog d = new CopyConfirmDialog(Options.getMainFrame(),title,true);
    if (isMoveOperation()) d.setOperation(ProgressActionDialog.MOVE); 
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setCopyFrom(filesSrc);
    d.setCopyTo(dirDst);
    d.setVisible(true);
    if(d.isCancelled()) return;

    progress = new ProgressActionDialog(Options.getMainFrame(),title,true);
    progress.setLocationRelativeTo(Options.getMainFrame());
    if (isMoveOperation()) progress.setOperation(ProgressActionDialog.MOVE); 

    resetAll(); 

    progress.startAction(new ActionExecuter() {
      public void start() {
        try {
          copyFiles(filesSrc, dirDst);
        } catch (ActionCancelledException ex) {
          this.cancel();
        } catch (IOException e) {
          this.cancel();
          Options.showMessage(e); 
        }

        ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
        ev.setSource(CopyAction.this);
        ev.setDirectory(dirDst);
        Broadcaster.notifyChangeDirectoryListeners(ev);
      }

      public void cancel() {
        cancel = true;
      }
    });
  }

  private long scanFilesSize(JFMFile[] files) throws IOException {
    long totalSizes = 0;
    for (int i=0; files != null && i<files.length; i++) {
      JFMFile f = (JFMFile)files[i];
      progress.setScaningFile(f.getPath()); 
      if (f.isDirectory()) {
        JFMFile[] subFiles = f.listFiles(); 
        if (subFiles != null && subFiles.length > 0) 
          totalSizes += scanFilesSize(subFiles);
        else
          totalFilesCount ++; // empty dir
      } else {
        totalSizes += f.length();
        totalFilesCount ++; 
      }
    }
    return totalSizes;
  }

  private boolean copyDir(JFMFile dir, JFMFile dest) throws IOException, ActionCancelledException {
    JFMFile[] f = dir.listFiles();
    if (f == null || f.length == 0) {
      copiedFilesCount ++;  // empty dir
      return true;
    }
    boolean result = true; 
    for (int i=0; i < f.length; i++) { 
      JFMFile file = f[i]; 
      if (file.isDirectory()) {
        JFMFile destFile = dest.mkdir(file.getName(), file.getAbsolutePath());
        if (copyDir(file, destFile) == false) 
          result = false; 
        else if (isMoveOperation()) 
          file.delete(); // all files moved
      } else {
        JFMFile destFile = dest.createFile(file.getName(), file.getAbsolutePath());
        if (copyFile(file, destFile) == false)
          result = false; 
      }
    }
    return result; 
  }

  @SuppressWarnings("resource")
  private boolean copyFile(JFMFile fin, JFMFile fout) throws IOException, ActionCancelledException {
    boolean result = false; 

    if (fin.getFsName().equals(fout.getFsName()) &&
        fin.getFsSchemeName().equals(fout.getFsSchemeName()) &&
        fout.exists() && fin.equals(fout)) {
      long filesize = fin.length();
      totalBytesWritten += filesize;
      copiedFilesCount ++;
      progress.setProgressValue(
          totalFilesSizes, totalBytesWritten, filesize, filesize,
          totalFilesCount, copiedFilesCount);
      return false;
    }

    if (fout.exists() && !overwriteAll && !skipAll) {
      progress.setBreakStart(); 

      String message = Strings.format(
    	  "Target file %1$s already exists.\n\nSource last modified date: %2$s (%3$s bytes)\nTarget last modified date: %4$s (%5$s bytes)\n\nWhat should I do?", 
          fout.getPath(), Options.formatTime(fin.lastModified()), ""+fin.length(), 
          Options.formatTime(fout.lastModified()), ""+fout.length());

      String[] buttons = new String[]{Strings.get("Overwrite"),Strings.get("Overwrite all"),Strings.get("Skip"),Strings.get("Skip all"),Strings.get("Cancel")};

      int selected = JOptionPane.showOptionDialog(progress, 
          message, Strings.get("File exists"), 
          JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, 
          null, buttons, buttons[2]);

      progress.setBreakStop(); 

      switch (selected) {
        case 0:
          break;
        case 1:
          overwriteAll = true;
          break;
        case 2:
          long filesize = fin.length(); 
          totalBytesWritten += filesize;          
          copiedFilesCount ++; 
          progress.setProgressValue(
              totalFilesSizes, totalBytesWritten, filesize, filesize, 
              totalFilesCount, copiedFilesCount); 
          return false;
        case 3:
          skipAll = true;
          break;
        case 4:
          throw new ActionCancelledException();
      }
    }

    if (fout.exists() && skipAll) {
      long filesize = fin.length(); 
      totalBytesWritten += filesize;
      copiedFilesCount ++; 
      progress.setProgressValue(
          totalFilesSizes, totalBytesWritten, filesize, filesize, 
          totalFilesCount, copiedFilesCount); 
      return false;
    }

    InputStream in = null;
    OutputStream out = null;
    try {
      long filesize = fin.length(); 

      progress.setCopyingFile(fin.getAbsolutePath()); 
      progress.setCopyingTo(fout.getParent()); 
      progress.setProgressValue(
          totalFilesSizes, totalBytesWritten, filesize, 0, 
          totalFilesCount, copiedFilesCount); 

      if (isMoveOperation()) {
        if (fin.getFsName().equals(fout.getFsName()) && 
            fin.getFsSchemeName().equals(fout.getFsSchemeName()) && 
            fin.getRootDriveFile().equals(fout.getRootDriveFile())) {
          if (fin.renameTo(fout)) {
            copiedFilesCount ++; 
            totalBytesWritten += filesize;
            progress.setProgressValue(
                totalFilesSizes, totalBytesWritten, filesize, filesize, 
                totalFilesCount, copiedFilesCount); 
            return true; 
          }
        }
      }

      in = fin.getInputStream();
      out = fout.getOutputStream();

      byte[] data = new byte[102400];
      int read = 0;
      long bytesWrote = 0;
      @SuppressWarnings("unused")
	  long f_length = filesize;

      ///todo Maybe async IO would be nice here
      while ((read = in.read(data)) >= 0) {
        if (cancel) 
          throw new ActionCancelledException();

        if (read == 0) continue; 

        out.write(data, 0, read);
        bytesWrote += read;
        totalBytesWritten += read;

        progress.setProgressValue(
            totalFilesSizes, totalBytesWritten, filesize, bytesWrote, 
            totalFilesCount, copiedFilesCount); 
      }

      copiedFilesCount ++; 
      progress.setProgressValue(
          totalFilesSizes, totalBytesWritten, filesize, filesize, 
          totalFilesCount, copiedFilesCount); 

      if (isMoveOperation()) {
        try {
          if (in != null) in.close(); 
          in = null; 
        } catch (Exception ignored) {}
        fin.delete(); 
      }

      result = true; 

    } catch(ActionCancelledException ex) {
      try {
        if (out != null) out.close(); 
        out = null; 
      } catch (Exception ignored) {}
      fout.delete();
      result = false; 
      throw ex;

    } catch(Exception ex) {
      ex.printStackTrace();
      try {
        if (out != null) out.close(); 
        out = null; 
      } catch (Exception ignored) {}
      fout.delete();
      result = false; 
      JOptionPane.showMessageDialog(progress, 
          Strings.format("Error while writing %1$s: %2$s", fout.getPath(), ex.toString()), Strings.get("Error"), 
          JOptionPane.ERROR_MESSAGE);

    } finally {
      try { 
        if (in != null) in.close(); 
      } catch (Exception ignored) {}
      try { 
        if (out != null) out.close(); 
      } catch (Exception ignored) {}
    }

    return result; 
  }

  private void copyFiles(JFMFile[] filesToBeCopied, JFMFile destinationDir) 
      throws IOException, ActionCancelledException {
    if (filesToBeCopied.length == 0) progress.dispose();

    progress.setScaningStart();
    totalFilesSizes = scanFilesSize(filesToBeCopied);
    progress.setScaningDone(); 

    for (int i=0; i < filesToBeCopied.length; i++) {
      JFMFile el = (JFMFile)filesToBeCopied[i];      
      if (el.isDirectory()) {
        JFMFile destFile = destinationDir.mkdir(el.getName(), el.getAbsolutePath());
        boolean result = copyDir(el, destFile);
        if (isMoveOperation() && result) 
          el.delete(); // all files moved
      } else {
        JFMFile destFile = destinationDir.createFile(el.getName(), el.getAbsolutePath());
        copyFile(el, destFile);
      }
    }
  }

}
