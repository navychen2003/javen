package com.forizon.jimage.viewer;

import com.forizon.jimage.viewer.context.ContextBuilder;
import com.forizon.jimage.viewer.imagelist.wrapspi.WrapException;
import javax.swing.SwingUtilities;
import java.net.URI; 

/** Runnable class which builds JImageView sessions */
public class JImageView implements Runnable {
    final public static String APPLICATION_NAME = "ApplicationName";
    final public static String APPLICATION_HOMEPAGE = "ApplicationHomepage";
    final public static String APPLICATION_SUPPORT = "ApplicationSupport";
    final public static String APPLICATION_VERSION = "ApplicationVersion";
    final public static String APPLICATION_VERSIONTAG = "ApplicationTag";
    final public static String APPLICATION_WORKDIRECTORY = "ApplicationWorkDirectory";
    final public static String LOCALIZATION_FILE
            = "com/forizon/jimage/viewer/Bundle";
    /**
     * Indicates whether the image should be zoomed so that as much of the
     * visible canvas is used without needing any scroll bars.
     */
    final public static String CONFIGURATION_ZOOMTOFIT      = "view.zoomToFit";
    /**
     * Indicates whether the image iterator should loop when the iterator's
     * position is either at the begining or at the end.
     */
    final public static String CONFIGURATION_LOOP           = "explore.loop";
    /** The delay between slides in slideshow mode */
    final public static String CONFIGURATION_SLIDESHOWDELAY = "slideshow.delay";
    /** The width of the window */
    final public static String CONFIGURATION_WIDTH          = "frame.width";
    /** The height of the window */
    final public static String CONFIGURATION_HEIGHT         = "frame.height";
    /** The last opened file */
    final public static String CONFIGURATION_LASTIMAGE      = "view.lastImage";

    /** Context builder */
    protected ContextBuilder builder;

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new JImageView(args));
    }

    private static JImageView instance = null;
    private static Object lockObj = new Object();

    public static JImageView get() {
        synchronized (lockObj) {
            if (instance == null)
                instance = new JImageView();
            return instance;
        }
    }

    public static void view(URI uri) {
        JImageView viewer = JImageView.get(); 
        if (uri != null) viewer.setImage(uri); 
        viewer.run(); 
    }

    public void setImage(URI uri) {
        try {
            builder.build().getImageContext().setImage(uri);
        } catch (WrapException ex) {
            builder.build().getReporter().report(ex);
        }
    }

    /**
     * Equivalent to:
     * <code>new JImageView(new String[]{})</code>
     * @see #JImageView(String[])
     */
    public JImageView() {
        this(new String[]{});
    }

    /**
     * @param args passed to the Context builder
     * @see com.forizon.jimage.viewer.JImageView#createBuilder
     */
    public JImageView(String[] args) {
        builder = createBuilder(args);
    }

    /** Starts a session of JImageView */
    @Override
    public void run() {
        builder.build().getWindow().setVisible(true);
        builder.build().getWindow().setLocationRelativeTo(null);
        builder.build().getWindow().toFront();
    }

    /**
     * Returns the context builder used to set up the session
     * @param args currently only used to pass command line arguments
     * @return the context builder used to set up the session
     */
    protected ContextBuilder createBuilder(String[] args) {
        return new ContextBuilder(args);
    }
}
