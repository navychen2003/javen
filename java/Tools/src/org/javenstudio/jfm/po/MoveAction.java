package org.javenstudio.jfm.po;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import org.javenstudio.common.util.Strings;


public class MoveAction extends CopyAction {
  private static final long serialVersionUID = 1L;

  public MoveAction() {
  }

  public boolean isMoveOperation() {
    return true;
  }

  public String getName() {
     return Strings.get("Move to ...");
  }

  public void setValues() {
     super.setValues();
     putValue(MNEMONIC_KEY, KeyEvent.VK_X);
     putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl X"));
     putValue(SMALL_ICON, new javax.swing.ImageIcon(getClass().getResource("/images/icons/edit-cut.png")));
  }

}
