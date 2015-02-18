package org.javenstudio.common.indexdb;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface ISortField {

	public static enum Type {

		/** Sort by document score (relevance).  Sort values are Float and higher
		 * values are at the front. */
		SCORE,

		/** Sort by document number (index order).  Sort values are Integer and lower
		 * values are at the front. */
		DOC,

		/** Sort using term values as Strings.  Sort values are String and lower
		 * values are at the front. */
		STRING,

		/** Sort using term values as encoded Integers.  Sort values are Integer and
		 * lower values are at the front. */
		INT,

		/** Sort using term values as encoded Floats.  Sort values are Float and
		 * lower values are at the front. */
		FLOAT,

		/** Sort using term values as encoded Longs.  Sort values are Long and
		 * lower values are at the front. */
		LONG,

		/** Sort using term values as encoded Doubles.  Sort values are Double and
		 * lower values are at the front. */
		DOUBLE,

		/** Sort using term values as encoded Shorts.  Sort values are Short and
		 * lower values are at the front. */
		SHORT,

		/** Sort using a custom Comparator.  Sort values are any Comparable and
		 * sorting is done according to natural order. */
		CUSTOM,

		/** Sort using term values as encoded Bytes.  Sort values are Byte and
		 * lower values are at the front. */
		BYTE,

		/** 
		 * Sort using term values as Strings, but comparing by
		 * value (using String.compareTo) for all comparisons.
		 * This is typically slower than {@link #STRING}, which
		 * uses ordinals to do the sorting. 
		 */
		STRING_VAL,

		/** Sort use byte[] index values. */
		BYTES,

		/** Force rewriting of SortField using {@link SortField#rewrite(IndexSearcher)}
		 * before it can be used for sorting */
		REWRITEABLE
	}
	
	/**
	 * Marker interface as super-interface to all parsers. It
	 * is used to specify a custom parser to {@link
	 * SortField#SortField(String, FieldCache.Parser)}.
	 */
	public interface Parser {
	}

	/** 
	 * Interface to parse bytes from document fields.
	 * @see FieldCache#getBytes(IAtomicReader, String, FieldCache.ByteParser, boolean)
	 */
	public interface ByteParser extends Parser {
		/** Return a single Byte representation of this field's value. */
		public byte parseByte(BytesRef term);
	}

	/** 
	 * Interface to parse shorts from document fields.
	 * @see FieldCache#getShorts(IAtomicReader, String, FieldCache.ShortParser, boolean)
	 */
	public interface ShortParser extends Parser {
		/** Return a short representation of this field's value. */
		public short parseShort(BytesRef term);
	}

	/** 
	 * Interface to parse ints from document fields.
	 * @see FieldCache#getInts(IAtomicReader, String, FieldCache.IntParser, boolean)
	 */
	public interface IntParser extends Parser {
		/** Return an integer representation of this field's value. */
		public int parseInt(BytesRef term);
	}

	/** 
	 * Interface to parse floats from document fields.
	 * @see FieldCache#getFloats(IAtomicReader, String, FieldCache.FloatParser, boolean)
	 */
	public interface FloatParser extends Parser {
		/** Return an float representation of this field's value. */
		public float parseFloat(BytesRef term);
	}

	/** 
	 * Interface to parse long from document fields.
	 * @see FieldCache#getLongs(IAtomicReader, String, FieldCache.LongParser, boolean)
	 */
	public interface LongParser extends Parser {
		/** Return an long representation of this field's value. */
		public long parseLong(BytesRef term);
	}

	/** 
	 * Interface to parse doubles from document fields.
	 * @see FieldCache#getDoubles(IAtomicReader, String, FieldCache.DoubleParser, boolean)
	 */
	public interface DoubleParser extends Parser {
		/** Return an long representation of this field's value. */
		public double parseDouble(BytesRef term);
	}
	
	/** 
	 * Returns the name of the field.  Could return <code>null</code>
	 * if the sort is by SCORE or DOC.
	 * @return Name of field, possibly <code>null</code>.
	 */
	public String getField();
	
	/** 
	 * Returns the type of contents in the field.
	 * @return One of the constants SCORE, DOC, STRING, INT or FLOAT.
	 */
	public Type getType();
	
	/** 
	 * Returns whether the sort should be reversed.
	 * @return  True if natural order should be reversed.
	 */
	public boolean getReverse();
	
	/** 
	 * Returns the {@link FieldComparatorSource} used for
	 * custom sorting
	 */
	public IFieldComparatorSource getComparatorSource();
	
	/**
	 * Rewrites this SortField, returning a new SortField if a change is made.
	 * Subclasses should override this define their rewriting behavior when this
	 * SortField is of type {@link SortField.Type#REWRITEABLE}
	 *
	 * @param searcher IndexSearcher to use during rewriting
	 * @return New rewritten SortField, or {@code this} if nothing has changed.
	 * @throws IOException Can be thrown by the rewriting
	 */
	public ISortField rewrite(ISearcher searcher) throws IOException;
	
	/** 
	 * Returns the {@link FieldComparator} to use for
	 * sorting.
	 *
	 * @param numHits number of top hits the queue will store
	 * @param sortPos position of this SortField within {@link
	 *   Sort}.  The comparator is primary if sortPos==0,
	 *   secondary if sortPos==1, etc.  Some comparators can
	 *   optimize themselves when they are the primary sort.
	 * @return {@link FieldComparator} to use when sorting
	 */
	public IFieldComparator<?> getComparator(final int numHits, final int sortPos) 
			throws IOException;
	
}
