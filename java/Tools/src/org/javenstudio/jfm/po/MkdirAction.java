package org.javenstudio.jfm.po;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.event.ChangeDirectoryEvent;
import org.javenstudio.jfm.event.Broadcaster;
import org.javenstudio.jfm.filesystems.JFMFile;


public class MkdirAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public MkdirAction() {
  }

  public String getName() {
     return Strings.get("New folder");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_M);
     //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl M"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/folder-new.png")));
  }

  public void actionPerformed(ActionEvent e) {
    JFMFile dir = Options.getActivePanel().getCurrentWorkingDirectory();
    if (dir == null) return;

    String fsName = Options.getActivePanel().getFsName();

    InputDialog d = new InputDialog(Options.getMainFrame(), Strings.get("New directory"), true);
    d.setMessage(Strings.format("Enter the name of the new directory at %1$s:", fsName), 
    		Strings.get("Parent directory:"), Strings.get("Directory name:"));
    d.setInputPath(dir.getPath());
    d.setVisible(true);
    if(d.isCancelled()) return;

    String newName = d.getInputText(); 
    if (newName == null) return;

    try {
      JFMFile newDir = dir.mkdir(newName, newName);

      ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
      ev.setDirectory(newDir);
      ev.setSource(this);
      Broadcaster.notifyChangeDirectoryListeners(ev);

    } catch (Exception ex) {
      Options.showMessage(ex); 
    }
  }

}
