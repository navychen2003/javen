package org.javenstudio.jfm.main.configurationdialog;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.javenstudio.jfm.event.BroadcastEvent;
import org.javenstudio.jfm.event.Broadcaster;
import org.javenstudio.jfm.event.ColorChangeEvent;
import org.javenstudio.jfm.event.FontChangeEvent;
import org.javenstudio.jfm.event.HelpURLChangeEvent;
import org.javenstudio.jfm.main.Options;


/**
 * This class holds the list of configuration related events, and fires when requested
 * @author sergiu
 */
public class ConfigurationEventsQueue {

  @SuppressWarnings("rawtypes")
  private static Set events = new HashSet();
  private static ConfigurationDialog dialog;
  
  
  /**
   * Adds an event to the list, that will be pending for notification
   * @param ev The event 
   */
  @SuppressWarnings("unchecked")
  public static void addPendingEvent(BroadcastEvent ev){
    events.add(ev);
    if(dialog != null) {
      dialog.configurationChanged();
    }
  }
  
  /**
   * Notifies all pending events, afer that clearing the list. 
   */
  public static void notifyPendingEvents(){
    @SuppressWarnings("rawtypes")
	Iterator it=events.iterator();
    while(it.hasNext()){
      BroadcastEvent ev=(BroadcastEvent)it.next();
      switch(ev.getType()){
        case BroadcastEvent.CHANGE_FONT_TYPE:
          Options.setPanelsFont(((FontChangeEvent)ev).getFont());
          Broadcaster.notifyFontChangeListeners((FontChangeEvent)ev);
          break;
        case BroadcastEvent.CHANGE_FGCOLOR_TYPE:
          Options.setForegroundColor(((ColorChangeEvent)ev).getColor());          
          Broadcaster.notifyColorChangeListeners((ColorChangeEvent)ev);
          break;
        case BroadcastEvent.CHANGE_BGCOLOR_TYPE:
          Options.setBackgroundColor(((ColorChangeEvent)ev).getColor());
          Broadcaster.notifyColorChangeListeners((ColorChangeEvent)ev);
          break;
        case BroadcastEvent.CHANGE_MKCOLOR_TYPE:
          Options.setMarkedColor(((ColorChangeEvent)ev).getColor());
          Broadcaster.notifyColorChangeListeners((ColorChangeEvent)ev);
          break;
        case BroadcastEvent.CHANGE_MKBGCOLOR_TYPE:
          Options.setMarkedBackground(((ColorChangeEvent)ev).getColor());
          Broadcaster.notifyColorChangeListeners((ColorChangeEvent)ev);
          break;
        case BroadcastEvent.CHANGE_HELP_URL:
          Options.getPreferences().put(Options.JFM_HELP_URL, ((HelpURLChangeEvent)ev).getUrl());
          break;
      }
    }    
    events.clear();
  }
  
  /**
   * Clears the events queue
   */
  public static void clearPendingEvents(){
    events.clear();
  }
  
  /**
   * Sets the dialog that manages the configuration
   * @param d the dialog
   */
  public static void setConfigurationDialog(ConfigurationDialog d){
    dialog=d;
  }

}
