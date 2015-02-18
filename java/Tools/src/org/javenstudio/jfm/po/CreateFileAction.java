package org.javenstudio.jfm.po;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.event.ChangeDirectoryEvent;
import org.javenstudio.jfm.event.Broadcaster;
import org.javenstudio.jfm.filesystems.JFMFile;


public class CreateFileAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public CreateFileAction() {
  }

  public String getName() {
     return Strings.get("Create ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_N);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl N"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-new.png")));
  }

  public void actionPerformed(ActionEvent e) {
    JFMFile dir = Options.getActivePanel().getCurrentWorkingDirectory();
    if (dir == null) return;

    String fsName = Options.getActivePanel().getFsName();

    InputDialog d = new InputDialog(Options.getMainFrame(), Strings.get("New file"), true); 
    d.setMessage(Strings.format("Enter the name of the new file at %1$s:", fsName), 
    		Strings.get("Parent directory:"), Strings.get("File name:")); 
    d.setInputPath(dir.getPath());
    d.setVisible(true);
    if(d.isCancelled()) return;

    String newFile = d.getInputText(); 
    if (newFile == null) return;

    try {
      dir.createNewFile(newFile, newFile);
    } catch (Exception ex) {
      Options.showMessage(ex); 
    }

    ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
    ev.setDirectory(dir);
    ev.setSource(this);
    Broadcaster.notifyChangeDirectoryListeners(ev);
  }

}
