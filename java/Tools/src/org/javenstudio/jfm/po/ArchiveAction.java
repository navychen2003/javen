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


public class ArchiveAction extends AbstractAction {
  private static final long serialVersionUID = 1L;
  
  private ProgressActionDialog progress = null;
  public long totalFilesSize = 0;
  public long totalFinishedSize = 0;
  public long totalArchivedSize = 0;
  public int totalSelectedFiles = 0;
  public int totalArchivedFiles = 0;
  private boolean overwriteAll = false;
  private boolean skipAll = false;
  private boolean cancel = false;

  public ArchiveAction() {
  }

  public String getName() {
     return Strings.get("Archive to ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_A);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl A"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/zip.png")));
  }

  public boolean isCanceled() {
    return cancel;
  }

  private void scanFilesSize(JFMFile[] files) throws IOException {
    for (int i=0; files != null && i<files.length; i++) {
      JFMFile f = (JFMFile)files[i];
      progress.setScaningFile(f.getPath());
      if (f.isDirectory()) {
        JFMFile[] subfiles = f.listFiles(); 
        if (subfiles != null && subfiles.length > 0) 
          scanFilesSize(subfiles);
        else
          totalSelectedFiles ++;
      } else {
        totalFilesSize += f.length();
        totalSelectedFiles ++;
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    final JFMFile files[] = Options.getActivePanel().getSelectedFiles();
    if (files == null || files.length == 0) return;

    @SuppressWarnings("unused")
	final JFMFile dirSrc = Options.getActivePanel().getCurrentWorkingDirectory();
    final JFMFile dirDst = Options.getInactivePanel().getCurrentWorkingDirectory();

    final String fsNameSrc = Options.getActivePanel().getFsName();
    final String fsNameDst = Options.getInactivePanel().getFsName();

    String title = Strings.format("Archive from %1$s to %2$s", fsNameSrc, fsNameDst);

    ArchiveConfirmDialog d = new ArchiveConfirmDialog(Options.getMainFrame(),title,true);
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setArchiveFrom(files);
    d.setArchiveTo(dirDst);
    d.setVisible(true);
    if(d.isCancelled()) return;

    String archiveName = d.getArchiveName(); 
    String archiveType = d.getArchiveType(); 
    if (archiveName == null || archiveName.length() == 0) {
      Options.showMessage(Strings.get("You should input archive name.")); 
      return; 
    }
    if (archiveType == null || archiveType.length() == 0) 
      archiveType = ".zip"; 
    if (!archiveName.endsWith(archiveType))
      archiveName += archiveType; 

    totalFilesSize = 0;
    totalFinishedSize = 0;
    totalArchivedSize = 0;
    totalSelectedFiles = 0;
    totalArchivedFiles = 0;
    overwriteAll = false;
    skipAll = false;
    cancel = false;

    final String archiveFilename = archiveName; 

    progress = new ProgressActionDialog(Options.getMainFrame(),title,true);
    progress.setLocationRelativeTo(Options.getMainFrame());
    progress.setOperation(ProgressActionDialog.ARCHIVE);

    progress.startAction(new ActionExecuter() {
      public void start() {
        try {
          archiveFiles(files, dirDst, archiveFilename);
        } catch (ActionCancelledException ex) {
          this.cancel();
        } catch (IOException e) {
          this.cancel();
          Options.showMessage(e);
        }

        ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
        ev.setSource(ArchiveAction.this);
        ev.setDirectory(dirDst);
        Broadcaster.notifyChangeDirectoryListeners(ev);
      }

      public void cancel() {
        cancel = true;
      }
    });
  }

  public boolean checkOverwriteFile(JFMFile fout) throws IOException {
    boolean result = false;
    try {
      result = checkOverwriteFile0(fout);
    } catch (ActionCancelledException ex) {
      cancel = true;
    }
    return result;
  }

  private boolean checkOverwriteFile0(JFMFile fout) throws IOException, ActionCancelledException {
    if (fout.exists() && !overwriteAll && !skipAll) {
      progress.setBreakStart();

      String message = Strings.format("Target file %1$s already exists.\n\nTarget last modified date: %2$s (%3$s bytes)\n\nWhat should I do?", 
          fout.getPath(), Options.formatTime(fout.lastModified()), ""+fout.length());

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
          throw new ActionCancelledException();
        case 3:
          skipAll = true;
          break;
        case 4:
          throw new ActionCancelledException();
      }
    }

    if (fout.exists() && skipAll) {
      throw new ActionCancelledException();
    }

    return true;
  }

  private void archiveFiles(JFMFile[] files, JFMFile dirDst, String archiveName) 
      throws IOException, ActionCancelledException {
    if (files == null || dirDst == null || archiveName == null) 
      return; 

    JFMFile archiveFile = dirDst.createFile(archiveName, archiveName); 

    Archiver arch = null;
    if (Options.isTarFile(archiveFile))
      arch = new ArchiveTar();
    else if (Options.isZipFile(archiveFile))
      arch = new ArchiveZip();

    if (arch == null) {
      Options.showMessage(Strings.format("Cannot create this type of archive: %1$s",archiveName)); 
      return; 
    }

    progress.setScaningStart();
    scanFilesSize(files); 
    progress.setScaningDone(); 

    arch.setAction(this);
    arch.setProgress(progress);
    arch.archive(files, archiveFile);
  }

}
