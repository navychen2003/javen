package org.javenstudio.jfm.po;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import org.javenstudio.common.util.Strings;
import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.event.ChangeDirectoryEvent;
import org.javenstudio.jfm.event.Broadcaster;


public class RefreshAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public RefreshAction() {
  }

  public String getName() {
     return Strings.get("Refresh");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_R);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl R"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/view-refresh.png")));
  }

  public void actionPerformed(ActionEvent e) {

    ChangeDirectoryEvent ev=new ChangeDirectoryEvent();
    ev.setDirectory(Options.getActivePanel().getCurrentWorkingDirectory());
    ev.setSource(this);
    Broadcaster.notifyChangeDirectoryListeners(ev);
  }

}
