package org.javenstudio.raptor.bigdb.regionserver;

/**
 * Implementations are stoppable.
 */
interface Stoppable {
  // Starting small, just doing a stoppable/stop for now and keeping it package
  // protected for now.  Needed so don't have to pass RegionServer instance
  // everywhere.  Doing Lifecycle seemed a stretch since none of our servers
  // do natural start/stop, etc. RegionServer is hosted in a Thread (can't do
  // 'stop' on a Thread and 'start' has special meaning for Threads) and then
  // Master is implemented differently again (it is a Thread itself). We
  // should move to redoing Master and RegionServer servers to use Spring or
  // some such container but for now, I just need stop -- St.Ack.
  /**
   * Stop service.
   */
  public void stop();
}

