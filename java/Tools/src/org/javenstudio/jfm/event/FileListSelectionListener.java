package org.javenstudio.jfm.event;


public interface FileListSelectionListener extends BroadcastListener {
  
  public void fileListSelectionChanged(FileListSelectionEvent ev);

}
