package org.javenstudio.raptor.util;

import java.io.Reader; 
import java.io.BufferedReader; 
import java.text.MessageFormat;
import java.util.ArrayList; 
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer; 
import java.net.URLClassLoader;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration; 
import org.javenstudio.raptor.conf.ConfigurationFactory;


/**
 * An internationalization / localization helper class which reduces
 * the bother of handling ResourceBundles and takes care of the
 * common cases of message formating which otherwise require the
 * creation of Object arrays and such.
 *
 * <p>The StringManager operates on a package basis. One StringManager
 * per package can be created and accessed via the getManager method
 * call.
 *
 * <p>The StringManager will look for a ResourceBundle named by
 * the package name given plus the suffix of "LocalStrings". In
 * practice, this means that the localized information will be contained
 * in a LocalStrings.properties file located in the package
 * directory of the classpath.
 *
 * <p>Please see the documentation for java.util.ResourceBundle for
 * more information.
 *
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 */
public class StringManager {
    private static final Logger LOG = Logger.getLogger(StringManager.class);

    public static class StringManagers extends StringManager {
        private HashSet<String> names = new HashSet<String>(); 
        private ArrayList<StringManager> managers = new ArrayList<StringManager>(); 

        public StringManagers() {
            this(null); 
        }

        public StringManagers(Locale loc) {
            super(loc); 
        }

        public void add(String packageName) {
            add(getManager(getLocale(), packageName)); 
        }

        public synchronized void add(StringManager mgr) {
            if (mgr == null || mgr == this) 
                return; 
            if (mgr.getName() == null || names.contains(mgr.getName()))
                return; 
            managers.add(mgr); 
            names.add(mgr.getName()); 
        }

        protected synchronized String getStringInternal(String key) {
            for (int i=0; i < managers.size(); i++) {
                StringManager mgr = managers.get(i); 
                String value = mgr != null ? mgr.getStringInternal(key, false) : null; 
                if (value != null) 
                    return value; 
            }
            return super.getStringInternal(key); 
        }

        public synchronized String getMessage(String key) {
            for (int i=0; i < managers.size(); i++) {
                StringManager mgr = managers.get(i); 
                String value = mgr != null ? mgr.getMessage(key) : null; 
                if (value != null) 
                    return value; 
            }
            return super.getMessage(key); 
        }
    }


    private Configuration conf = null; 
    private Locale locale = null; 
    private String name = null; 

    public Configuration getConf() {
        return getConf(true); 
    }

    public Configuration getConf(boolean create) {
        synchronized (this) {
            if (conf == null) 
                conf = ConfigurationFactory.get(false); 
            if (conf == null && create) 
                conf = ConfigurationFactory.create(); 
        }
        return conf; 
    }

    public Locale getLocale() {
        return locale; 
    }

    public String getName() {
        return name; 
    }

    /**
     * The ResourceBundle for this StringManager.
     */
    private ResourceBundle bundle = null;
    private ResourceBundle appBundle = null;
    

    private StringManager(Locale loc) {
        this((Configuration)null, loc); 
    }

    private StringManager(Configuration conf, Locale loc) {
        this.conf = conf; 
        this.locale = loc; 
        if (loc != null) {
            String appBundleName = "appLocalStrings"; 
            this.appBundle = getResourceBundle(this, appBundleName, loc, false);
        }
    }
    
    /**
     * Creates a new StringManager for a given package. This is a
     * private method and all access to it is arbitrated by the
     * static getManager method call so that only one StringManager
     * per package will be created.
     *
     * @param packageName Name of package to create StringManager for.
     */
    private StringManager(String packageName, Locale loc) {
        this((Configuration)null, packageName, loc); 
    }

    private StringManager(Configuration conf, String packageName, Locale loc) {
        this.conf = conf; 
        this.locale = loc; 
        this.name = packageName; 
        String bundleName = packageName + ".LocalStrings";
        String appBundleName = packageName + ".appLocalStrings";
        this.bundle = getResourceBundle(this, bundleName, loc, true); 
        this.appBundle = getResourceBundle(this, appBundleName, loc, false); 
        if (this.appBundle == null) {
          appBundleName = "appLocalStrings"; 
          this.appBundle = getResourceBundle(this, appBundleName, loc, false);
        }
    }

