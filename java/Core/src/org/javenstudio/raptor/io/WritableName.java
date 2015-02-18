package org.javenstudio.raptor.io;

import java.util.HashMap;
import java.io.IOException;

import org.javenstudio.raptor.conf.Configuration;


/** 
 * Utility to permit renaming of Writable implementation classes without
 * invalidiating files that contain their class name.
 */
@SuppressWarnings("deprecation")
public class WritableName {
  private static HashMap<String, Class<?>> NAME_TO_CLASS =
    new HashMap<String, Class<?>>();
  private static HashMap<Class<?>, String> CLASS_TO_NAME =
    new HashMap<Class<?>, String>();

  static {                                        // define important types
    WritableName.setName(NullWritable.class, "null");
    WritableName.setName(LongWritable.class, "long");
    WritableName.setName(UTF8.class, "UTF8");
    WritableName.setName(MD5Hash.class, "MD5Hash");
  }

  private WritableName() {}                      // no public ctor

  /** Set the name that a class should be known as to something other than the
   * class name. */
  @SuppressWarnings("rawtypes")
  public static synchronized void setName(Class writableClass, String name) {
    CLASS_TO_NAME.put(writableClass, name);
    NAME_TO_CLASS.put(name, writableClass);
  }

  /** Add an alternate name for a class. */
  @SuppressWarnings("rawtypes")
  public static synchronized void addName(Class writableClass, String name) {
    NAME_TO_CLASS.put(name, writableClass);
  }

  /** Return the name for a class.  Default is {@link Class#getName()}. */
  @SuppressWarnings("rawtypes")
  public static synchronized String getName(Class writableClass) {
    String name = CLASS_TO_NAME.get(writableClass);
    if (name != null)
      return name;
    return writableClass.getName();
  }

  /** Return the class for a name.  Default is {@link Class#forName(String)}.*/
  public static synchronized Class<?> getClass(String name, Configuration conf
                                            ) throws IOException {
    Class<?> writableClass = NAME_TO_CLASS.get(name);
    if (writableClass != null)
      return writableClass.asSubclass(Writable.class);
    try {
      return conf.getClassByName(name);
    } catch (ClassNotFoundException e) {
      IOException newE = new IOException("WritableName can't load class: " + name);
      newE.initCause(e);
      throw newE;
    }
  }

}
