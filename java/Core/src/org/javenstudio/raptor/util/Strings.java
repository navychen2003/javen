package org.javenstudio.raptor.util;


/**
 * An internationalization / localization helper class which reduces
 * the bother of handling ResourceBundles and takes care of the
 * common cases of message formating which otherwise require the
 * creation of Object arrays and such.
 *
 */
public class Strings {
    //private static final Logger LOG = Logger.getLogger(Strings.class);

    private static StringManager.StringManagers strings = null;
    private static Object lockStrings = new Object();

    private static StringManager.StringManagers getStrings() {
      synchronized (lockStrings) {
        if (strings == null) {
          String locale = System.getenv("HAWK_LANGUAGE"); 
          if (locale == null || locale.length() == 0)
            locale = System.getProperty("user.language") + "_" + System.getProperty("user.country");
          strings = StringManager.getManagers(locale);
          //strings.add(StringManager.class.getPackage().getName());
        }
        return strings;
      }
    }

    public static String get(String name) {
      return getStrings().getString(name);
    }

    public static void addStrings(String name) {
      getStrings().add(name);
    }

}
