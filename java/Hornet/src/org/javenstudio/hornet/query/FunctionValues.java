package org.javenstudio.hornet.query;

import org.javenstudio.common.indexdb.IExplanation;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.search.Explanation;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.util.MutableValue;
import org.javenstudio.hornet.util.MutableValueFloat;

/**
 * Represents field values as different types.
 * Normally created via a {@link ValueSource} for a particular field and reader.
 *
 * FunctionValues is distinct from ValueSource because
 * there needs to be an object created at query evaluation time that
 * is not referenced by the query itself because:
 * - Query objects should be MT safe
 * - For caching, Query objects are often used as keys... you don't
 *   want the Query carrying around big objects
 */
public abstract class FunctionValues {

	private UnsupportedOperationException newUnsupportedException(String name) { 
		throw new UnsupportedOperationException(getClass().getName() + "." + name + "() not supported");
	}
	
	public byte byteVal(int doc) { throw newUnsupportedException("byteVal"); }
	public short shortVal(int doc) { throw newUnsupportedException("shortVal"); }

	public float floatVal(int doc) { throw newUnsupportedException("floatVal"); }
	public int intVal(int doc) { throw newUnsupportedException("intVal"); }
	public long longVal(int doc) { throw newUnsupportedException("longVal"); }
	public double doubleVal(int doc) { throw newUnsupportedException("doubleVal"); }
	
	// TODO: should we make a termVal, returns BytesRef?
	public String stringVal(int doc) { throw newUnsupportedException("stringVal"); }
	
	// For Functions that can work with multiple values from the same document. 
	// This does not apply to all functions
	public void byteVal(int doc, byte[] vals) { throw newUnsupportedException("byteVal"); }
	public void shortVal(int doc, short[] vals) { throw newUnsupportedException("shortVal"); }

	public void floatVal(int doc, float[] vals) { throw newUnsupportedException("floatVal"); }
	public void intVal(int doc, int[] vals) { throw newUnsupportedException("intVal"); }
	public void longVal(int doc, long[] vals) { throw newUnsupportedException("longVal"); }
	public void doubleVal(int doc, double[] vals) { throw newUnsupportedException("doubleVal"); }

	// TODO: should we make a termVal, fills BytesRef[]?
	public void stringVal(int doc, String[] vals) { throw newUnsupportedException("stringVal"); }
	
	public boolean boolVal(int doc) { return intVal(doc) != 0; }
	
	/** 
	 * returns the bytes representation of the string val 
	 * TODO: should this return the indexed raw bytes not? 
	 */
	public boolean bytesVal(int doc, BytesRef target) {
		String s = stringVal(doc);
		if (s == null) {
			target.mLength = 0;
			return false;
		}
		target.copyChars(s);
		return true;
	}
	
	/** Native Java Object representation of the value */
	public Object objectVal(int doc) {
		// most FunctionValues are functions, so by default return a Float()
		return floatVal(doc);
	}

	/** Returns true if there is a value for this document */
	public boolean exists(int doc) {
		return true;
	}
	
	/**
	 * @param doc The doc to retrieve to sort ordinal for
	 * @return the sort ordinal for the specified doc
	 * TODO: Maybe we can just use intVal for this...
	 */
	public int ordVal(int doc) { 
		throw new UnsupportedOperationException(); 
	}

	/**
	 * @return the number of unique sort ordinals this instance has
	 */
	public int numOrd() { 
		throw new UnsupportedOperationException(); 
	}
	
	public abstract String toString(int doc);
	
	public ValueFiller getValueFiller() {
		return new ValueFiller() {
				private final MutableValueFloat mValue = new MutableValueFloat();
	
				@Override
				public MutableValue getValue() {
					return mValue;
				}
	
				@Override
				public void fillValue(int doc) {
					mValue.set(floatVal(doc));
				}
		    };
	}
	
	public IExplanation explain(int doc) {
		return new Explanation(floatVal(doc), toString(doc));
	}
	
	public ValueSourceScorer getScorer(IIndexReader reader) {
		return new ValueSourceScorer(reader, this);
	}
	
	// A RangeValueSource can't easily be a ValueSource that takes another ValueSource
	// because it needs different behavior depending on the type of fields.  There is also
	// a setup cost - parsing and normalizing params, and doing a binary search on the StringIndex.
	// TODO: change "reader" to AtomicReaderContext
	public ValueSourceScorer getRangeScorer(IIndexReader reader, 
			String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
		float lower;
		float upper;

		if (lowerVal == null) 
			lower = Float.NEGATIVE_INFINITY;
		else 
			lower = Float.parseFloat(lowerVal);
		
		if (upperVal == null) 
			upper = Float.POSITIVE_INFINITY;
		else 
			upper = Float.parseFloat(upperVal);
		
		final float l = lower;
		final float u = upper;

		if (includeLower && includeUpper) {
			return new ValueSourceScorer(reader, this) {
					@Override
					public boolean matchesValue(int doc) {
						float docVal = floatVal(doc);
						return docVal >= l && docVal <= u;
					}
				};
			
		} else if (includeLower && !includeUpper) {
			return new ValueSourceScorer(reader, this) {
					@Override
					public boolean matchesValue(int doc) {
						float docVal = floatVal(doc);
						return docVal >= l && docVal < u;
					}
				};
				
		} else if (!includeLower && includeUpper) {
			return new ValueSourceScorer(reader, this) {
					@Override
					public boolean matchesValue(int doc) {
						float docVal = floatVal(doc);
						return docVal > l && docVal <= u;
					}
				};
				
		} else {
			return new ValueSourceScorer(reader, this) {
					@Override
					public boolean matchesValue(int doc) {
						float docVal = floatVal(doc);
						return docVal > l && docVal < u;
					}
				};
		}
	}
	
}
