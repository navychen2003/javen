package org.javenstudio.jfm.po;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.filesystems.JFMFile;


public class FilePropertiesAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public FilePropertiesAction() {
  }

  public String getName() {
     return Strings.get("Properties");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_P);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl P"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-properties.png")));
  }

  public void actionPerformed(ActionEvent e) {
    JFMFile file = Options.getActivePanel().getSelectedFile();
    FilePropertiesDialog.showDialog(file); 
  }

}
