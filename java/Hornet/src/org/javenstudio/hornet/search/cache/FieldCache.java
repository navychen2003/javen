package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDocTermOrds;
import org.javenstudio.common.indexdb.IDocTerms;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.NumericUtil;

/**
 * Expert: Maintains caches of term values.
 *
 * @see org.apache.lucene.util.FieldCacheSanityChecker
 */
public abstract class FieldCache {

	public static final class CreationPlaceholder {
		Object mValue;
	}

	/**
	 * Hack: When thrown from a Parser (NUMERIC_UTILS_* ones), this stops
	 * processing terms and returns the current FieldCache
	 * array.
	 */
	public static final class StopFillCacheException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	/** Expert: The cache used internally by sorting and range query classes. */
	public static FieldCache DEFAULT = new FieldCacheImpl();

	/** The default parser for byte values, which are encoded by {@link Byte#toString(byte)} */
	public static final ISortField.ByteParser DEFAULT_BYTE_PARSER = new ISortField.ByteParser() {
			public byte parseByte(BytesRef term) {
				// TODO: would be far better to directly parse from
				// UTF8 bytes... but really users should use
				// IntField, instead, which already decodes
				// directly from byte[]
				return Byte.parseByte(term.utf8ToString());
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".DEFAULT_BYTE_PARSER"; 
			}
		};

	/** The default parser for short values, which are encoded by {@link Short#toString(short)} */
	public static final ISortField.ShortParser DEFAULT_SHORT_PARSER = new ISortField.ShortParser() {
			public short parseShort(BytesRef term) {
				// TODO: would be far better to directly parse from
				// UTF8 bytes... but really users should use
				// IntField, instead, which already decodes
				// directly from byte[]
				return Short.parseShort(term.utf8ToString());
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".DEFAULT_SHORT_PARSER"; 
			}
		};

	/** The default parser for int values, which are encoded by {@link Integer#toString(int)} */
	public static final ISortField.IntParser DEFAULT_INT_PARSER = new ISortField.IntParser() {
			public int parseInt(BytesRef term) {
				// TODO: would be far better to directly parse from
				// UTF8 bytes... but really users should use
				// IntField, instead, which already decodes
				// directly from byte[]
				return Integer.parseInt(term.utf8ToString());
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".DEFAULT_INT_PARSER"; 
			}
		};

	/** The default parser for float values, which are encoded by {@link Float#toString(float)} */
	public static final ISortField.FloatParser DEFAULT_FLOAT_PARSER = new ISortField.FloatParser() {
			public float parseFloat(BytesRef term) {
				// TODO: would be far better to directly parse from
				// UTF8 bytes... but really users should use
				// FloatField, instead, which already decodes
				// directly from byte[]
				return Float.parseFloat(term.utf8ToString());
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".DEFAULT_FLOAT_PARSER"; 
			}
		};

	/** The default parser for long values, which are encoded by {@link Long#toString(long)} */
	public static final ISortField.LongParser DEFAULT_LONG_PARSER = new ISortField.LongParser() {
			public long parseLong(BytesRef term) {
				// TODO: would be far better to directly parse from
				// UTF8 bytes... but really users should use
				// LongField, instead, which already decodes
				// directly from byte[]
				return Long.parseLong(term.utf8ToString());
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".DEFAULT_LONG_PARSER"; 
			}
		};

	/** The default parser for double values, which are encoded by {@link Double#toString(double)} */
	public static final ISortField.DoubleParser DEFAULT_DOUBLE_PARSER = new ISortField.DoubleParser() {
			public double parseDouble(BytesRef term) {
				// TODO: would be far better to directly parse from
				// UTF8 bytes... but really users should use
				// DoubleField, instead, which already decodes
				// directly from byte[]
				return Double.parseDouble(term.utf8ToString());
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".DEFAULT_DOUBLE_PARSER"; 
			}
		};

	/**
	 * A parser instance for int values encoded by {@link NumericUtil}, e.g. when indexed
	 * via {@link IntField}/{@link NumericTokenStream}.
	 */
	public static final ISortField.IntParser NUMERIC_UTILS_INT_PARSER = new ISortField.IntParser(){
			public int parseInt(BytesRef term) {
				if (NumericUtil.getPrefixCodedIntShift(term) > 0)
					throw new StopFillCacheException();
				return NumericUtil.prefixCodedToInt(term);
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".NUMERIC_UTILS_INT_PARSER"; 
			}
		};

	/**
	 * A parser instance for float values encoded with {@link NumericUtil}, e.g. when indexed
	 * via {@link FloatField}/{@link NumericTokenStream}.
	 */
	public static final ISortField.FloatParser NUMERIC_UTILS_FLOAT_PARSER = new ISortField.FloatParser(){
			public float parseFloat(BytesRef term) {
				if (NumericUtil.getPrefixCodedIntShift(term) > 0)
					throw new StopFillCacheException();
				return NumericUtil.sortableIntToFloat(NumericUtil.prefixCodedToInt(term));
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".NUMERIC_UTILS_FLOAT_PARSER"; 
			}
		};

