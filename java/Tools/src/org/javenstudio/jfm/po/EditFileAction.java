package org.javenstudio.jfm.po;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.views.fview.FileViewDialog;
import org.javenstudio.jfm.filesystems.JFMFile;


public class EditFileAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public EditFileAction() {
  }

  public String getName() {
     return Strings.get("Edit ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_E);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl E"));
     //putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-edit.png")));
  }

  public void actionPerformed(ActionEvent e) {
    JFMFile viewFile = Options.getActivePanel().getSelectedFile();
    if (viewFile == null || !viewFile.isFile())
      return; 

    FileViewDialog d = new FileViewDialog(Options.getMainFrame(),viewFile.getPath(),false);
    d.setLocationRelativeTo(Options.getMainFrame());
    d.setContent(viewFile,true);
    d.setVisible(true);
  }

}
