package org.javenstudio.panda.util;

import java.util.Arrays;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.analysis.CharacterUtils;

/**
 * A simple class that stores key Strings as char[]'s in a
 * hash table. Note that this is not a general purpose
 * class.  For example, it cannot remove items from the
 * map, nor does it resize its hash table to be smaller,
 * etc.  It is designed to be quick to retrieve items
 * by char[] keys without the necessity of converting
 * to a String first.
 *
 * <a name="version"></a>
 * <p>You must specify the required {@link Version}
 * compatibility when creating {@link CharArrayMap}:
 * <ul>
 *   <li> As of 3.1, supplementary characters are
 *       properly lowercased.</li>
 * </ul>
 * Before 3.1 supplementary characters could not be
 * lowercased correctly due to the lack of Unicode 4
 * support in JDK 1.4. To use instances of
 * {@link CharArrayMap} with the behavior before Lucene
 * 3.1 pass a {@link Version} &lt; 3.1 to the constructors.
 */
public class CharArrayMap<V> extends AbstractMap<Object,V> {
	
	// private only because missing generics
	private static final CharArrayMap<?> EMPTY_MAP = new EmptyCharArrayMap<Object>();
	private static final int INIT_SIZE = 8;
	
	private boolean mIgnoreCase;  
	private int mCount;
	// package private because used in CharArraySet's non Set-conform CharArraySetIterator
	private char[][] mKeys; 
	// package private because used in CharArraySet's non Set-conform CharArraySetIterator
	private V[] mValues; 

	/**
	 * Create map with enough capacity to hold startSize terms
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
	@SuppressWarnings("unchecked")
	public CharArrayMap(int startSize, boolean ignoreCase) {
		mIgnoreCase = ignoreCase;
		
		int size = INIT_SIZE;
		while (startSize + (startSize>>2) > size) {
			size <<= 1;
		}
		
		mKeys = new char[size][];
		mValues = (V[]) new Object[size];
	}

	/**
	 * Creates a map from the mappings in another map. 
	 * 
	 * @param matchVersion
	 *          compatibility match version see <a href="#version">Version
	 *          note</a> above for details.
	 * @param c
	 *          a map whose mappings to be copied
	 * @param ignoreCase
	 *          <code>false</code> if and only if the set should be case sensitive
	 *          otherwise <code>true</code>.
	 */
	public CharArrayMap(Map<?,? extends V> c, boolean ignoreCase) {
		this(c.size(), ignoreCase);
		putAll(c);
	}
  
	/** Create set from the supplied map (used internally for readonly maps...) */
	private CharArrayMap(CharArrayMap<V> toCopy){
		mKeys = toCopy.mKeys;
		mValues = toCopy.mValues;
		mIgnoreCase = toCopy.mIgnoreCase;
		mCount = toCopy.mCount;
	}
  
	/** 
	 * Clears all entries in this map. This method is supported for reusing, 
	 * but not {@link Map#remove}. 
	 */
	@Override
	public void clear() {
		mCount = 0;
		Arrays.fill(mKeys, null);
		Arrays.fill(mValues, null);
	}

	/** 
	 * true if the <code>len</code> chars of <code>text</code> starting at <code>off</code>
	 * are in the {@link #keySet()} 
	 */
	public boolean containsKey(char[] text, int off, int len) {
		return mKeys[getSlot(text, off, len)] != null;
	}

	/** true if the <code>CharSequence</code> is in the {@link #keySet()} */
	public boolean containsKey(CharSequence cs) {
		return mKeys[getSlot(cs)] != null;
	}

	@Override
	public boolean containsKey(Object o) {
		if (o instanceof char[]) {
			final char[] text = (char[])o;
			return containsKey(text, 0, text.length);
		}
		
		return containsKey(o.toString());
	}

	/** 
	 * returns the value of the mapping of <code>len</code> chars of <code>text</code>
	 * starting at <code>off</code> 
	 */
	public V get(char[] text, int off, int len) {
		return mValues[getSlot(text, off, len)];
	}

	/** returns the value of the mapping of the chars inside this {@code CharSequence} */
	public V get(CharSequence cs) {
		return mValues[getSlot(cs)];
	}

	@Override
	public V get(Object o) {
		if (o instanceof char[]) {
			final char[] text = (char[])o;
			return get(text, 0, text.length);
		}
		
		return get(o.toString());
	}

	private int getSlot(char[] text, int off, int len) {
		int code = getHashCode(text, off, len);
		int pos = code & (mKeys.length-1);
		char[] text2 = mKeys[pos];
		
		if (text2 != null && !equals(text, off, len, text2)) {
			final int inc = ((code>>8)+code)|1;
			do {
				code += inc;
				pos = code & (mKeys.length-1);
				text2 = mKeys[pos];
			} while (text2 != null && !equals(text, off, len, text2));
		}
		
		return pos;
	}

