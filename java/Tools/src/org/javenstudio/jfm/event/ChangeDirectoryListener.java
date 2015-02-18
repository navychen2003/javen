package org.javenstudio.jfm.event;

public interface ChangeDirectoryListener extends BroadcastListener {
  
  public void changeDirectory(ChangeDirectoryEvent event);

}
