package org.javenstudio.common.indexdb;

import java.util.Map;

public interface IFieldInfo {
	
	/** @return the name of the field */
	public String getName();
	
	/** @return the number of the field */
	public int getNumber();
	
	/** @return IndexOptions for the field, or null if the field is not indexed */
	public IndexOptions getIndexOptions();
	
	/** @return true if this field is indexed. */
	public boolean isIndexed();
	
	/** @return true if any payloads exist for this field. */
	public boolean hasPayloads();
	
	/** @return true if any term vectors exist for this field. */
	public boolean hasVectors();
	
	/** @return true if norms are explicitly omitted for this field */
	public boolean isOmitsNorms();
	
	/** @return true if this field actually has any norms. */
	public boolean hasNorms();
	
	/** Get a codec attribute value, or null if it does not exist */
	public String getAttribute(String key);
	
	/** @return internal codec attributes map. May be null if no mappings exist. */
	public Map<String,String> getAttributes();
	
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
	public String putAttribute(String key, String value);
	
	public void setStoreTermVectors();
	public void setStorePayloads();
	
}
