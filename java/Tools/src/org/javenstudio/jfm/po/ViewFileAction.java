package org.javenstudio.jfm.po;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.views.fview.FileViewDialog;
import com.forizon.jimage.viewer.JImageView; 


public class ViewFileAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public ViewFileAction() {
  }

  public String getName() {
     return Strings.get("View ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_O);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl O"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/document-open.png")));
  }

  public void actionPerformed(ActionEvent e) {
    JFMFile file = Options.getActivePanel().getSelectedFile();
    if (file == null || !file.isFile())
      return; 

    viewFile(file); 
  }

  public static void viewFile(JFMFile file) {
    if (Options.isImageFile(file)) {
      JImageView.view(file.toURI()); 
    } else {
      FileViewDialog d = new FileViewDialog(Options.getMainFrame(),file.getPath(),false);
      d.setLocationRelativeTo(Options.getMainFrame());
      d.setContent(file,false);
      d.setVisible(true);
    }
  }

}
