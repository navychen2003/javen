package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Stores the normalization value computed in
 * {@link Similarity#computeNorm(FieldInvertState, Norm)} per field.
 * Normalization values must be consistent within a single field, different
 * value types are not permitted within a single field. All values set must be
 * fixed size values ie. all values passed to {@link Norm#setBytes(BytesRef)}
 * must have the same length per field.
 */
public interface INorm {

	/**
	 * Returns the {@link IndexableField} representation for this norm
	 */
	public IField getField();

	/**
	 * Returns a spare {@link BytesRef} 
	 */
	public BytesRef getSpare();
  
	/**
	 * Sets a float norm value
	 */
	public void setFloat(float norm);
  
	/**
	 * Sets a double norm value
	 */
	public void setDouble(double norm);
  
	/**
	 * Sets a short norm value
	 */
	public void setShort(short norm);
  
	/**
	 * Sets a int norm value
	 */
	public void setInt(int norm);
  
	/**
	 * Sets a long norm value
	 */
	public void setLong(long norm);
  
	/**
	 * Sets a byte norm value
	 */
	public void setByte(byte norm);

	/**
	 * Sets a fixed byte array norm value
	 */
	public void setBytes(BytesRef norm);
	  
}
