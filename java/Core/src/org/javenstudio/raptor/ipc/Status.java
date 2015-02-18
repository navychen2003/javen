package org.javenstudio.raptor.ipc;

/**
 * Status of a IPC call.
 */
enum Status {
  SUCCESS (0),
  ERROR (1),
  FATAL (-1);
  
  int state;
  private Status(int state) {
    this.state = state;
  }
}

