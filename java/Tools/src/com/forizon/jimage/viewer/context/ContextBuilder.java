package com.forizon.jimage.viewer.context;

import com.forizon.util.PersistentProperties;
import com.forizon.util.PropertiesSubject;
import com.forizon.jimage.JImage;
import com.forizon.jimage.viewer.actions.ActionsBuilder;
import com.forizon.jimage.viewer.*;
import com.forizon.jimage.viewer.actions.AbstractAction;
import com.forizon.jimage.JImagePane;
import com.forizon.jimage.viewer.util.DefaultLanguageSupport;
import com.forizon.jimage.viewer.util.LanguageSupport;
import java.awt.Rectangle;
import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import java.util.logging.Logger;
import javax.swing.UIManager;

import org.javenstudio.jfm.main.Main;

/**
 * Builds a context object
 */
public class ContextBuilder {
    private static ContextBuilder instance = null; 
    private static Object lockObj = new Object(); 

    public static ContextBuilder get() {
        return get(new String[]{}); 
    }

    public static ContextBuilder get(String[] args) {
        synchronized (lockObj) {
            if (instance == null) 
                instance = new ContextBuilder(args); 
            return instance; 
        }
    }

    final protected String[] args;
    /** stores all contextual information of the application */
    final protected Context context;
    private boolean initialized = false; 

    public ContextBuilder(String[] args) {
        this.args = args;
        this.context = createContext();
    }

    final public synchronized Context build() {
        if (initialized == false) {
            initializeLAF();
            initialize();
            prepare();
            initialized = true; 
        }
        return context;
    }

    protected void initialize() {
        context.reporter = createReporter();
        context.properties = createProperties();
        context.configuration = createConfiguration();
        context.languageSupport = createLanguageSupport();
        context.imageContext = createImageContext();
        context.jImagePane = createJImagePane();
        context.actions = createActions();
        context.window = createWindow();
    }

    protected void prepare() {
        prepareReporter();
        prepareProperties();
        prepareConfiguration();
        prepareLanguageSupport();
        prepareImageContext();
        prepareJImagePane();
        prepareActions();
        prepareWindow();
    }

    /**
     * Initialize the application's actions
     */
    public void prepareReporter() {}

    /**
     * Prepare the session's
     * <code>{@link com.forizon.jimage.viewer.context.ImageContext}</code>
     */
    public void prepareImageContext() {}

    /**
     * Initialize the application's actions
     */
    public void prepareActions() {}

    /**
     * Initialize the application's
     * <code>{@link com.forizon.util.PersistentProperties}</code> which
     * handles user preferences.
     */
    public void prepareConfiguration() {
        try {
            File file = new File(
             context.getProperties().getProperty(JImageView.APPLICATION_WORKDIRECTORY)
             + "configuration.properties");
            context.configuration.setFile(file);
            if (args.length > 0) {
                File argFile;
                for (int i = 0; i < args.length; i++) {
                    try {
                        argFile = new File(args[0].trim());
                        context.configuration.setProperty(JImageView.CONFIGURATION_LASTIMAGE, argFile.toURI().toString());
                    } catch (Exception e) {
                        context.reporter.report(e);
                    }
                }
            }
        } catch (Exception e) {
            context.reporter.report(e);
        }
    }

    /**
     * Initialize the application's
     * <code>{@link com.forizon.util.PersistentProperties}</code> which
     * handles user preferences.
     */
    public void prepareProperties() {
        Properties property = context.getProperties();
        property.setProperty(JImageView.APPLICATION_NAME, "JImageView");
        property.setProperty(JImageView.APPLICATION_HOMEPAGE, Main.WEBSITE);
        property.setProperty(JImageView.APPLICATION_SUPPORT, Main.HELPURL);
        property.setProperty(JImageView.APPLICATION_VERSION, Main.VERSION);
        property.setProperty(JImageView.APPLICATION_WORKDIRECTORY,
                System.getProperty("user.home")
                   + File.separator + ".javenstudio" + File.separator
                   + property.getProperty(JImageView.APPLICATION_NAME)
                   + File.separator + "1.x.x" + File.separator);
    }

    /**
     * Initialize JImage's GUI
     * @throws java.awt.HeadlessException
     */
    public void prepareWindow() {
        context.window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        context.window.setIconImage(new ImageIcon(getClass().getResource("/images/icons/jimage.png")).getImage());
        // Load and Set bounds
        try {
            Rectangle bounds = context.window.getBounds();
            bounds.width = Integer.parseInt(
                    context.getConfiguration().getProperty(
                        JImageView.CONFIGURATION_WIDTH));
            bounds.height = Integer.parseInt(
                    context.getConfiguration().getProperty(
                        JImageView.CONFIGURATION_HEIGHT));
            context.window.setBounds(bounds);
        } catch (NumberFormatException e) {
            context.reporter.report(e);
        }
        context.window.setJMenuBar(new MenuBarBuilder(context).build());
        context.window.setContentPane(context.jImagePane);
        context.window.addWindowListener(new WindowListener(context));
    }

    /**
     * Initialize the <code>{@link java.util.ResourceBundle}</code> which stores
     * the user's localized text
     */
    public void prepareLanguageSupport() {}

    /**
     * Initialize the
     * <code>{@link com.forizon.jimage.JImagePane}</code> which
     * displays any loaded image
     */
    public void prepareJImagePane() {
        JImage jImage = context.jImagePane.getJImage();
        jImage.addURIDropListener(new URIDropListener(context));
        jImage.addMouseListener(new FullScreenMouseListener(context));

        context.imageContext.imageIterator.addIteratorListener(
                new JImagePaneListener(context));
    }

    protected Reporter createReporter() {
        Logger logger = Logger.getLogger(toString(), JImageView.LOCALIZATION_FILE);
        return new DefaultReporter(context, logger);
    }

    protected JImagePane createJImagePane() {
        return new JImagePane();
    }

    protected JFrame createWindow() {
        return new JFrame(context.languageSupport.localizeString("com.forizon.jimage.viewer.view.text"));
    }

    protected Map<String, AbstractAction> createActions() {
        return new ActionsBuilder(context).getActions();
    }

    protected ImageContext createImageContext() {
        ImageContextBuilder builder = new ImageContextBuilder(context);
        return builder.build();
    }

    protected Context createContext() {
        return new Context();
    }

    protected PersistentProperties createConfiguration() {
        Properties defaults = new Properties();
        defaults.setProperty(JImageView.CONFIGURATION_ZOOMTOFIT, "false");
        defaults.setProperty(JImageView.CONFIGURATION_LOOP, "false");
        defaults.setProperty(JImageView.CONFIGURATION_SLIDESHOWDELAY, "3");
        defaults.setProperty(JImageView.CONFIGURATION_WIDTH, "200");
        defaults.setProperty(JImageView.CONFIGURATION_HEIGHT, "200");
        return new PersistentProperties(defaults);
    }

    protected PropertiesSubject createProperties() {
        return new PropertiesSubject();
    }

    protected LanguageSupport createLanguageSupport() {
        return new DefaultLanguageSupport(context, ResourceBundle.getBundle(JImageView.LOCALIZATION_FILE));
    }

    protected void initializeLAF() {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            context.reporter.report(e);
            //Logger.getLogger(JImageView.class.getPackage().getName(), JImageView.LOCALIZATION_FILE).log(Level.WARNING, e.getLocalizedMessage(), e);
        }
    }
}

