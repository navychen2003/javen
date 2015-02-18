package org.javenstudio.jfm.event;


public class ChangeStatusEvent extends BroadcastEvent {

  public int getType() {
    return BroadcastEvent.CHANGE_STATUS_TYPE;
  }
	
  public ChangeStatusEvent() {
  }

  private String status = null;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
