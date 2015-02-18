package com.forizon.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * JImage's configuration manager. Extends
 * <code>{@link PropertiesSubject}</code> and adds methods to simplify
 * loading/saving to a file.
 */
public class PersistentProperties extends PropertiesSubject {
	private static final long serialVersionUID = 1L;
	
	/** File used to store the configuration values */
    protected File file;
    /**
     * Indicates whether a configuration value has been changed since the last
     * load/save operation
     */
    protected boolean dirty;

    /**
     * Equivalent to
     * <code>{@link #PersistentProperties(java.util.Properties) PersistentProperties(null)}</code>
     */
    public PersistentProperties() {
        this((Properties)null);
    }

    /**
     * Construct an object to store configuration values using a
     * <code>{@link java.util.Properties}</code> object for default values.
     */
    public PersistentProperties(Properties def) {
        super(def);
        dirty = false;
    }

    /**
     * 
     */
    public PersistentProperties(File file) throws IOException {
        this();
        setFile(file);
    }

    /**
     * Construct an object to store configuration values using a
     * <code>{@link java.util.Properties}</code> object for default values.
     */
    public PersistentProperties(Properties def, File file) throws IOException {
        this(def);
        setFile(file);
    }

    /**
     * Loads the given file
     * @param aFile file to load
     * @throws java.io.IOException if loading the file failed
     */
    public void setFile(File aFile) throws IOException {
        file = aFile;
        if (file.exists()) {
            FileInputStream in = new FileInputStream(file);
            try {
                loadFromXML(in);
            } finally {
                in.close();
            }
        }
        dirty = false;
    }

    /**
     * Returns whether a property was changed after the last save/load operation
     * @return whether a property was changed after the last save/load operation
     */
    public boolean isDirty () {
        return dirty;
    }

    /**
     * Sets a property, sets dirty flag to true and fires a property change
     * @param key property key
     * @param newValue value to set
     * @return the value which was previously set
     */
    @Override
    public Object setProperty (String key, String newValue) {
        String oldValue = getProperty(key);
        if (oldValue == null || !oldValue.equals(newValue)) {
            dirty = true;
        }
        return super.setProperty(key, newValue);
    }

    /**
     * Saves the properties to the last {@link #setFile(java.io.File) loaded}
     * file.
     * @throws java.io.IOException
     */
    public void save ()
        throws IOException
    {
        if (dirty) {
            file.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(file);
            try {
                storeToXML(out, "");
            } finally {
                out.close();
            }
        }
        dirty = false;
    }
}