    private static ResourceBundle getResourceBundle(
                      StringManager manager, String bundleName, Locale loc, boolean logWarn) {
        ResourceBundle bundle = null; 
        try {
            bundle = ResourceBundle.getBundle(bundleName, loc); 
            //LOG.info("Loaded resource " + bundleName + " for locale " + bundle.getLocale());
            return bundle;
        } catch( MissingResourceException ex ) {
            Locale defaultLocale = Locale.CHINESE; //Locale.getDefault(); 
            
            if (LOG.isWarnEnabled() && logWarn) 
                LOG.warn("Missing resource bundle: " + bundleName + 
                         " for Locale: " + loc + ", try load default: " + defaultLocale); 
            
            // Try from the current loader ( that's the case for trusted apps )
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if( cl != null ) {
                try {
                    bundle = ResourceBundle.getBundle(bundleName, defaultLocale, cl);
                    return bundle;
                } catch(MissingResourceException ex2) {
                    if (LOG.isErrorEnabled() && logWarn)
                        LOG.error("Can't find resource " + bundleName + " for default");
                }
            }
            if( cl == null )
                cl = manager.getClass().getClassLoader();

            if (LOG.isErrorEnabled() && logWarn)
                LOG.error("Can't find resource " + bundleName + " " + cl);
            
            if( cl instanceof URLClassLoader ) {
                if (LOG.isDebugEnabled() && logWarn) 
                    LOG.debug("URLClasLoader: " + ((URLClassLoader)cl).getURLs());
            }
        }
        return null; 
    }

    /**
     * Get a string from the underlying resource bundle.
     *
     * @param key The resource name
     */
    public String getString(String key) {
        return MessageFormat.format(getStringInternal(key), (Object [])null);
    }


    public String getMessage(String key) {
        String value = getStringInternal(key, false); 
        if (value != null) 
            return MessageFormat.format(value, (Object [])null);

        return getMessage0(key); 
    }

    private String getMessage0(String key) {
        String name = key, value = null; 
        if (locale != null) {
            String lang = locale.getLanguage();
            String country = locale.getCountry();
            if (country != null && country.length() > 0) 
                name = name + "_" + lang + "_" + country + ".message";
            else
                name = name + "_" + lang + ".message";

            value = getMessageFile(getConf(), name); 
            if (value == null || value.length() == 0) {
                name = key + ".message";
                value = getMessageFile(getConf(), name); 
            }
        } else {
            name = name + ".message";
            value = getMessageFile(getConf(), name); 
        }
        return value; 
    }

    private static class MessageFile {
        @SuppressWarnings("unused")
		public String filename = null; 
        public String message = null; 
        public long loadedTime = 0; 
    }

    private static HashMap<String, MessageFile> messages = new HashMap<String, MessageFile>(); 

    private static String getMessageFile(Configuration conf, String filename) {
        if (conf == null || filename == null || filename.length() == 0)
            return null; 

        long current = System.currentTimeMillis(); 
        String value = null; 
        MessageFile mfile = null; 
        synchronized (messages) {
            mfile = messages.get(filename); 
            if (mfile != null) {
                value = mfile.message; 
                long t = current - mfile.loadedTime; 
                if ((value != null || t < 60 * 60 * 1000) && t < 24 * 60 * 60 * 1000) 
                    return value; 
            }
        }

        try {
            Reader reader = conf.getConfResourceAsReader(filename); 
            if (reader != null) {
                BufferedReader br = new BufferedReader(reader); 
                String line = null; 
                StringBuffer sbuf = new StringBuffer(); 
                while ((line = br.readLine()) != null) {
                    sbuf.append(line); sbuf.append("\n"); 
                }
                value = sbuf.toString(); 

                //LOG.info("loaded message file: "+filename); 
                reader.close(); br.close(); 
            }
        } catch (Exception e) {
            if (mfile == null) 
                LOG.warn("load message file: "+filename+" failed: "+e); 
        }

        synchronized (messages) {
            mfile = new MessageFile(); 
            mfile.filename = filename; 
            mfile.message = value; 
            mfile.loadedTime = current; 

            messages.put(filename, mfile); 
        }

        return value; 
    }

