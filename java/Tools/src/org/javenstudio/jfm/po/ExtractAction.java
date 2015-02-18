package org.javenstudio.jfm.po;

import java.io.IOException; 
import java.util.ArrayList; 
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.event.ChangeDirectoryEvent;
import org.javenstudio.jfm.event.Broadcaster;
import org.javenstudio.jfm.filesystems.JFMFile;


public class ExtractAction extends AbstractAction {
  private static final long serialVersionUID = 1L;
  
  private ProgressActionDialog progress = null;
  public long totalArchivesSize = 0; 
  public long totalFinishedSize = 0; 
  public long totalExtractedSize = 0; 
  public int totalArchivesFiles = 0; 
  public int totalExtractedFiles = 0; 
  private boolean overwriteAll = false;
  private boolean skipAll = false;
  private boolean cancel = false;

  public ExtractAction() {
  }

  public String getName() {
     return Strings.get("Extract to ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_T);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl T"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/zip.png")));
  }

  private void resetAll() {
    totalArchivesSize = 0; 
    totalFinishedSize = 0; 
    totalExtractedSize = 0; 
    totalArchivesFiles = 0; 
    totalExtractedFiles = 0; 
    overwriteAll = false;
    skipAll = false;
    cancel = false;
  }

  public boolean isCanceled() {
    return cancel; 
  }

  public void actionPerformed(ActionEvent e) {
    resetAll(); 

    final JFMFile files[] = getArchiveFiles(Options.getActivePanel().getSelectedFiles());
    if (files == null || files.length == 0) return;

    @SuppressWarnings("unused")
	final JFMFile dirSrc = Options.getActivePanel().getCurrentWorkingDirectory();
    final JFMFile dirDst = Options.getInactivePanel().getCurrentWorkingDirectory();

    String fsNameSrc = Options.getActivePanel().getFsName();
    String fsNameDst = Options.getInactivePanel().getFsName();

    String title = Strings.format("Extract from %1$s to %2$s", fsNameSrc, fsNameDst);

    CopyConfirmDialog d = new CopyConfirmDialog(Options.getMainFrame(),title,true);
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setOperation(ProgressActionDialog.EXTRACT);
    d.setCopyFrom(files);
    d.setCopyTo(dirDst);
    d.setVisible(true);
    if(d.isCancelled()) return;

    progress = new ProgressActionDialog(Options.getMainFrame(),title,true);
    progress.setLocationRelativeTo(Options.getMainFrame());
    progress.setOperation(ProgressActionDialog.EXTRACT);

    progress.startAction(new ActionExecuter() {
      public void start() {
        try {
          extractFiles(files, dirDst);
        } catch (ActionCancelledException ex) {
          this.cancel();
        } catch (IOException e) {
          this.cancel();
          Options.showMessage(e);
        }

        ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
        ev.setSource(ExtractAction.this);
        ev.setDirectory(dirDst);
        Broadcaster.notifyChangeDirectoryListeners(ev);
      }

      public void cancel() {
        cancel = true;
      }
    });
  }

  private void extractFiles(JFMFile[] files, JFMFile dirDst) throws IOException, ActionCancelledException {
    for (int i=0; files != null && i<files.length; i++) {
      JFMFile file = files[i];
      Extractor extr = null; 
      if (Options.isTarFile(file)) 
        extr = new ArchiveTar(); 
      else if (Options.isZipFile(file)) 
        extr = new ArchiveZip(); 
      else if (Options.isRarFile(file)) 
        extr = new ArchiveRar(); 
 
      if (extr == null) continue; 
      extr.setAction(this); 
      extr.setProgress(progress); 
      extr.extract(file, dirDst); 
    }
  }

  private JFMFile[] getArchiveFiles(JFMFile[] files) {
    if (files == null) return files; 
    ArrayList<JFMFile> afiles = new ArrayList<JFMFile>(); 
    for (int i=0; files != null && i < files.length; i++) {
      JFMFile file = files[i]; 
      if (file == null) continue; 
      if (Options.isArchiveFile(file)) {
        afiles.add(file); 
        totalArchivesSize += file.length(); 
        totalArchivesFiles ++; 
      }
    }
    return afiles.toArray(new JFMFile[afiles.size()]); 
  }

  public boolean checkOverwriteFile(ArchiveFile fin, JFMFile fout) throws IOException {
    boolean result = false; 
    try {
      result = checkOverwriteFile0(fin, fout); 
    } catch (ActionCancelledException ex) {
      cancel = true; 
    }
    return result; 
  }

  private boolean checkOverwriteFile0(ArchiveFile fin, JFMFile fout) throws IOException, ActionCancelledException {
    if (fout.exists() && !overwriteAll && !skipAll) {
      progress.setBreakStart();

      String message = Strings.format(
    	  "Target file %1$s already exists.\n\nSource last modified date: %2$s (%3$s bytes)\nTarget last modified date: %4$s (%5$s bytes)\n\nWhat should I do?", 
          fout.getPath(), Options.formatTime(fin.lastModified), ""+fin.length, 
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
          long filesize = fin.length;
          totalFinishedSize += filesize;
          totalExtractedSize += filesize;
          totalExtractedFiles ++;
          progress.setProgressValue(
              totalArchivesSize, totalFinishedSize, totalExtractedSize, filesize, filesize,
              totalArchivesFiles, totalExtractedFiles);
          return false;
        case 3:
          skipAll = true;
          break;
        case 4:
          throw new ActionCancelledException();
      }
    }

    if (fout.exists() && skipAll) {
      long filesize = fin.length;
      totalFinishedSize += filesize;
      totalExtractedSize += filesize;
      totalExtractedFiles ++;
      progress.setProgressValue(
          totalArchivesSize, totalFinishedSize, totalExtractedSize, filesize, filesize,
          totalArchivesFiles, totalExtractedFiles);
      return false;
    }

    return true; 
  }

}
