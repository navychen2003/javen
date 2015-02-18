package org.javenstudio.jfm.event;

import org.javenstudio.jfm.views.JFMViewRepresentation;


public class ChangeViewEvent extends BroadcastEvent {

  private JFMViewRepresentation viewRep;
  private String filesystemClassName = null;
  private String filesystemName = null;
  private boolean reconnect = false; 
	
  public ChangeViewEvent() {
  }

  public int getType() {
    return BroadcastEvent.CHANGE_VIEW_TYPE;
  }
  
  /**
   * @return Returns the viewRep.
   */
  public JFMViewRepresentation getViewRep() {
    return viewRep;
  }

  /**
   * @param viewRep The viewRep to set.
   */
  public void setViewRep(JFMViewRepresentation viewRep) {
    this.viewRep = viewRep;
  }

  /**
   * @return the filesystemClassName
   */
  public String getFilesystemClassName() {
    return filesystemClassName;
  }

  /**
   * @param filesystemClassName the filesystemClassName to set
   */
  public void setFilesystemClassName(String filesystemClassName) {
    this.filesystemClassName = filesystemClassName;
  }

  public void setFilesystemName(String name) {
    this.filesystemName = name; 
  }

  public String getFilesystemName() {
    return filesystemName; 
  }

  public void setReconnect(boolean b) {
    this.reconnect = b; 
  }

  public boolean isReconnect() {
    return reconnect; 
  }
}