    protected String getStringInternal(String key) {
        return getStringInternal(key, true); 
    }

    protected String getStringInternal(String key, boolean noempty) {
        String value = getStringInternal0(appBundle, key); 
        if (value == null) {
            if (bundle != null) {
                value = getStringInternal0(bundle, key); 
                if (value == null && noempty) 
                    value = "Cannot find message associated with key '" + key + "'";
            } else if (noempty) {
                value = key; 
            }
        }
        Configuration cnf = getConf(false);
        return cnf != null ? cnf.substituteVarsWin(value) : value; 
    }

    protected static String getStringInternal0(ResourceBundle bundle, String key) {
        if (key == null) 
            throw new NullPointerException("key is null");

        String str = null;

        if( bundle == null )
            return null; 
        try {
            str = bundle.getString(key);
        } catch (MissingResourceException mre) {
            str = null; 
        }

        return str;
    }

    /**
     * Get a string from the underlying resource bundle and format
     * it with the given set of arguments.
     *
     * @param key The resource name
     * @param args Formatting directives
     */
    public String getString(String key, Object[] args) {
        String iString = null;
        String value = getStringInternal(key);

        // this check for the runtime exception is some pre 1.1.6
        // VM's don't do an automatic toString() on the passed in
        // objects and barf out

        try {
            // ensure the arguments are not null so pre 1.2 VM's don't barf
            Object nonNullArgs[] = args;
            for (int i=0; i<args.length; i++) {
                if (args[i] == null) {
                    if (nonNullArgs==args) nonNullArgs=(Object[])args.clone();
                    nonNullArgs[i] = "null";
                }
            }

            iString = MessageFormat.format(value, nonNullArgs);
        } catch (IllegalArgumentException iae) {
            StringBuffer buf = new StringBuffer();
            buf.append(value);
            for (int i = 0; i < args.length; i++) {
                buf.append(" arg[" + i + "]=" + args[i]);
            }
            iString = buf.toString();
        }
        return iString;
    }

    /**
     * Get a string from the underlying resource bundle and format it
     * with the given object argument. This argument can of course be
     * a String object.
     *
     * @param key The resource name
     * @param arg Formatting directive
     */
    public String getString(String key, Object arg) {
        Object[] args = new Object[] {arg};
        return getString(key, args);
    }

    /**
     * Get a string from the underlying resource bundle and format it
     * with the given object arguments. These arguments can of course
     * be String objects.
     *
     * @param key The resource name
     * @param arg1 Formatting directive
     * @param arg2 Formatting directive
     */
    public String getString(String key, Object arg1, Object arg2) {
        Object[] args = new Object[] {arg1, arg2};
        return getString(key, args);
    }

    /**
     * Get a string from the underlying resource bundle and format it
     * with the given object arguments. These arguments can of course
     * be String objects.
     *
     * @param key The resource name
     * @param arg1 Formatting directive
     * @param arg2 Formatting directive
     * @param arg3 Formatting directive
     */
    public String getString(String key, Object arg1, Object arg2,
                            Object arg3) {
        Object[] args = new Object[] {arg1, arg2, arg3};
        return getString(key, args);
    }

    /**
     * Get a string from the underlying resource bundle and format it
     * with the given object arguments. These arguments can of course
     * be String objects.
     *
     * @param key The resource name
     * @param arg1 Formatting directive
     * @param arg2 Formatting directive
     * @param arg3 Formatting directive
     * @param arg4 Formatting directive
     */
    public String getString(String key, Object arg1, Object arg2,
                            Object arg3, Object arg4) {
        Object[] args = new Object[] {arg1, arg2, arg3, arg4};
        return getString(key, args);
    }


    private static Locale defaultLocale = Locale.CHINESE; //Locale.getDefault(); 
    private static HashSet<String> allLocales = new HashSet<String>(); 
    
    static { 
        Locale[] locales = Locale.getAvailableLocales(); 
        for (int i = 0; i < locales.length; i ++) {
            allLocales.add(getLocaleKey(locales[i])); 
        }
    }
    
