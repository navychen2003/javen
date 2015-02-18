package org.javenstudio.panda.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple class that stores Strings as char[]'s in a
 * hash table.  Note that this is not a general purpose
 * class.  For example, it cannot remove items from the
 * set, nor does it resize its hash table to be smaller,
 * etc.  It is designed to be quick to test if a char[]
 * is in the set without the necessity of converting it
 * to a String first.
 *
 * <a name="version"></a>
 * <p>You must specify the required {@link Version}
 * compatibility when creating {@link CharArraySet}:
 * <ul>
 *   <li> As of 3.1, supplementary characters are
 *       properly lowercased.</li>
 * </ul>
 * Before 3.1 supplementary characters could not be
 * lowercased correctly due to the lack of Unicode 4
 * support in JDK 1.4. To use instances of
 * {@link CharArraySet} with the behavior before Lucene
 * 3.1 pass a {@link Version} < 3.1 to the constructors.
 * <P>
 * <em>Please note:</em> This class implements {@link java.util.Set Set} but
 * does not behave like it should in all cases. The generic type is
 * {@code Set<Object>}, because you can add any object to it,
 * that has a string representation. The add methods will use
 * {@link Object#toString} and store the result using a {@code char[]}
 * buffer. The same behavior have the {@code contains()} methods.
 * The {@link #iterator()} returns an {@code Iterator<char[]>}.
 */
public class CharArraySet extends AbstractSet<Object> {
	
	public static final CharArraySet EMPTY_SET = 
			new CharArraySet(CharArrayMap.<Object>emptyMap());
	
	private static final Object PLACEHOLDER = new Object();
  
	private final CharArrayMap<Object> mMap;
  
	/**
	 * Create set with enough capacity to hold startSize terms
	 * 
	 * @param matchVersion
	 *          compatibility match version see <a href="#version">Version
	 *          note</a> above for details.
	 * @param startSize
	 *          the initial capacity
	 * @param ignoreCase
	 *          <code>false</code> if and only if the set should be case sensitive
	 *          otherwise <code>true</code>.
	 */
	public CharArraySet(int startSize, boolean ignoreCase) {
		this(new CharArrayMap<Object>(startSize, ignoreCase));
	}

	/**
	 * Creates a set from a Collection of objects. 
	 * 
	 * @param matchVersion
	 *          compatibility match version see <a href="#version">Version
	 *          note</a> above for details.
	 * @param c
	 *          a collection whose elements to be placed into the set
	 * @param ignoreCase
	 *          <code>false</code> if and only if the set should be case sensitive
	 *          otherwise <code>true</code>.
	 */
	public CharArraySet(Collection<?> c, boolean ignoreCase) {
		this(c.size(), ignoreCase);
		addAll(c);
	}

	/** 
	 * Create set from the specified map (internal only), 
	 * used also by {@link CharArrayMap#keySet()} 
	 */
	CharArraySet(final CharArrayMap<Object> map){
		mMap = map;
	}
  
	/** 
	 * Clears all entries in this set. This method is supported for reusing, 
	 * but not {@link Set#remove}. 
	 */
	@Override
	public void clear() {
		mMap.clear();
	}

	/** 
	 * true if the <code>len</code> chars of <code>text</code> starting at <code>off</code>
	 * are in the set 
	 */
	public boolean contains(char[] text, int off, int len) {
		return mMap.containsKey(text, off, len);
	}

	/** true if the <code>CharSequence</code> is in the set */
	public boolean contains(CharSequence cs) {
		return mMap.containsKey(cs);
	}

	@Override
	public boolean contains(Object o) {
		return mMap.containsKey(o);
	}

	@Override
	public boolean add(Object o) {
		return mMap.put(o, PLACEHOLDER) == null;
	}

	/** Add this CharSequence into the set */
	public boolean add(CharSequence text) {
		return mMap.put(text, PLACEHOLDER) == null;
	}
  
	/** Add this String into the set */
	public boolean add(String text) {
		return mMap.put(text, PLACEHOLDER) == null;
	}

	/** 
	 * Add this char[] directly to the set.
	 * If ignoreCase is true for this Set, the text array will be directly modified.
	 * The user should never modify this text array after calling this method.
	 */
	public boolean add(char[] text) {
		return mMap.put(text, PLACEHOLDER) == null;
	}

	@Override
	public int size() {
		return mMap.size();
	}
  
	/**
	 * Returns an unmodifiable {@link CharArraySet}. This allows to provide
	 * unmodifiable views of internal sets for "read-only" use.
	 * 
	 * @param set
	 *          a set for which the unmodifiable set is returned.
	 * @return an new unmodifiable {@link CharArraySet}.
	 * @throws NullPointerException
	 *           if the given set is <code>null</code>.
	 */
	public static CharArraySet unmodifiableSet(CharArraySet set) {
		if (set == null)
			throw new NullPointerException("Given set is null");
		
		if (set == EMPTY_SET)
			return EMPTY_SET;
		
		if (set.mMap instanceof CharArrayMap.UnmodifiableCharArrayMap)
			return set;
		
		return new CharArraySet(CharArrayMap.unmodifiableMap(set.mMap));
	}

	/**
	 * Returns a copy of the given set as a {@link CharArraySet}. If the given set
	 * is a {@link CharArraySet} the ignoreCase property will be preserved.
	 * <p>
	 * <b>Note:</b> If you intend to create a copy of another {@link CharArraySet} where
	 * the {@link Version} of the source set differs from its copy
	 * {@link #CharArraySet(Version, Collection, boolean)} should be used instead.
	 * The {@link #copy(Version, Set)} will preserve the {@link Version} of the
	 * source set it is an instance of {@link CharArraySet}.
	 * </p>
	 * 
	 * @param matchVersion
	 *          compatibility match version see <a href="#version">Version
	 *          note</a> above for details. This argument will be ignored if the
	 *          given set is a {@link CharArraySet}.
	 * @param set
	 *          a set to copy
	 * @return a copy of the given set as a {@link CharArraySet}. If the given set
	 *         is a {@link CharArraySet} the ignoreCase property as well as the
	 *         matchVersion will be of the given set will be preserved.
	 */
	public static CharArraySet copy(final Set<?> set) {
		if (set == EMPTY_SET)
			return EMPTY_SET;
		
		if (set instanceof CharArraySet) {
			final CharArraySet source = (CharArraySet) set;
			return new CharArraySet(CharArrayMap.copy(source.mMap));
		}
		
		return new CharArraySet(set, false);
	}
  
	/**
	 * Returns an {@link Iterator} for {@code char[]} instances in this set.
	 */
	public Iterator<Object> iterator() {
		// use the AbstractSet#keySet()'s iterator (to not produce endless recursion)
		return mMap.originalKeySet().iterator();
	}
  
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");
		for (Object item : this) {
			if (sb.length()>1) sb.append(", ");
			if (item instanceof char[]) 
				sb.append((char[]) item);
			else 
				sb.append(item);
		}
		return sb.append(']').toString();
	}
	
}
