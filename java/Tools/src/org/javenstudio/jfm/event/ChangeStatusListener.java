package org.javenstudio.jfm.event;

public interface ChangeStatusListener extends BroadcastListener {
  
  public void changeStatus(ChangeStatusEvent event);

}
