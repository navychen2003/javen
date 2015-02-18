package org.javenstudio.falcon.util;

import java.lang.System;
import java.lang.Thread;
import java.util.Map;

import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

/** A recursive timer.
 * 
 * RecursiveTimers are started automatically when instantiated; subtimers are also
 * started automatically when created.
 *
 * @since 1.3
 */
public class RecursiveTimer {

  public static final int STARTED = 0;
  public static final int STOPPED = 1;
  public static final int PAUSED = 2;

  protected int state;
  protected double startTime;
  protected double time;
  protected double culmTime;
  protected NamedMap<RecursiveTimer> children;

  public RecursiveTimer() {
    time = 0;
    culmTime = 0;
    children = new NamedMap<RecursiveTimer>();
    startTime = now();
    state = STARTED;
  }

  /** Get current time
   *
   * May override to implement a different timer (CPU time, etc).
   */
  protected double now() { return System.currentTimeMillis(); }

  /** Recursively stop timer and sub timers */
  public double stop() {
    assert state == STARTED || state == PAUSED;
    time = culmTime;
    if(state == STARTED) 
      time += now() - startTime;
    state = STOPPED;
    
    for( Map.Entry<String,RecursiveTimer> entry : children ) {
      RecursiveTimer child = entry.getValue();
      if(child.state == STARTED || child.state == PAUSED) 
        child.stop();
    }
    return time;
  }

  public void pause() {
    assert state == STARTED;
    culmTime += now() - startTime;
    state = PAUSED;
  }
  
  public void resume() {
    if(state == STARTED)
      return;
    assert state == PAUSED;
    state = STARTED;
    startTime = now();
  }

  /** Get total elapsed time for this timer.
   *
   * Timer must be STOPped.
   */
  public double getTime() {
    assert state == STOPPED;
    return time;
  }

  /** Create new subtimer with given name
   *
   * Subtimer will be started.
   */
  public RecursiveTimer sub(String desc) {
    RecursiveTimer child = children.get( desc );
    if( child == null ) {
      child = new RecursiveTimer();
      children.add(desc, child);
    }
    return child;
  }

  @Override
  public String toString() {
    return asNamedList().toString();
  }

  public NamedList<?> asNamedList() {
    NamedList<Object> m = new NamedMap<Object>();
    m.add( "time", time );
    if( children.size() > 0 ) {
      for( Map.Entry<String, RecursiveTimer> entry : children ) {
        m.add( entry.getKey(), entry.getValue().asNamedList() );
      }
    }
    return m;
  }
  
  /**
   * Manipulating this map may have undefined results.
   */
  public NamedMap<RecursiveTimer> getChildren()
  {
    return children;
  }

  /*************** Testing *******/
  public static void main(String []argv) throws InterruptedException {
    RecursiveTimer rt = new RecursiveTimer(), subt, st;
    Thread.sleep(100);

    subt = rt.sub("sub1");
    Thread.sleep(50);
    st = subt.sub("sub1.1");
    st.resume();
    Thread.sleep(10);
    st.pause();
    Thread.sleep(50);
    st.resume();
    Thread.sleep(10);
    st.pause();
    subt.stop();
    rt.stop();

    System.out.println( rt.toString());
  }
}
