package org.javenstudio.jfm.event;

import org.javenstudio.jfm.filesystems.JFMFile;

public class ChangeDirectoryEvent extends BroadcastEvent {

  /* (non-Javadoc)
   * @see org.javenstudio.jfm.event.BroadcastEvent#getType()
   */
  public int getType() {
    return BroadcastEvent.CHANGE_DIR_TYPE;
  }
	
  public ChangeDirectoryEvent() {
    this(null);
  }

  public ChangeDirectoryEvent(JFMFile dir) {
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