    public synchronized static boolean setLocale(Locale loc) {
        if (loc != null) {
            String localeKey = getLocaleKey(loc); 
            if (!allLocales.contains(localeKey)) 
                return false; 
            
            if (!getDefaultLocaleKey().equals(localeKey)) {
                defaultLocale = loc; 
                return true; 
            }
        }
        return false; 
    }

    public static String getLocaleKey(Locale loc) {
        if (loc == null) return null; 
        String locstr = loc.getLanguage(); 
        String country = loc.getCountry(); 
        if (country != null && country.length() > 0) 
          locstr = locstr + "_" + country; 
        return locstr.toLowerCase(); 
    }
    
    public synchronized static String getDefaultLocaleKey() {
        return getLocaleKey(defaultLocale); 
    }

    public synchronized static boolean setLocale(String language, String country) {
        try {
            if (country == null) 
                return setLocale(language); 
            
            Locale loc = new Locale(language, country); 
            return setLocale(loc); 
        }
        catch (NullPointerException e) {
            if (LOG.isDebugEnabled()) 
                LOG.warn("wrong locale lang: " + language + " and country: " + country); 
        } 
        return false; 
    }

    public synchronized static boolean setLocale(String language) {
        try {
            Locale loc = new Locale(language); 
            return setLocale(loc); 
        }
        catch (NullPointerException e) {
            if (LOG.isDebugEnabled()) 
                LOG.debug("wrong locale lang: " + language); 
        } 
        return false; 
    }
    

    private static HashMap<String, HashMap<String, StringManager> > allManagers = 
                        new HashMap<String, HashMap<String, StringManager> >();

    /**
     * Get the StringManager for a particular package. If a manager for
     * a package already exists, it will be reused, else a new
     * StringManager will be created and returned.
     *
     * @param packageName The package name
     */
    public static StringManager getManager(String packageName) {
        String localeKey = getDefaultLocaleKey(); 
        return getManager(localeKey, packageName); 
    }

    public static StringManager getManager(Locale locale, String packageName) {
        return getManager(getLocaleKey(locale), packageName); 
    }

    public static StringManager getManager(Locale locale, String[] packageNames) {
        return getManager(getLocaleKey(locale), packageNames); 
    }

    public static StringManager getManager(String localeKey, String[] packageNames) {
        return getManagers(localeKey, packageNames); 
    }

    public static StringManagers getManagers(String localeKey) {
        return getManagers(localeKey, null); 
    }

    public static StringManagers getManagers(String localeKey, String[] packageNames) {
        StringManagers manager = new StringManagers(createLocale(localeKey)); 
        for (int i=0; packageNames != null && i < packageNames.length; i++) {
            String name = packageNames[i]; 
            if (name == null || name.length() == 0) continue; 
            manager.add(name); 
        }
        manager.add(StringManagers.class.getPackage().getName()); 
        return manager; 
    }

    public synchronized static StringManager getManager(String localeKey, String packageName) {
        if (localeKey == null) 
            localeKey = getDefaultLocaleKey(); 

        HashMap<String, StringManager> managers = allManagers.get(localeKey); 
        
        if (managers == null) {
            //LOG.info("creating managers for locale: " + localeKey); 
            
            managers = new HashMap<String, StringManager>(); 
            allManagers.put(localeKey, managers); 
        }
        
        StringManager mgr = (StringManager)managers.get(packageName);

        if (mgr == null) {
            //LOG.info("creating string manager for package: " + packageName + " with locale: " + localeKey); 
            
            mgr = new StringManager(packageName, createLocale(localeKey)); 
            managers.put(packageName, mgr); 
        }
        
        return mgr;
    }

    public static Locale createLocale(String localeKey) {
        if (localeKey == null || localeKey.length() == 0) 
            return defaultLocale; 

        String language = null; 
        String country = null; 
        StringTokenizer st = new StringTokenizer(localeKey, "_."); 
        while (st.hasMoreTokens()) {
            if (language == null) 
                language = st.nextToken(); 
            else if (country == null) 
                country = st.nextToken(); 
            else
                break; 
        }

        Locale locale = null; 

        if (language != null && country != null) 
            locale = new Locale(language, country); 
        else if (language != null) 
            locale = new Locale(language); 

        if (locale == null) 
            locale = defaultLocale; 

        return locale; 
    }
    
}