	/**
	 * A parser instance for long values encoded by {@link NumericUtil}, e.g. when indexed
	 * via {@link LongField}/{@link NumericTokenStream}.
	 */
	public static final ISortField.LongParser NUMERIC_UTILS_LONG_PARSER = new ISortField.LongParser(){
			public long parseLong(BytesRef term) {
				if (NumericUtil.getPrefixCodedLongShift(term) > 0)
					throw new StopFillCacheException();
				return NumericUtil.prefixCodedToLong(term);
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".NUMERIC_UTILS_LONG_PARSER"; 
			}
		};

	/**
	 * A parser instance for double values encoded with {@link NumericUtil}, e.g. when indexed
	 * via {@link DoubleField}/{@link NumericTokenStream}.
	 */
	public static final ISortField.DoubleParser NUMERIC_UTILS_DOUBLE_PARSER = new ISortField.DoubleParser(){
			public double parseDouble(BytesRef term) {
				if (NumericUtil.getPrefixCodedLongShift(term) > 0)
					throw new StopFillCacheException();
				return NumericUtil.sortableLongToDouble(NumericUtil.prefixCodedToLong(term));
			}
			@Override
			public String toString() { 
				return FieldCache.class.getName()+".NUMERIC_UTILS_DOUBLE_PARSER"; 
			}
		};
  
 
	/** 
	 * Checks the internal cache for an appropriate entry, and if none is found,
	 * reads the terms in <code>field</code> and returns a bit set at the size of
	 * <code>reader.maxDoc()</code>, with turned on bits for each docid that 
	 * does have a value for this field.
	 */
	public abstract Bits getDocsWithField(IAtomicReader reader, String field) 
			throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if none is
	 * found, reads the terms in <code>field</code> as a single byte and returns an array
	 * of size <code>reader.maxDoc()</code> of the value each document
	 * has in the given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the single byte values.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract byte[] getBytes(IAtomicReader reader, String field, 
			boolean setDocsWithField) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if none is found,
	 * reads the terms in <code>field</code> as bytes and returns an array of
	 * size <code>reader.maxDoc()</code> of the value each document has in the
	 * given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the bytes.
	 * @param parser  Computes byte for string values.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract byte[] getBytes(IAtomicReader reader, String field, 
			ISortField.ByteParser parser, boolean setDocsWithField) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if none is
	 * found, reads the terms in <code>field</code> as shorts and returns an array
	 * of size <code>reader.maxDoc()</code> of the value each document
	 * has in the given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the shorts.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract short[] getShorts(IAtomicReader reader, String field, 
			boolean setDocsWithField) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if none is found,
	 * reads the terms in <code>field</code> as shorts and returns an array of
	 * size <code>reader.maxDoc()</code> of the value each document has in the
	 * given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the shorts.
	 * @param parser  Computes short for string values.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract short[] getShorts(IAtomicReader reader, String field, 
			ISortField.ShortParser parser, boolean setDocsWithField) throws IOException;
  
	/** 
	 * Checks the internal cache for an appropriate entry, and if none is
	 * found, reads the terms in <code>field</code> as integers and returns an array
	 * of size <code>reader.maxDoc()</code> of the value each document
	 * has in the given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the integers.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract int[] getInts(IAtomicReader reader, String field, 
			boolean setDocsWithField) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if none is found,
	 * reads the terms in <code>field</code> as integers and returns an array of
	 * size <code>reader.maxDoc()</code> of the value each document has in the
	 * given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the integers.
	 * @param parser  Computes integer for string values.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract int[] getInts(IAtomicReader reader, String field, 
			ISortField.IntParser parser, boolean setDocsWithField) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if
	 * none is found, reads the terms in <code>field</code> as floats and returns an array
	 * of size <code>reader.maxDoc()</code> of the value each document
	 * has in the given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the floats.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract float[] getFloats(IAtomicReader reader, String field, 
			boolean setDocsWithField) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if
	 * none is found, reads the terms in <code>field</code> as floats and returns an array
	 * of size <code>reader.maxDoc()</code> of the value each document
	 * has in the given field.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the floats.
	 * @param parser  Computes float for string values.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract float[] getFloats(IAtomicReader reader, String field,
			ISortField.FloatParser parser, boolean setDocsWithField) throws IOException;

	/**
	 * Checks the internal cache for an appropriate entry, and if none is
	 * found, reads the terms in <code>field</code> as longs and returns an array
	 * of size <code>reader.maxDoc()</code> of the value each document
	 * has in the given field.
	 *
	 * @param reader Used to get field values.
	 * @param field  Which field contains the longs.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws java.io.IOException If any error occurs.
	 */
	public abstract long[] getLongs(IAtomicReader reader, String field, 
			boolean setDocsWithField) throws IOException;

	/**
	 * Checks the internal cache for an appropriate entry, and if none is found,
	 * reads the terms in <code>field</code> as longs and returns an array of
	 * size <code>reader.maxDoc()</code> of the value each document has in the
	 * given field.
	 *
	 * @param reader Used to get field values.
	 * @param field  Which field contains the longs.
	 * @param parser Computes integer for string values.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException If any error occurs.
	 */
	public abstract long[] getLongs(IAtomicReader reader, String field, 
			ISortField.LongParser parser, boolean setDocsWithField) throws IOException;

