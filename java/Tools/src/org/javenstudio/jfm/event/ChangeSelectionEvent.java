package org.javenstudio.jfm.event;

import org.javenstudio.jfm.filesystems.JFMFile;

public class ChangeSelectionEvent extends BroadcastEvent {

  public int getType() {
    return BroadcastEvent.CHANGE_SELECTION_TYPE;
  }
	
  public ChangeSelectionEvent() {
    this(null);
  }

  public ChangeSelectionEvent(JFMFile dir) {
    setFile(dir);
  }

  private JFMFile file = null;
  private int firstIndex = 0; 
  private int lastIndex = 0; 

  public JFMFile getFile() {
    return file;
  }

  public void setFile(JFMFile file) {
    this.file = file;
  }

  public void setFirstIndex(int first) { 
    this.firstIndex = first; 
  } 

  public int getFirstIndex() { 
    return firstIndex; 
  } 

  public void setLastIndex(int last) { 
    this.lastIndex = last; 
  } 

  public int getLastIndex() { 
    return lastIndex; 
  } 

}
