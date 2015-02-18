package org.javenstudio.common.indexdb;

public interface IFieldInfos extends Iterable<IFieldInfo> {

	public static interface Builder { 
		public IFieldInfo addOrUpdate(String name, IField.Type fieldType);
	}
	
	/** @return true if any fields have freqs */
	public boolean hasFreq();
	
	/** @return true if any fields have positions */
	public boolean hasProx();
	
	/** @return true if at least one field has any vectors */
	public boolean hasVectors();
	
	/** @return true if at least one field has any norms */
	public boolean hasNorms();
	
	/** @return number of fields */
	public int size();
	
	/**
	 * Return the fieldinfo object referenced by the field name
	 * @return the FieldInfo object or null when the given fieldName
	 * doesn't exist.
	 */  
	public IFieldInfo getFieldInfo(String fieldName);
	
	/**
	 * Return the fieldinfo object referenced by the fieldNumber.
	 * @param fieldNumber
	 * @return the FieldInfo object or null when the given fieldNumber
	 * doesn't exist.
	 */  
	public IFieldInfo getFieldInfo(int fieldNumber);
	
}
