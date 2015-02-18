package org.javenstudio.raptor.io;

import org.javenstudio.raptor.conf.*;
import org.javenstudio.raptor.util.ReflectionUtils;
import java.util.HashMap;

/** 
 * Factories for non-public writables.  Defining a factory permits {@link
 * ObjectWritable} to be able to construct instances of non-public classes. 
 */
@SuppressWarnings("rawtypes")
public class WritableFactories {
  private static final HashMap<Class, WritableFactory> CLASS_TO_FACTORY =
    new HashMap<Class, WritableFactory>();

  private WritableFactories() {}                  // singleton

  /** Define a factory for a class. */
  public static synchronized void setFactory(Class c, WritableFactory factory) {
    CLASS_TO_FACTORY.put(c, factory);
  }

  /** Define a factory for a class. */
  public static synchronized WritableFactory getFactory(Class c) {
    return CLASS_TO_FACTORY.get(c);
  }

  /** Create a new instance of a class with a defined factory. */
  public static Writable newInstance(Class<? extends Writable> c, Configuration conf) {
    WritableFactory factory = WritableFactories.getFactory(c);
    if (factory != null) {
      Writable result = factory.newInstance();
      if (result instanceof Configurable) {
        ((Configurable) result).setConf(conf);
      }
      return result;
    } else {
      return ReflectionUtils.newInstance(c, conf);
    }
  }
  
  /** Create a new instance of a class with a defined factory. */
  public static Writable newInstance(Class<? extends Writable> c) {
    return newInstance(c, null);
  }

}
