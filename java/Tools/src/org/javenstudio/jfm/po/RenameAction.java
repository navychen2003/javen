package org.javenstudio.jfm.po;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.event.ChangeDirectoryEvent;
import org.javenstudio.jfm.event.Broadcaster;
import org.javenstudio.jfm.filesystems.JFMFile;


public class RenameAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public RenameAction() {
  }

  public String getName() {
     return Strings.get("Rename ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_F);
     //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl F"));
     //putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/file-rename.png")));
  }

  public void actionPerformed(ActionEvent e) {
    JFMFile file = Options.getActivePanel().getSelectedFile();
    if (file == null) return;

    String fsName = Options.getActivePanel().getFsName();
    String fileType = file.isDirectory() ? Strings.get("dir") : Strings.get("file"); 

    InputDialog d = new InputDialog(Options.getMainFrame(), Strings.get("New name"), true);
    d.setMessage(Strings.format("Enter the new name of the %1$s at %2$s:", fileType, fsName), 
    		Strings.get("Full path name:"), Strings.format("New %1$s name:", fileType));
    d.setInputPath(file.getPath());
    d.setInputText(file.getName()); 
    d.setVisible(true);
    if(d.isCancelled()) return;

    String newName = d.getInputText(); 
    if (newName == null || newName.length() == 0) return;
    if (newName.equals(file.getName())) return; 

    try {
      file.rename(newName);
    } catch (Exception ex) {
      Options.showMessage(ex); 
    }

    ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
    ev.setDirectory(Options.getActivePanel().getCurrentWorkingDirectory());
    ev.setSource(this);
    Broadcaster.notifyChangeDirectoryListeners(ev);
  }

}
