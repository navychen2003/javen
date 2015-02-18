package com.forizon.jimage;

import java.util.EventListener;

/**
 * Listens for JImage drop events
 */
public interface URIDropListener extends EventListener {
    /**
     * This method is called when a URI is dropped a component which is being
     * listened to
     * @param dropTargetDropEvent the drop target event information
     */
    public void drop(URIDropEvent dropTargetDropEvent);
}

