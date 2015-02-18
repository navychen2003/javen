package org.javenstudio.jfm.event;


public interface ChangeViewListener extends BroadcastListener {

  public void viewChanged(ChangeViewEvent ev);

}
