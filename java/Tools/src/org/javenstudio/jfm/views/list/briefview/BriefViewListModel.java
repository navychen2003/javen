package org.javenstudio.jfm.views.list.briefview;

import javax.swing.DefaultListModel;
import java.util.Vector; 
import java.util.Arrays;  

import org.javenstudio.jfm.main.Options; 
import org.javenstudio.jfm.event.Broadcaster;
import org.javenstudio.jfm.event.BrowseDirectoryEvent;
import org.javenstudio.jfm.filesystems.JFMFile;
import org.javenstudio.jfm.filesystems.JFMFileSystem; 
import org.javenstudio.jfm.views.list.JFMModel; 


@SuppressWarnings("rawtypes")
public class BriefViewListModel extends DefaultListModel implements JFMModel {
  private static final long serialVersionUID = 1L;
  
  private JFMFileSystem filesystem = null;
  private JFMFile workingDirectory = null;
  
  /**
   * The Constructor
   */
  public BriefViewListModel(JFMFileSystem filesystem) {
    super();
    setFilesystem(filesystem);    
  }

  @SuppressWarnings("unchecked")
  public void browseDirectory(JFMFile file){
    removeAllElements(); //removing the old rows

    //setting the working directory
    workingDirectory = file;      
    if (file.getParentFile() != null) {
      JFMFile parent = file.getParentFile();
      parent.setDisplayName("..");
      addElement(parent);        
    }

    BrowseDirectoryEvent dirEvent = new BrowseDirectoryEvent(file);
    dirEvent.setDirectory(file); 
    dirEvent.setSource(this);
    Broadcaster.notifyBrowseDirectoryListeners(dirEvent);

    try {
      JFMFile[] files = file.listFiles();
      if (files == null) return;
      Arrays.sort(files);      
      for (int i=0; i<files.length; i++) {
        addElement(files[i]);
      }      
    } catch (Exception ex) {
      Options.showMessage(ex); 
    }
  }
    
  public JFMFile getWorkingDirectory(){
    return workingDirectory;
  }
    
  /**
   * @return Returns the filesystem.
   */
  public JFMFileSystem getFilesystem() {
    return filesystem;
  }

  /**
   * @param filesystem The filesystem to set.
   */
  public void setFilesystem(JFMFileSystem filesystem) {
    this.filesystem = filesystem;
  }

  public JFMFile getFileAt(int row) { 
    if (row >= 0 && row < size()) 
      return (JFMFile)elementAt(row); 
    else
      return null; 
  }

  public int getRowCount() { return size(); }

  @SuppressWarnings("unchecked")
  public Vector getCurrentFiles() { 
    Vector files = new Vector();
    for (int i=0; i<size(); i++) {
      files.addElement(getFileAt(i));
    }
    return files;
  }

}
