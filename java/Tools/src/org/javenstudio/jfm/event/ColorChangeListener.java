package org.javenstudio.jfm.event;


/**
 * The listener that get's notified when the  color changes. 
 * @author sergiu
 */
public interface ColorChangeListener extends BroadcastListener {

  public void colorChanged(ColorChangeEvent event); 

}
