package org.javenstudio.panda.snowball;

import java.lang.reflect.Method;

/**
 * This is the rev 502 of the Snowball SVN trunk,
 * but modified:
 * made abstract and introduced abstract method stem to avoid expensive reflection in filter class.
 * refactored StringBuffers to StringBuilder
 * uses char[] as buffer instead of StringBuffer/StringBuilder
 * eq_s,eq_s_b,insert,replace_s take CharSequence like eq_v and eq_v_b
 * reflection calls (Lovins, etc) use EMPTY_ARGS/EMPTY_PARAMS
 */
public class Among {
  private static final Class<?>[] EMPTY_PARAMS = new Class[0];

  public Among(String s, int substring_i, int result,
               String methodname, SnowballProgram methodobject) {
    this.s_size = s.length();
    this.s = s.toCharArray();
    this.substring_i = substring_i;
    this.result = result;
    this.methodobject = methodobject;
    if (methodname.length() == 0) {
      this.method = null;
    } else {
      try {
        this.method = methodobject.getClass().
            getDeclaredMethod(methodname, EMPTY_PARAMS);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }

    public final int s_size; /* search string */
    public final char[] s; /* search string */
    public final int substring_i; /* index to longest matching substring */
    public final int result;      /* result of the lookup */
    public final Method method; /* method to use if substring matches */
    public final SnowballProgram methodobject; /* object to invoke method on */
   
};