	/**
	 * Checks the internal cache for an appropriate entry, and if none is
	 * found, reads the terms in <code>field</code> as integers and returns an array
	 * of size <code>reader.maxDoc()</code> of the value each document
	 * has in the given field.
	 *
	 * @param reader Used to get field values.
	 * @param field  Which field contains the doubles.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException If any error occurs.
	 */
	public abstract double[] getDoubles(IAtomicReader reader, String field, 
			boolean setDocsWithField) throws IOException;

	/**
	 * Checks the internal cache for an appropriate entry, and if none is found,
	 * reads the terms in <code>field</code> as doubles and returns an array of
	 * size <code>reader.maxDoc()</code> of the value each document has in the
	 * given field.
	 *
	 * @param reader Used to get field values.
	 * @param field  Which field contains the doubles.
	 * @param parser Computes integer for string values.
	 * @param setDocsWithField  If true then {@link #getDocsWithField} will
	 *        also be computed and stored in the FieldCache.
	 * @return The values in the given field for each document.
	 * @throws IOException If any error occurs.
	 */
	public abstract double[] getDoubles(IAtomicReader reader, String field, 
			ISortField.DoubleParser parser, boolean setDocsWithField) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if none
	 * is found, reads the term values in <code>field</code>
	 * and returns a {@link DocTerms} instance, providing a
	 * method to retrieve the term (as a BytesRef) per document.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the strings.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract IDocTerms getTerms(IAtomicReader reader, String field)
			throws IOException;

	/** 
	 * Expert: just like {@link #getTerms(IAtomicReader,String)},
	 *  but you can specify whether more RAM should be consumed in exchange for
	 *  faster lookups (default is "true").  Note that the
	 *  first call for a given reader and field "wins",
	 *  subsequent calls will share the same cache entry. 
	 */
	public abstract IDocTerms getTerms(IAtomicReader reader, String field, 
			float acceptableOverheadRatio) throws IOException;

	/** 
	 * Checks the internal cache for an appropriate entry, and if none
	 * is found, reads the term values in <code>field</code>
	 * and returns a {@link DocTerms} instance, providing a
	 * method to retrieve the term (as a BytesRef) per document.
	 * @param reader  Used to get field values.
	 * @param field   Which field contains the strings.
	 * @return The values in the given field for each document.
	 * @throws IOException  If any error occurs.
	 */
	public abstract IDocTermsIndex getTermsIndex(IAtomicReader reader, String field)
			throws IOException;

	/** 
	 * Expert: just like {@link
	 *  #getTermsIndex(IAtomicReader,String)}, but you can specify
	 *  whether more RAM should be consumed in exchange for
	 *  faster lookups (default is "true").  Note that the
	 *  first call for a given reader and field "wins",
	 *  subsequent calls will share the same cache entry. 
	 */
	public abstract IDocTermsIndex getTermsIndex(IAtomicReader reader, String field, 
			float acceptableOverheadRatio) throws IOException;

	/**
	 * Checks the internal cache for an appropriate entry, and if none is found, reads the term values
	 * in <code>field</code> and returns a {@link DocTermOrds} instance, providing a method to retrieve
	 * the terms (as ords) per document.
	 *
	 * @param reader  Used to build a {@link DocTermOrds} instance
	 * @param field   Which field contains the strings.
	 * @return a {@link DocTermOrds} instance
	 * @throws IOException  If any error occurs.
	 */
	public abstract IDocTermOrds getDocTermOrds(IAtomicReader reader, String field) 
			throws IOException;

	/**
	 * EXPERT: Generates an array of CacheEntry objects representing all items 
	 * currently in the FieldCache.
	 * <p>
	 * NOTE: These CacheEntry objects maintain a strong reference to the 
	 * Cached Values.  Maintaining references to a CacheEntry the AtomicIndexReader 
	 * associated with it has garbage collected will prevent the Value itself
	 * from being garbage collected when the Cache drops the WeakReference.
	 * </p>
	 */
	public abstract CacheEntry[] getCacheEntries();

	/**
	 * <p>
	 * EXPERT: Instructs the FieldCache to forcibly expunge all entries 
	 * from the underlying caches.  This is intended only to be used for 
	 * test methods as a way to ensure a known base state of the Cache 
	 * (with out needing to rely on GC to free WeakReferences).  
	 * It should not be relied on for "Cache maintenance" in general 
	 * application code.
	 * </p>
	 */
	public abstract void purgeAllCaches();

	/**
	 * Expert: drops all cache entries associated with this
	 * reader.  NOTE: this reader must precisely match the
	 * reader that the cache entry is keyed on. If you pass a
	 * top-level reader, it usually will have no effect as
   	 * Lucene now caches at the segment reader level.
   	 */
	public abstract void purge(IAtomicReader r);
  
}
