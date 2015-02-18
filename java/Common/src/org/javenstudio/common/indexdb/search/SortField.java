package org.javenstudio.common.indexdb.search;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IFieldComparatorSource;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.StringHelper;

// TODO(simonw) -- for cleaner transition, maybe we should make
// a new SortField that subclasses this one and always uses
// index values?

/**
 * Stores information about how to sort documents by terms in an individual
 * field.  Fields must be indexed in order to sort by them.
 *
 * @see Sort
 */
public abstract class SortField implements ISortField {

	protected String mField;
	protected Type mType;  // defaults to determining type dynamically
	protected boolean mReverse = false;  // defaults to natural order
	protected ISortField.Parser mParser;

	// Used for CUSTOM sort
	protected FieldComparatorSource mComparatorSource = null;

	// Used for 'sortMissingFirst/Last'
	protected Object mMissingValue = null;

	protected Comparator<BytesRef> mBytesComparator = 
  			BytesRef.getUTF8SortedAsUnicodeComparator();
  	
  	protected boolean mUseIndexValues = false;
	
	/** 
	 * Creates a sort by terms in the given field with the type of term
	 * values explicitly given.
	 * @param field  Name of field to sort by.  Can be <code>null</code> if
	 *               <code>type</code> is SCORE or DOC.
	 * @param type   Type of values in the terms.
	 */
	public SortField(String field, Type type) {
		initFieldType(field, type);
	}

	/** 
	 * Creates a sort, possibly in reverse, by terms in the given field with the
	 * type of term values explicitly given.
	 * @param field  Name of field to sort by.  Can be <code>null</code> if
	 *               <code>type</code> is SCORE or DOC.
	 * @param type   Type of values in the terms.
	 * @param reverse True if natural order should be reversed.
	 */
	public SortField(String field, Type type, boolean reverse) {
		initFieldType(field, type);
		mReverse = reverse;
	}

	/** 
	 * Creates a sort by terms in the given field, parsed
	 * to numeric values using a custom {@link FieldCache.Parser}.
	 * @param field  Name of field to sort by.  Must not be null.
	 * @param parser Instance of a {@link FieldCache.Parser},
	 *  which must subclass one of the existing numeric
	 *  parsers from {@link FieldCache}. Sort type is inferred
	 *  by testing which numeric parser the parser subclasses.
	 * @throws IllegalArgumentException if the parser fails to
	 *  subclass an existing numeric parser, or field is null
	 */
	public SortField(String field, ISortField.Parser parser) {
		this(field, parser, false);
	}

	/** 
	 * Creates a sort, possibly in reverse, by terms in the given field, parsed
	 * to numeric values using a custom {@link FieldCache.Parser}.
	 * @param field  Name of field to sort by.  Must not be null.
	 * @param parser Instance of a {@link FieldCache.Parser},
	 *  which must subclass one of the existing numeric
	 *  parsers from {@link FieldCache}. Sort type is inferred
	 *  by testing which numeric parser the parser subclasses.
	 * @param reverse True if natural order should be reversed.
	 * @throws IllegalArgumentException if the parser fails to
	 *  subclass an existing numeric parser, or field is null
	 */
	public SortField(String field, ISortField.Parser parser, boolean reverse) {
		if (parser instanceof ISortField.IntParser) initFieldType(field, Type.INT);
		else if (parser instanceof ISortField.FloatParser) initFieldType(field, Type.FLOAT);
		else if (parser instanceof ISortField.ShortParser) initFieldType(field, Type.SHORT);
		else if (parser instanceof ISortField.ByteParser) initFieldType(field, Type.BYTE);
		else if (parser instanceof ISortField.LongParser) initFieldType(field, Type.LONG);
		else if (parser instanceof ISortField.DoubleParser) initFieldType(field, Type.DOUBLE);
		else {
			throw new IllegalArgumentException("Parser instance does not subclass existing " + 
					"numeric parser from FieldCache (got " + parser + ")");
		}

		mReverse = reverse;
		mParser = parser;
	}
  
	public SortField setMissingValue(Object missingValue) {
		if (mType != Type.BYTE && mType != Type.SHORT && mType != Type.INT && 
			mType != Type.FLOAT && mType != Type.LONG && mType != Type.DOUBLE) {
			throw new IllegalArgumentException( "Missing value only works for numeric types" );
		}
		mMissingValue = missingValue;
		return this;
	}

	/** 
	 * Creates a sort with a custom comparison function.
	 * @param field Name of field to sort by; cannot be <code>null</code>.
	 * @param comparator Returns a comparator for sorting hits.
	 */
	public SortField(String field, FieldComparatorSource comparator) {
		initFieldType(field, Type.CUSTOM);
		mComparatorSource = comparator;
	}

	/** 
	 * Creates a sort, possibly in reverse, with a custom comparison function.
	 * @param field Name of field to sort by; cannot be <code>null</code>.
	 * @param comparator Returns a comparator for sorting hits.
	 * @param reverse True if natural order should be reversed.
	 */
	public SortField(String field, FieldComparatorSource comparator, boolean reverse) {
		initFieldType(field, Type.CUSTOM);
		mReverse = reverse;
		mComparatorSource = comparator;
	}

