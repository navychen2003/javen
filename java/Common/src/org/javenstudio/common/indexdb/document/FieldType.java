package org.javenstudio.common.indexdb.document;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.util.NumericType;
import org.javenstudio.common.indexdb.util.NumericUtil;

/**
 * Describes the properties of a field.
 */
public class FieldType implements IField.Type {

	private boolean mIndexed;
	private boolean mStored;
	private boolean mTokenized;
	private boolean mStoreTermVectors;
	private boolean mStoreTermVectorOffsets;
	private boolean mStoreTermVectorPositions;
	private boolean mOmitNorms;
	private IndexOptions mIndexOptions = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
	private int mNumericPrecisionStep = NumericUtil.PRECISION_STEP_DEFAULT;
	private NumericType mNumericType;
	private boolean mFrozen;
	
	public FieldType(FieldType ref) {
		mIndexed = ref.isIndexed();
		mStored = ref.isStored();
		mTokenized = ref.isTokenized();
		mStoreTermVectors = ref.isStoreTermVectors();
		mStoreTermVectorOffsets = ref.isStoreTermVectorOffsets();
		mStoreTermVectorPositions = ref.isStoreTermVectorPositions();
		mOmitNorms = ref.isOmitNorms();
		mIndexOptions = ref.getIndexOptions();
		mNumericType = ref.getNumericType();
		// Do not copy frozen!
	}
  
	public FieldType() {}

	private void checkIfFrozen() {
		if (mFrozen) 
			throw new IllegalStateException("this FieldType is already frozen and cannot be changed");
	}

	/**
	 * Prevents future changes. Note, it is recommended that this is called once
	 * the FieldTypes's properties have been set, to prevent unintentional state
	 * changes.
	 */
	public void freeze() {
		mFrozen = true;
	}
  
	@Override
	public boolean isIndexed() {
		return mIndexed;
	}
  
	public void setIndexed(boolean value) {
		checkIfFrozen();
		mIndexed = value;
	}

	@Override
	public boolean isStored() {
		return mStored;
	}
  
	public void setStored(boolean value) {
		checkIfFrozen();
		mStored = value;
	}

	@Override
	public boolean isTokenized() {
		return mTokenized;
	}
  
	public void setTokenized(boolean value) {
		checkIfFrozen();
		mTokenized = value;
	}

	@Override
	public boolean isStoreTermVectors() {
		return mStoreTermVectors;
	}
  
	public void setStoreTermVectors(boolean value) {
		checkIfFrozen();
		mStoreTermVectors = value;
	}

	@Override
	public boolean isStoreTermVectorOffsets() {
		return mStoreTermVectorOffsets;
	}
  
	public void setStoreTermVectorOffsets(boolean value) {
		checkIfFrozen();
		mStoreTermVectorOffsets = value;
	}

	@Override
	public boolean isStoreTermVectorPositions() {
		return mStoreTermVectorPositions;
	}
  
	public void setStoreTermVectorPositions(boolean value) {
		checkIfFrozen();
		mStoreTermVectorPositions = value;
	}
  
	@Override
	public boolean isOmitNorms() {
		return mOmitNorms;
	}
  
	public void setOmitNorms(boolean value) {
		checkIfFrozen();
		mOmitNorms = value;
	}

	@Override
	public IndexOptions getIndexOptions() {
		return mIndexOptions;
	}
  
	public void setIndexOptions(IndexOptions value) {
		checkIfFrozen();
		mIndexOptions = value;
	}

	public void setNumericType(NumericType type) {
		checkIfFrozen();
		mNumericType = type;
	}

	/** 
	 * NumericDataType; if
	 *  non-null then the field's value will be indexed
	 *  numerically so that {@link NumericRangeQuery} can be
	 *  used at search time. 
	 */
	public NumericType getNumericType() {
		return mNumericType;
	}

	public void setNumericPrecisionStep(int precisionStep) {
		checkIfFrozen();
		if (precisionStep < 1) 
			throw new IllegalArgumentException("precisionStep must be >= 1 (got " + precisionStep + ")");
		
		mNumericPrecisionStep = precisionStep;
	}

	/** Precision step for numeric field. */
	public int getNumericPrecisionStep() {
		return mNumericPrecisionStep;
	}

	/** Prints a Field for human consumption. */
	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder();
		toStringAppend(result, isStored(), "stored");
		
		if (isIndexed()) {
			toStringAppend(result, "indexed");
			toStringAppend(result, isTokenized(), "tokenized");
			toStringAppend(result, isStoreTermVectors(), "termVector");
			toStringAppend(result, isStoreTermVectorOffsets(), "termVectorOffsets");
			toStringAppend(result, isStoreTermVectorPositions(), "termVectorPosition");
			toStringAppend(result, isOmitNorms(), "omitNorms");
			
			if (mIndexOptions != IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) {
				result.append(",indexOptions=");
				result.append(mIndexOptions);
			}
			
			if (mNumericType != null) {
				result.append(",numericType=");
				result.append(mNumericType);
				result.append(",numericPrecisionStep=");
				result.append(mNumericPrecisionStep);
			}
		}
		
		return result.toString();
	}
	
	private StringBuilder toStringAppend(StringBuilder result, String value) { 
		if (result.length() > 0) result.append(",");
		result.append(value);
		return result;
	}
	
	private StringBuilder toStringAppend(StringBuilder result, boolean append, String value) { 
		if (append) toStringAppend(result, value);
		return result;
	}
	
}
