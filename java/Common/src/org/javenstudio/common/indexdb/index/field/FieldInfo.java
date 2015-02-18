package org.javenstudio.common.indexdb.index.field;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IndexOptions;

/**
 *  Access to the Fieldable Info file that describes document fields and whether or
 *  not they are indexed. Each segment has a separate Fieldable Info file. Objects
 *  of this class are thread-safe for multiple readers, but only one thread can
 *  be adding documents at a time, with no other reader or writer threads
 *  accessing this object.
 */
public final class FieldInfo implements IFieldInfo {
	
	private final String mName;
	private final int mNumber;

	private boolean mIndexed;

	// True if any document indexed term vectors
	private boolean mStoreTermVector;

	// omit norms associated with indexed fields  
	private boolean mOmitNorms; 
	
	// whether this field stores payloads together with term positions
	private boolean mStorePayloads; 

	private IndexOptions mIndexOptions;
	private Map<String,String> mAttributes;

	/**
	 * Constructor
	 */
	public FieldInfo(String name, boolean indexed, int number, boolean storeTermVector, 
			boolean omitNorms, boolean storePayloads, IndexOptions indexOptions, 
			Map<String,String> attributes) {
		mName = name;
		mIndexed = indexed;
		mNumber = number;
		if (indexed) {
			mStoreTermVector = storeTermVector;
			mStorePayloads = storePayloads;
			mOmitNorms = omitNorms;
			mIndexOptions = indexOptions;
		} else { 
			// for non-indexed fields, leave defaults
			mStoreTermVector = false;
			mStorePayloads = false;
			mOmitNorms = false;
			mIndexOptions = null;
		}
		mAttributes = attributes;
		assert checkConsistency();
	}

	@Override
	public final String getName() { return mName; }
	
	@Override
	public final int getNumber() { return mNumber; }
  
	private boolean checkConsistency() {
		if (!mIndexed) {
			assert !mStoreTermVector;
			assert !mStorePayloads;
			assert !mOmitNorms;
			assert mIndexOptions == null;
		} else {
			assert mIndexOptions != null;
			//if (mOmitNorms) 
			//	assert mNormType == null;
			
			// Cannot store payloads unless positions are indexed:
			assert mIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0 
					|| !mStorePayloads;
		}

		return true;
	}

	// should only be called by FieldInfos#addOrUpdate
	final void update(boolean indexed, boolean storeTermVector, boolean omitNorms, 
			boolean storePayloads, IndexOptions indexOptions) {
		if (mIndexed != indexed) 
			mIndexed = true;             		// once indexed, always index
		
		if (indexed) { // if updated field data is not for indexing, leave the updates out
			if (mStoreTermVector != storeTermVector) 
				mStoreTermVector = true;    	// once vector, always vector
			
			if (mStorePayloads != storePayloads) 
				mStorePayloads = true;
			
			if (mOmitNorms != omitNorms) {
				mOmitNorms = true; 	// if one require omitNorms at least once, it remains off for life
			}
			if (mIndexOptions != indexOptions) {
				if (mIndexOptions == null) {
					mIndexOptions = indexOptions;
				} else {
					// downgrade
					mIndexOptions = mIndexOptions.compareTo(indexOptions) < 0 ? 
							mIndexOptions : indexOptions;
				}
				if (mIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) < 0) {
					// cannot store payloads if we don't store positions:
					mStorePayloads = false;
				}
			}
		}
		assert checkConsistency();
	}

	/** @return IndexOptions for the field, or null if the field is not indexed */
	@Override
	public IndexOptions getIndexOptions() {
		return mIndexOptions;
	}

	public void setStoreTermVectors() {
		mStoreTermVector = true;
		assert checkConsistency();
	}
  
	public void setStorePayloads() {
		if (mIndexed && mIndexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0) 
			mStorePayloads = true;
		
		assert checkConsistency();
	}

	/**
	 * @return true if norms are explicitly omitted for this field
	 */
	@Override
	public boolean isOmitsNorms() {
		return mOmitNorms;
	}
  
	/**
	 * @return true if this field actually has any norms.
	 */
	@Override
	public boolean hasNorms() {
		return false;
	}
  
	/**
	 * @return true if this field is indexed.
	 */
	@Override
	public boolean isIndexed() {
		return mIndexed;
	}
  
	/**
	 * @return true if any payloads exist for this field.
	 */
	@Override
	public boolean hasPayloads() {
		return mStorePayloads;
	}
  
	/**
	 * @return true if any term vectors exist for this field.
	 */
	@Override
	public boolean hasVectors() {
		return mStoreTermVector;
	}
  
	/**
	 * Get a codec attribute value, or null if it does not exist
	 */
	@Override
	public String getAttribute(String key) {
		if (mAttributes == null) 
			return null;
		else 
			return mAttributes.get(key);
	}
  
	/**
	 * Puts a codec attribute value.
	 * <p>
	 * This is a key-value mapping for the field that the codec can use
	 * to store additional metadata, and will be available to the codec
	 * when reading the segment via {@link #getAttribute(String)}
	 * <p>
	 * If a value already exists for the field, it will be replaced with 
	 * the new value.
	 */
	@Override
	public String putAttribute(String key, String value) {
		if (mAttributes == null) 
			mAttributes = new HashMap<String,String>();
		
		return mAttributes.put(key, value);
	}
  
	/**
	 * @return internal codec attributes map. May be null if no mappings exist.
	 */
	@Override
	public Map<String,String> getAttributes() {
		return mAttributes;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + mName + ",number=" 
				+ mNumber + ",indexed=" + mIndexed + "}";
	}
	
}
