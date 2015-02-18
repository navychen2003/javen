package com.forizon.jimage;

import java.net.URI;
import java.util.EventObject;

/**
 * Accessor for JImage drop event information
 */
public class URIDropEvent extends EventObject {
	private static final long serialVersionUID = 1L;
	/**
     * URI of the object dropped into JImage
     */
    final protected URI droppedURI;

    /**
     * @param source the source of the event
     * @param aDroppedURI the URI dropped on the srouce
     */
    protected URIDropEvent (Object source, URI aDroppedURI) {
        super(source);
        droppedURI = aDroppedURI;
    }

    /**
     * Returns the dropped URI
     * @return the dropped URI
     */
    public URI getURI () {
        return droppedURI;
    }
}

