package org.javenstudio.jfm.po;

import org.javenstudio.jfm.main.Options;

import java.awt.event.ActionEvent;


public class QuitAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  public QuitAction() {
  }
 
  public void actionPerformed(ActionEvent e) {
    Options.savePreferences();
    System.exit(0);
  }

}
