package org.javenstudio.common.indexdb.index.consumer;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.INorm;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Stores the normalization value computed in
 * {@link Similarity#computeNorm(FieldInvertState, Norm)} per field.
 * Normalization values must be consistent within a single field, different
 * value types are not permitted within a single field. All values set must be
 * fixed size values ie. all values passed to {@link Norm#setBytes(BytesRef)}
 * must have the same length per field.
 */
public final class Norm implements INorm {
	private IField mField;
	private BytesRef mSpare;
  
	/**
	 * Returns the {@link IndexableField} representation for this norm
	 */
	public IField getField() {
		return mField;
	}
  
	/**
	 * Returns a spare {@link BytesRef} 
	 */
	public BytesRef getSpare() {
		if (mSpare == null) 
			mSpare = new BytesRef();
		
		return mSpare;
	}

	/**
	 * Sets a float norm value
	 */
	public void setFloat(float norm) {
		//setType(Type.FLOAT_32);
		//this.field.setFloatValue(norm);
	}

	/**
	 * Sets a double norm value
	 */
	public void setDouble(double norm) {
		//setType(Type.FLOAT_64);
		//this.field.setDoubleValue(norm);
	}

	/**
	 * Sets a short norm value
	 */
	public void setShort(short norm) {
		//setType(Type.FIXED_INTS_16);
		//this.field.setShortValue(norm);
	}

	/**
	 * Sets a int norm value
	 */
	public void setInt(int norm) {
		//setType(Type.FIXED_INTS_32);
		//this.field.setIntValue(norm);
	}

	/**
	 * Sets a long norm value
	 */
	public void setLong(long norm) {
		//setType(Type.FIXED_INTS_64);
		//this.field.setLongValue(norm);
	}

	/**
	 * Sets a byte norm value
	 */
	public void setByte(byte norm) {
		//setType(Type.FIXED_INTS_8);
		//this.field.setByteValue(norm);
	}

	/**
	 * Sets a fixed byte array norm value
	 */
	public void setBytes(BytesRef norm) {
		//setType(Type.BYTES_FIXED_STRAIGHT);
		//this.field.setBytesValue(norm);
	}

}