package org.javenstudio.jfm.main.configurationdialog;

import javax.swing.tree.DefaultMutableTreeNode;

import org.javenstudio.jfm.main.configurationdialog.panels.ConfigurationPanel;


/**
 * A node in the configuration tree
 * @author sergiu
 */
public class ConfigurationTreeNode extends DefaultMutableTreeNode {
  private static final long serialVersionUID = 1L;

  public ConfigurationTreeNode(){
    super();    
  }

  public ConfigurationTreeNode(ConfigurationPanel userObject){
    super(userObject);
    setUserObject(userObject);    
  }
  
  public void setUserObject(Object userObject){
    if(userObject instanceof ConfigurationPanel){
      super.setUserObject(userObject);
    }else{
      throw new InternalError("The userobject on an tree node must be a ConfigurationPanel");
    }
  }
 
}
