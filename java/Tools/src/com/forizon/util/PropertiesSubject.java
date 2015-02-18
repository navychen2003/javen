package com.forizon.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Properties;

/**
 * <code>{@link java.util.Properties}</code> backed by a
 * <code>{@link java.beans.PropertyChangeSupport}</code> to notify listeners of
 * property changes.
 */
public class PropertiesSubject extends Properties {
	private static final long serialVersionUID = 1L;
	
	PropertyChangeSupport propertyChangeSupport;

    public PropertiesSubject() {
        this(null);
    }

    public PropertiesSubject(Properties defaults) {
        super(defaults);
    }

    public Object setProperty (String key) {
        return setProperty(key, null);
    }

    @Override
    public Object setProperty (String key, String newValue) {
        String oldValue = getProperty(key);
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(key, oldValue, newValue);
        }
        return super.setProperty(key, newValue);
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }
  
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        return (propertyChangeSupport == null)
                ? new PropertyChangeListener[0]
                : propertyChangeSupport.getPropertyChangeListeners();
    }
  
    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return (propertyChangeSupport == null)
                ? new PropertyChangeListener[0]
                : propertyChangeSupport.getPropertyChangeListeners(propertyName);
    }
}