	/** Returns true if the String is in the set */  
	private int getSlot(CharSequence text) {
		int code = getHashCode(text);
		int pos = code & (mKeys.length-1);
		char[] text2 = mKeys[pos];
		
		if (text2 != null && !equals(text, text2)) {
			final int inc = ((code>>8)+code)|1;
			do {
				code += inc;
				pos = code & (mKeys.length-1);
				text2 = mKeys[pos];
			} while (text2 != null && !equals(text, text2));
		}
		
		return pos;
	}

	/** Add the given mapping. */
	public V put(CharSequence text, V value) {
		return put(text.toString(), value); // could be more efficient
	}

	@Override
	public V put(Object o, V value) {
		if (o instanceof char[]) {
			return put((char[])o, value);
		}
		return put(o.toString(), value);
	}
  
	/** Add the given mapping. */
	public V put(String text, V value) {
		return put(text.toCharArray(), value);
	}

	/** 
	 * Add the given mapping.
	 * If ignoreCase is true for this Set, the text array will be directly modified.
	 * The user should never modify this text array after calling this method.
	 */
	public V put(char[] text, V value) {
		if (mIgnoreCase)
			for (int i=0; i < text.length;) {
				i += Character.toChars(Character.toLowerCase(
						CharacterUtils.getInstance().codePointAt(text, i)), text, i);
			}
		
		int slot = getSlot(text, 0, text.length);
		if (mKeys[slot] != null) {
			final V oldValue = mValues[slot];
			mValues[slot] = value;
			return oldValue;
		}
		
		mKeys[slot] = text;
		mValues[slot] = value;
		mCount++;

		if (mCount + (mCount>>2) > mKeys.length) 
			rehash();

		return null;
	}

	@SuppressWarnings("unchecked")
	private void rehash() {
		assert mKeys.length == mValues.length;
		
		final int newSize = 2*mKeys.length;
		final char[][] oldkeys = mKeys;
		final V[] oldvalues = mValues;
		
		mKeys = new char[newSize][];
		mValues = (V[]) new Object[newSize];

		for (int i=0; i < oldkeys.length; i++) {
			char[] text = oldkeys[i];
			if (text != null) {
				// todo: could be faster... no need to compare strings on collision
				final int slot = getSlot(text,0,text.length);
				mKeys[slot] = text;
				mValues[slot] = oldvalues[i];
			}
		}
	}
  
	private boolean equals(char[] text1, int off, int len, char[] text2) {
		if (len != text2.length)
			return false;
		
		final int limit = off+len;
		if (mIgnoreCase) {
			for (int i=0; i < len;) {
				final int codePointAt = CharacterUtils.getInstance().codePointAt(text1, off+i, limit);
				if (Character.toLowerCase(codePointAt) != CharacterUtils.getInstance().codePointAt(text2, i))
					return false;
				i += Character.charCount(codePointAt); 
			}
		} else {
			for (int i=0; i < len; i++) {
				if (text1[off+i] != text2[i])
					return false;
			}
		}
		return true;
	}

	private boolean equals(CharSequence text1, char[] text2) {
		int len = text1.length();
		if (len != text2.length)
			return false;
		
		if (mIgnoreCase) {
			for (int i=0; i < len;) {
				final int codePointAt = CharacterUtils.getInstance().codePointAt(text1, i);
				if (Character.toLowerCase(codePointAt) != CharacterUtils.getInstance().codePointAt(text2, i))
					return false;
				i += Character.charCount(codePointAt);
			}
		} else {
			for (int i=0; i < len; i++) {
				if (text1.charAt(i) != text2[i])
					return false;
			}
		}
		return true;
	}
  
	private int getHashCode(char[] text, int offset, int len) {
		if (text == null)
			throw new NullPointerException();
		
		int code = 0;
		final int stop = offset + len;
		
		if (mIgnoreCase) {
			for (int i=offset; i < stop;) {
				final int codePointAt = CharacterUtils.getInstance().codePointAt(text, i, stop);
				code = code*31 + Character.toLowerCase(codePointAt);
				i += Character.charCount(codePointAt);
			}
		} else {
			for (int i=offset; i < stop; i++) {
				code = code*31 + text[i];
			}
		}
		return code;
	}

	private int getHashCode(CharSequence text) {
		if (text == null)
			throw new NullPointerException();
		
		int code = 0;
		int len = text.length();
		
		if (mIgnoreCase) {
			for (int i=0; i<len;) {
				int codePointAt = CharacterUtils.getInstance().codePointAt(text, i);
				code = code*31 + Character.toLowerCase(codePointAt);
				i += Character.charCount(codePointAt);
			}
		} else {
			for (int i=0; i < len; i++) {
				code = code*31 + text.charAt(i);
			}
		}
		return code;
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return mCount;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		for (Map.Entry<Object,V> entry : entrySet()) {
			if (sb.length()>1) sb.append(", ");
			sb.append(entry);
		}
		return sb.append('}').toString();
	}

