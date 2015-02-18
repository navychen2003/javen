package com.forizon.jimage.viewer.context;

import com.forizon.jimage.JImagePane;
import com.forizon.util.PersistentProperties;
import com.forizon.util.PropertiesSubject;
import com.forizon.jimage.viewer.actions.AbstractAction;
import com.forizon.jimage.viewer.util.LanguageSupport;
import java.util.Map;
import javax.swing.JFrame;

/** Contexts maintain links to important parts of a session */
public class Context {
    /** Root window of the session */
    protected JFrame window;
    /** Primary JImagePane which displays the currently selected image */
    protected JImagePane jImagePane;
    /**
     * Map of String identifier to
     * {@link javax.swing.Action}s.
     */
    protected Map<String, AbstractAction> actions;
    /** User configuration */
    protected PersistentProperties configuration;
    /** Application properties */
    protected PropertiesSubject properties;
    /** Language support */
    protected LanguageSupport languageSupport;
    /** Image context */
    protected ImageContext imageContext;
    /** Issue logger */
    protected Reporter reporter;

    public JFrame getWindow() {
        return window;
    }

    public JImagePane getJImagePane() {
        return jImagePane;
    }

    public Map<String, AbstractAction> getActions() {
        return actions;
    }

    public PropertiesSubject getProperties() {
        return properties;
    }

    public PersistentProperties getConfiguration() {
        return configuration;
    }

    public ImageContext getImageContext() {
        return imageContext;
    }

    public LanguageSupport getLanguageSupport() {
        return languageSupport;
    }

    public Reporter getReporter() {
        return reporter;
    }
}
