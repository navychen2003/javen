package org.javenstudio.jfm.event;


/**
 * This is the event that is propagated when an view object changes it's files selection. 
 * @author Giurgiu Sergiu
 * @version 1.0
 */
public class FileListSelectionEvent extends BroadcastEvent {

  public FileListSelectionEvent() {
  }
  
  private int[] newFilesListIndexes;
  private int panelLocation;

  public int getType() {
    return BroadcastEvent.FILE_LIST_SELECTION_TYPE;
  }
  
  public void setPanelLocation(int loc){
    panelLocation=loc;
  }
  
  public int getPanelLocation(){
    return panelLocation;
  }
  
  public void setFilesIndexes(int[] list){
    newFilesListIndexes=list;
  }
  
  public int[] getFilesIndexes(){
    return newFilesListIndexes;
  }

}
