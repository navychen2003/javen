package org.javenstudio.falcon.datum.table.store;

import java.util.HashMap;
import java.util.Map;

/**
 * A Static Interface.
 * Instead of having this code in the the DBMapWritable code, where it
 * blocks the possibility of altering the variables and changing their types,
 * it is put here in this static interface where the static final Maps are
 * loaded one time. Only byte[] and Cell are supported at this time.
 */
public interface CodeToClassAndBack {
  /**
   * Static map that contains mapping from code to class
   */
  public static final Map<Byte, Class<?>> CODE_TO_CLASS =
    new HashMap<Byte, Class<?>>();

  /**
   * Static map that contains mapping from class to code
   */
  public static final Map<Class<?>, Byte> CLASS_TO_CODE =
    new HashMap<Class<?>, Byte>();

  /**
   * Class list for supported classes
   */
  static final Class<?>[] sClassList = {byte[].class};

  /**
   * The static loader that is used instead of the static constructor in
   * DBMapWritable.
   */
  static final InternalStaticLoader sStaticLoader =
    new InternalStaticLoader(sClassList, CODE_TO_CLASS, CLASS_TO_CODE);

  /**
   * Class that loads the static maps with their values.
   */
  public class InternalStaticLoader{
    InternalStaticLoader(Class<?>[] classList,
        Map<Byte,Class<?>> CODE_TO_CLASS, Map<Class<?>, Byte> CLASS_TO_CODE){
      byte code = 1;
      for (int i=0; i < classList.length; i++) {
        CLASS_TO_CODE.put(classList[i], code);
        CODE_TO_CLASS.put(code, classList[i]);
        code++;
      }
    }
  }
}
