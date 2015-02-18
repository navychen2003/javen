package org.javenstudio.jfm.main.configurationdialog;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;


/**
 * The confguration tree model
 * @author sergiu
 */
public class ConfigurationTreeModel extends DefaultTreeModel {
  private static final long serialVersionUID = 1L;
  
  @SuppressWarnings("unused")
  private DefaultMutableTreeNode root;
  
  public ConfigurationTreeModel(TreeNode root) {
    super(root);
    this.root=(DefaultMutableTreeNode)root;
  }

}
