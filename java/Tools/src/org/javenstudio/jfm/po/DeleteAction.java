package org.javenstudio.jfm.po;

import java.io.IOException; 
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.event.ChangeDirectoryEvent;
import org.javenstudio.jfm.event.Broadcaster;
import org.javenstudio.jfm.filesystems.JFMFile;


public class DeleteAction extends AbstractAction {
  private static final long serialVersionUID = 1L;
  
  private ProgressActionDialog progress = null;
  private long totalFilesSize = 0;
  private long totalDeletedSize = 0;
  private int totalFilesCount = 0;
  private int deletedFilesCount = 0;
  private boolean canceled = false; 

  public DeleteAction() {
  }

  public String getName() {
     return Strings.get("Delete"); 
  }

  public void setValues() {
     super.setValues(); 
     putValue(MNEMONIC_KEY, KeyEvent.VK_DELETE);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("DELETE"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/edit-delete.png")));
  }

  public void actionPerformed(ActionEvent e) {
    final JFMFile[] files = Options.getActivePanel().getSelectedFiles();
    if (files == null || files.length <= 0)
      return;

    final String fsName = Options.getActivePanel().getFsName(); 
    final JFMFile dirDst = Options.getActivePanel().getCurrentWorkingDirectory();

    StringBuffer sbuf = new StringBuffer(); 
    for (int i=0; i<files.length; i++) {
      JFMFile file = files[i]; 
      if (sbuf.length() > 0) sbuf.append("\n"); 
      sbuf.append(file.getPath()); 
    }
    String fileNames = sbuf.toString(); 
    int result = JOptionPane.showConfirmDialog(Options.getMainFrame(),
        Strings.format("Do you want to delete these %1$s files and directories at %2$s?", ""+files.length, fsName) 
        	+ "\n\n" + fileNames, 
        Strings.get("Delete"), JOptionPane.YES_NO_OPTION);
    if (result != JOptionPane.YES_OPTION) return;

    String title = Strings.format("Delete files at %1$s", fsName); 
    
    progress = new ProgressActionDialog(Options.getMainFrame(),title,true);
    progress.setLocationRelativeTo(Options.getMainFrame());
    progress.setOperation(ProgressActionDialog.DELETE);

    totalFilesSize = 0;
    totalDeletedSize = 0;
    totalFilesCount = 0;
    deletedFilesCount = 0;
    canceled = false; 

    progress.startAction(new ActionExecuter() {
      public void start() {
        try {
          deleteFiles(files);
        } catch (ActionCancelledException ex) {
          this.cancel();
        } catch (IOException e) {
          this.cancel();
          Options.showMessage(e);
        }

        ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
        ev.setSource(DeleteAction.this);
        ev.setDirectory(dirDst);
        Broadcaster.notifyChangeDirectoryListeners(ev);
      }

      public void cancel() {
        canceled = true;
      }
    });
  }

  private void deleteFiles(JFMFile[] files) throws IOException, ActionCancelledException {
    for (int i=0; files != null && i<files.length; i++) {
      deleteFile(files[i]);
    }
  }

  private void deleteFile(JFMFile fi) throws IOException, ActionCancelledException {
    if (canceled) throw new ActionCancelledException(); 

    if (fi.isDirectory()) {
      JFMFile[] list = fi.listFiles();
      boolean hasSub = false; 
      for (int i=0; list != null && i<list.length; i++) {
        if (list[i] == null) continue; 
        deleteFile(list[i]);
        hasSub = true; 
      }
      doDeleteFile(fi, 0, hasSub ? 0 : 1);
    } else {
      doDeleteFile(fi, fi.length(), 1);
    }
  }

  private void doDeleteFile(JFMFile file, long filesize, int count) throws IOException, ActionCancelledException {
    if (canceled) throw new ActionCancelledException(); 

    progress.setCopyingFile(file.getAbsolutePath());
    progress.setCopyingTo(file.getParent());

    progress.setProgressValue(
        totalFilesSize, totalDeletedSize, filesize, 0,
        totalFilesCount, deletedFilesCount);

    file.delete(); 

    totalFilesSize += filesize; 
    totalDeletedSize += filesize; 
    totalFilesCount += count; 
    deletedFilesCount += count; 

    progress.setProgressValue(
        totalFilesSize, totalDeletedSize, filesize, filesize,
        totalFilesCount, deletedFilesCount);
  }

}