	// Sets field & type, and ensures field is not NULL unless
	// type is SCORE or DOC
	private void initFieldType(String field, Type type) {
		mType = type;
		if (field == null) {
			if (mType != Type.SCORE && mType != Type.DOC) 
				throw new IllegalArgumentException("field can only be null when type is SCORE or DOC");
		} else 
			mField = field;
	}

	/** 
	 * Returns the name of the field.  Could return <code>null</code>
	 * if the sort is by SCORE or DOC.
	 * @return Name of field, possibly <code>null</code>.
	 */
	@Override
	public String getField() {
		return mField;
	}

	/** 
	 * Returns the type of contents in the field.
	 * @return One of the constants SCORE, DOC, STRING, INT or FLOAT.
	 */
	@Override
	public Type getType() {
		return mType;
	}

	/** 
	 * Returns the instance of a {@link FieldCache} parser that fits to the given sort type.
	 * May return <code>null</code> if no parser was specified. Sorting is using the default parser then.
	 * @return An instance of a {@link FieldCache} parser, or <code>null</code>.
	 */
	public ISortField.Parser getParser() {
		return mParser;
	}

	/** 
	 * Returns whether the sort should be reversed.
	 * @return  True if natural order should be reversed.
	 */
	@Override
	public boolean getReverse() {
		return mReverse;
	}

	/** 
	 * Returns the {@link FieldComparatorSource} used for
	 * custom sorting
	 */
	@Override
	public IFieldComparatorSource getComparatorSource() {
		return mComparatorSource;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		String dv = mUseIndexValues ? " [dv]" : "";
		switch (mType) {
		case SCORE:
			buffer.append("<score>");
			break;

		case DOC:
			buffer.append("<doc>");
			break;

		case STRING:
			buffer.append("<string" + dv + ": \"").append(mField).append("\">");
			break;

		case STRING_VAL:
			buffer.append("<string_val" + dv + ": \"").append(mField).append("\">");
			break;

		case BYTE:
			buffer.append("<byte: \"").append(mField).append("\">");
			break;

		case SHORT:
			buffer.append("<short: \"").append(mField).append("\">");
			break;

		case INT:
			buffer.append("<int" + dv + ": \"").append(mField).append("\">");
			break;

		case LONG:
			buffer.append("<long: \"").append(mField).append("\">");
			break;

		case FLOAT:
			buffer.append("<float" + dv + ": \"").append(mField).append("\">");
			break;

		case DOUBLE:
			buffer.append("<double" + dv + ": \"").append(mField).append("\">");
			break;

		case CUSTOM:
			buffer.append("<custom:\"").append(mField).append("\": ").append(mComparatorSource).append('>');
			break;
      
		case REWRITEABLE:
			buffer.append("<rewriteable: \"").append(mField).append("\">");
			break;

		default:
			buffer.append("<???: \"").append(mField).append("\">");
			break;
		}

		if (mReverse) buffer.append('!');

		return buffer.toString();
	}

	/** 
	 * Returns true if <code>o</code> is equal to this.  If a
	 *  {@link FieldComparatorSource} or {@link
	 *  FieldCache.Parser} was provided, it must properly
	 *  implement equals (unless a singleton is always used). 
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SortField)) return false;
		final SortField other = (SortField)o;
		return (
				StringHelper.equals(other.mField, this.mField)
				&& other.mType == this.mType
				&& other.mReverse == this.mReverse
				&& (other.mComparatorSource == null ? this.mComparatorSource == null : 
					other.mComparatorSource.equals(this.mComparatorSource))
			);
	}

	/** 
	 * Returns true if <code>o</code> is equal to this.  If a
	 *  {@link FieldComparatorSource} or {@link
	 *  FieldCache.Parser} was provided, it must properly
	 *  implement hashCode (unless a singleton is always
	 *  used). 
	 */
	@Override
	public int hashCode() {
		int hash = mType.hashCode() ^ 0x346565dd + Boolean.valueOf(mReverse).hashCode() ^ 0xaf5998bb;
		if (mField != null) hash += mField.hashCode()^0xff5685dd;
		if (mComparatorSource != null) hash += mComparatorSource.hashCode();
		return hash;
	}

	public void setUseIndexValues(boolean b) {
		mUseIndexValues = b;
	}

	public boolean getUseIndexValues() {
		return mUseIndexValues;
	}

	public void setBytesComparator(Comparator<BytesRef> b) {
		mBytesComparator = b;
	}

	public Comparator<BytesRef> getBytesComparator() {
		return mBytesComparator;
	}

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
	@Override
	public abstract IFieldComparator<?> getComparator(final int numHits, 
			final int sortPos) throws IOException;

	/**
	 * Rewrites this SortField, returning a new SortField if a change is made.
	 * Subclasses should override this define their rewriting behavior when this
	 * SortField is of type {@link SortField.Type#REWRITEABLE}
	 *
	 * @param searcher IndexSearcher to use during rewriting
	 * @return New rewritten SortField, or {@code this} if nothing has changed.
	 * @throws IOException Can be thrown by the rewriting
	 */
	@Override
	public ISortField rewrite(ISearcher searcher) throws IOException {
		return this;
	}
	
}
