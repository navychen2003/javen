package org.javenstudio.jfm.event;

import org.javenstudio.jfm.filesystems.JFMFile;

public class BrowseDirectoryEvent extends BroadcastEvent {

  public int getType() {
    return BroadcastEvent.BROWSE_DIR_TYPE;
  }
	
  public BrowseDirectoryEvent() {
    this(null);
  }

  public BrowseDirectoryEvent(JFMFile dir) {
    setDirectory(dir);
  }

  private JFMFile directory = null;

  public JFMFile getDirectory() {
    return directory;
  }

  public void setDirectory(JFMFile directory) {
    this.directory = directory;
  }
}