	private EntrySet mEntrySet = null;
	private CharArraySet mKeySet = null;
  
	EntrySet createEntrySet() {
		return new EntrySet(true);
	}
  
	@Override
	public final EntrySet entrySet() {
		if (mEntrySet == null) 
			mEntrySet = createEntrySet();
		
		return mEntrySet;
	}
  
	// helper for CharArraySet to not produce endless recursion
	final Set<Object> originalKeySet() {
		return super.keySet();
	}

	/** 
	 * Returns an {@link CharArraySet} view on the map's keys.
	 * The set will use the same {@code matchVersion} as this map. 
	 */
	@Override 
	@SuppressWarnings({"unchecked","rawtypes"})
	public final CharArraySet keySet() {
		if (mKeySet == null) {
			// prevent adding of entries
			mKeySet = new CharArraySet((CharArrayMap) this) {
				@Override
				public boolean add(Object o) {
					throw new UnsupportedOperationException();
				}
				@Override
				public boolean add(CharSequence text) {
					throw new UnsupportedOperationException();
				}
				@Override
				public boolean add(String text) {
					throw new UnsupportedOperationException();
				}
				@Override
				public boolean add(char[] text) {
					throw new UnsupportedOperationException();
				}
			};
		}
		return mKeySet;
	}

	/** public iterator class so efficient methods are exposed to users */
	public class EntryIterator implements Iterator<Map.Entry<Object,V>> {
		private final boolean mAllowModify;
		private int mPos = -1;
		private int mLastPos;
		
		private EntryIterator(boolean allowModify) {
			mAllowModify = allowModify;
			goNext();
		}

		private void goNext() {
			mLastPos = mPos;
			mPos ++;
			while (mPos < mKeys.length && mKeys[mPos] == null) {
				mPos++;
			}
		}

		@Override
		public boolean hasNext() {
			return mPos < mKeys.length;
		}

		/** gets the next key... do not modify the returned char[] */
		public char[] nextKey() {
			goNext();
			return mKeys[mLastPos];
		}

		/** gets the next key as a newly created String object */
		public String nextKeyString() {
			return new String(nextKey());
		}

		/** returns the value associated with the last key returned */
		public V currentValue() {
			return mValues[mLastPos];
		}

		/** sets the value associated with the last key returned */    
		public V setValue(V value) {
			if (!mAllowModify)
				throw new UnsupportedOperationException();
			V old = mValues[mLastPos];
			mValues[mLastPos] = value;
			return old;      
		}

		/** use nextCharArray() + currentValue() for better efficiency. */
		@Override
		public Map.Entry<Object,V> next() {
			goNext();
			return new MapEntry(mLastPos, mAllowModify);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final class MapEntry implements Map.Entry<Object,V> {
		private final boolean mAllowModify;
		private final int mPos;
		
		private MapEntry(int pos, boolean allowModify) {
			mPos = pos;
			mAllowModify = allowModify;
		}

		@Override
		public Object getKey() {
			// we must clone here, as putAll to another CharArrayMap
			// with other case sensitivity flag would corrupt the keys
			return mKeys[mPos].clone();
		}

		@Override
		public V getValue() {
			return mValues[mPos];
		}

		public V setValue(V value) {
			if (!mAllowModify)
				throw new UnsupportedOperationException();
			final V old = mValues[mPos];
			mValues[mPos] = value;
			return old;
		}

		@Override
		public String toString() {
			return new StringBuilder().append(mKeys[mPos]).append('=')
					.append((mValues[mPos] == CharArrayMap.this) ? "(this Map)" : mValues[mPos])
					.toString();
		}
	}

	/** public EntrySet class so efficient methods are exposed to users */
	public final class EntrySet extends AbstractSet<Map.Entry<Object,V>> {
		private final boolean mAllowModify;
    
		private EntrySet(boolean allowModify) {
			mAllowModify = allowModify;
		}
  
		@Override
		public EntryIterator iterator() {
			return new EntryIterator(mAllowModify);
		}
    
		@Override
		@SuppressWarnings("unchecked")
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<Object,V> e = (Map.Entry<Object,V>)o;
			final Object key = e.getKey();
			final Object val = e.getValue();
			final Object v = get(key);
			return v == null ? val == null : v.equals(val);
		}
    
		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}
    
		@Override
		public int size() {
			return mCount;
		}
    
		@Override
		public void clear() {
			if (!mAllowModify)
				throw new UnsupportedOperationException();
			CharArrayMap.this.clear();
		}
	}
  
