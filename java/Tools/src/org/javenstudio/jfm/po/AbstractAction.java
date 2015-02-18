package org.javenstudio.jfm.po;

import org.javenstudio.jfm.main.Options; 


public abstract class AbstractAction extends javax.swing.AbstractAction {
  private static final long serialVersionUID = 1L;

  public AbstractAction() {
    setValues(); 
  }

  public void setValues() {
    putValue(NAME, Options.get(getClass().getName(), getName()));
  }

  public String getName() {
    return null; 
  }

}
