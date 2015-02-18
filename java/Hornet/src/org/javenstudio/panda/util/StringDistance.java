package org.javenstudio.panda.util;

/**
 * Interface for string distances.
 */
public interface StringDistance {

	/**
	 * Returns a float between 0 and 1 based on how similar the specified strings are to one another.  
	 * Returning a value of 1 means the specified strings are identical and 0 means the
	 * string are maximally different.
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return a float between 0 and 1 based on how similar the specified strings are to one another.
	 */
	public float getDistance(String s1,String s2);
  
}
