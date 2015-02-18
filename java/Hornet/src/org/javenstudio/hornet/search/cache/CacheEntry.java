package org.javenstudio.hornet.search.cache;

import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * EXPERT: A unique Identifier/Description for each item in the FieldCache. 
 * Can be useful for logging/debugging.
 */
public abstract class CacheEntry {
	
	public abstract Object getReaderKey();
	public abstract String getFieldName();
	public abstract Class<?> getCacheType();
	public abstract Object getCustom();
	public abstract Object getValue();
  
	private String mSize = null;
  
	final void setEstimatedSize(String size) {
		mSize = size;
	}

	/** 
	 * Computes (and stores) the estimated size of the cache Value 
	 * @see #getEstimatedSize
	 */
	public void estimateSize() {
		long size = JvmUtil.sizeOf(getValue());
		setEstimatedSize(StringHelper.toHumanReadableUnits(size));
	}

	/**
	 * The most recently estimated size of the value, null unless 
	 * estimateSize has been called.
	 */
	public final String getEstimatedSize() {
		return mSize;
	}
  
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append("'").append(getReaderKey()).append("'=>");
		b.append("'").append(getFieldName()).append("',");
		b.append(getCacheType()).append(",").append(getCustom());
		b.append("=>").append(getValue().getClass().getName()).append("#");
		b.append(System.identityHashCode(getValue()));
    
		String s = getEstimatedSize();
		if(null != s) 
			b.append(" (size =~ ").append(s).append(')');

		return b.toString();
	}

}
