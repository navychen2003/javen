package org.javenstudio.jfm.views.tree;

import javax.swing.tree.*;
import java.util.Arrays;

import org.javenstudio.jfm.main.Options;
import org.javenstudio.jfm.filesystems.*;


public class FTreeModel extends DefaultTreeModel {
  private static final long serialVersionUID = 1L;
  
  @SuppressWarnings("unused")
  private int location;
  private DefaultMutableTreeNode root;
  
  public FTreeModel(int location,DefaultMutableTreeNode root) {
    super(root);
    this.root=root;
    root.add(new DefaultMutableTreeNode("Loading..."));
    setLocation(location);
    initTree(Options.getStartDirectory());
  }
  
  public void setLocation(int l){
    location=l;
  }
  
  public void initTree(String dir){
    JFMFile f = null;//new FileElement(dir);
    JFMFile rootFile = getRootFile(f);
    root.setUserObject(rootFile);
    fillTreeFromNode(root);
  }
  
  public void fillTreeFromNode(DefaultMutableTreeNode n) {
    JFMFile el = (JFMFile)n.getUserObject();    
    if(!el.isDirectory()) return;        
    n.removeAllChildren();

    JFMFile[] childs = null;
    try {
      childs = el.listFiles(); 
    } catch (Exception ex) {
      childs = null; 
      Options.showMessage(ex); 
    }

    if (childs == null) return; 
    Arrays.sort(childs);
    
    for(int i=0; i<childs.length; i++) {
      DefaultMutableTreeNode a_node = new DefaultMutableTreeNode(childs[i]);
      if(childs[i].isDirectory()) a_node.add(new DefaultMutableTreeNode("Loading..."));
      n.add(a_node);
    }
    
    this.nodeStructureChanged(n);
  }

  private JFMFile getRootFile(JFMFile f) {
    return (f != null) ? f.getRootDriveFile() : null;
  }

}