	/**
	 * Returns an unmodifiable {@link CharArrayMap}. This allows to provide
	 * unmodifiable views of internal map for "read-only" use.
	 * 
	 * @param map
	 *          a map for which the unmodifiable map is returned.
	 * @return an new unmodifiable {@link CharArrayMap}.
	 * @throws NullPointerException
	 *           if the given map is <code>null</code>.
	 */
	public static <V> CharArrayMap<V> unmodifiableMap(CharArrayMap<V> map) {
		if (map == null)
			throw new NullPointerException("Given map is null");
		if (map == emptyMap() || map.isEmpty())
			return emptyMap();
		if (map instanceof UnmodifiableCharArrayMap)
			return map;
		return new UnmodifiableCharArrayMap<V>(map);
	}

	/**
	 * Returns a copy of the given map as a {@link CharArrayMap}. If the given map
	 * is a {@link CharArrayMap} the ignoreCase property will be preserved.
	 * <p>
	 * <b>Note:</b> If you intend to create a copy of another {@link CharArrayMap} where
	 * the {@link Version} of the source map differs from its copy
	 * {@link #CharArrayMap(Version, Map, boolean)} should be used instead.
	 * The {@link #copy(Version, Map)} will preserve the {@link Version} of the
	 * source map it is an instance of {@link CharArrayMap}.
	 * </p>
	 * 
	 * @param matchVersion
	 *          compatibility match version see <a href="#version">Version
	 *          note</a> above for details. This argument will be ignored if the
	 *          given map is a {@link CharArrayMap}.
	 * @param map
	 *          a map to copy
	 * @return a copy of the given map as a {@link CharArrayMap}. If the given map
	 *         is a {@link CharArrayMap} the ignoreCase property as well as the
	 *         matchVersion will be of the given map will be preserved.
	 */
	@SuppressWarnings("unchecked")
	public static <V> CharArrayMap<V> copy(final Map<?,? extends V> map) {
		if (map == EMPTY_MAP)
			return emptyMap();
		
		if (map instanceof CharArrayMap) {
			CharArrayMap<V> m = (CharArrayMap<V>) map;
			
			// use fast path instead of iterating all values
			// this is even on very small sets ~10 times faster than iterating
			final char[][] keys = new char[m.mKeys.length][];
			System.arraycopy(m.mKeys, 0, keys, 0, keys.length);
			
			final V[] values = (V[]) new Object[m.mValues.length];
			System.arraycopy(m.mValues, 0, values, 0, values.length);
			
			m = new CharArrayMap<V>(m);
			m.mKeys = keys;
			m.mValues = values;
			
			return m;
		}
		
		return new CharArrayMap<V>(map, false);
	}
  
	/** Returns an empty, unmodifiable map. */
	@SuppressWarnings("unchecked")
	public static <V> CharArrayMap<V> emptyMap() {
		return (CharArrayMap<V>) EMPTY_MAP;
	}
  
	// package private CharArraySet instanceof check in CharArraySet
	static class UnmodifiableCharArrayMap<V> extends CharArrayMap<V> {

		UnmodifiableCharArrayMap(CharArrayMap<V> map) {
			super(map);
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public V put(Object o, V val){
			throw new UnsupportedOperationException();
		}
    
		@Override
		public V put(char[] text, V val) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V put(CharSequence text, V val) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V put(String text, V val) {
			throw new UnsupportedOperationException();
		}
    
		@Override
		public V remove(Object key) {
			throw new UnsupportedOperationException();
		}
  
		@Override
		EntrySet createEntrySet() {
			return new EntrySet(false);
		}
	}
  
	/**
	 * Empty {@link org.apache.lucene.analysis.util.CharArrayMap.UnmodifiableCharArrayMap} optimized for speed.
	 * Contains checks will always return <code>false</code> or throw
	 * NPE if necessary.
	 */
	private static final class EmptyCharArrayMap<V> extends UnmodifiableCharArrayMap<V> {
		EmptyCharArrayMap() {
			super(new CharArrayMap<V>(0, false));
		}
    
		@Override
		public boolean containsKey(char[] text, int off, int len) {
			if (text == null)
				throw new NullPointerException();
			return false;
		}

		@Override
		public boolean containsKey(CharSequence cs) {
			if (cs == null)
				throw new NullPointerException();
			return false;
		}

		@Override
		public boolean containsKey(Object o) {
			if (o == null)
				throw new NullPointerException();
			return false;
		}
    
		@Override
		public V get(char[] text, int off, int len) {
			if (text == null)
				throw new NullPointerException();
			return null;
		}

		@Override
		public V get(CharSequence cs) {
			if (cs == null)
				throw new NullPointerException();
			return null;
		}

		@Override
		public V get(Object o) {
			if (o == null)
				throw new NullPointerException();
			return null;
		}
	}
	
}
