package org.javenstudio.common.indexdb.index.field;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IndexOptions;

/** 
 * Collection of {@link FieldInfo}s (accessible by number or by name).
 * 
 */
public class FieldInfos implements IFieldInfos {
	
	private final SortedMap<Integer,IFieldInfo> mByNumber = new TreeMap<Integer,IFieldInfo>();
	private final HashMap<String,IFieldInfo> mByName = new HashMap<String,IFieldInfo>();
	private final Collection<IFieldInfo> mValues; // for an unmodifiable iterator
  
	private final boolean mHasFreq;
	private final boolean mHasProx;
	private final boolean mHasVectors;
	private final boolean mHasNorms;
	
	public FieldInfos(FieldInfo[] infos) {
		boolean hasVectors = false;
		boolean hasProx = false;
		boolean hasFreq = false;
		boolean hasNorms = false;
    
		for (FieldInfo info : infos) {
			assert !mByNumber.containsKey(info.getNumber());
			mByNumber.put(info.getNumber(), info);
			assert !mByName.containsKey(info.getName());
			mByName.put(info.getName(), info);
      
			hasVectors |= info.hasVectors();
			hasProx |= info.isIndexed() && info.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
			hasFreq |= info.isIndexed() && info.getIndexOptions() != IndexOptions.DOCS_ONLY;
			hasNorms |= info.hasNorms();
		}
    
		mHasVectors = hasVectors;
		mHasProx = hasProx;
		mHasFreq = hasFreq;
		mHasNorms = hasNorms;
		mValues = Collections.unmodifiableCollection(mByNumber.values());
	}
  
	/** Returns true if any fields have freqs */
	@Override
	public boolean hasFreq() {
		return mHasFreq;
	}
  
	/** Returns true if any fields have positions */
	@Override
	public boolean hasProx() {
		return mHasProx;
	}
  
	/**
	 * @return true if at least one field has any vectors
	 */
	@Override
	public boolean hasVectors() {
		return mHasVectors;
	}
  
	/**
	 * @return true if at least one field has any norms
	 */
	@Override
	public boolean hasNorms() {
		return mHasNorms;
	}
  
	/**
	 * @return number of fields
	 */
	@Override
	public int size() {
		assert mByNumber.size() == mByName.size();
		return mByNumber.size();
	}
  
	/**
	 * Returns an iterator over all the fieldinfo objects present,
	 * ordered by ascending field number
	 */
	// TODO: what happens if in fact a different order is used?
	@Override
	public Iterator<IFieldInfo> iterator() {
		return mValues.iterator();
	}

	/**
	 * Return the fieldinfo object referenced by the field name
	 * @return the FieldInfo object or null when the given fieldName
	 * doesn't exist.
	 */
	@Override
	public IFieldInfo getFieldInfo(String fieldName) {
		return mByName.get(fieldName);
	}

	/**
	 * Return the fieldinfo object referenced by the fieldNumber.
	 * @param fieldNumber
	 * @return the FieldInfo object or null when the given fieldNumber
	 * doesn't exist.
	 */
	@Override
	public IFieldInfo getFieldInfo(int fieldNumber) {
		return (fieldNumber >= 0) ? mByNumber.get(fieldNumber) : null;
	}
  
}
