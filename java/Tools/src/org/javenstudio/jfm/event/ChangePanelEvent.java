package org.javenstudio.jfm.event;


public class ChangePanelEvent extends BroadcastEvent {

  public ChangePanelEvent() {
  }
  
  public int getType() {
    return BroadcastEvent.CHANGE_PANEL_TYPE;
  }

  private int location = 0;

  public int getLocation() {
    return location;
  }

  public void setLocation(int location) {
    this.location = location;
  }

}
