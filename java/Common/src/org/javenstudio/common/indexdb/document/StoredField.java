package org.javenstudio.common.indexdb.document;

import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * A field whose value is stored so that {@link
 *  IndexSearcher#doc} and {@link IndexReader#document} will
 *  return the field and its value. 
 */
public final class StoredField extends AbstractField {

	public final static FieldType TYPE;
	static {
		TYPE = new FieldType();
		TYPE.setStored(true);
		TYPE.freeze();
	}

	public StoredField(String name, byte[] value) {
		super(name, value, TYPE);
	}
  
	public StoredField(String name, byte[] value, int offset, int length) {
		super(name, value, offset, length, TYPE);
	}

	public StoredField(String name, BytesRef value) {
		super(name, value, TYPE);
	}

	public StoredField(String name, String value) {
		super(name, value, TYPE);
	}

	public StoredField(String name, int value) {
		super(name, TYPE, Integer.valueOf(value));
	}

	public StoredField(String name, float value) {
		super(name, TYPE, Float.valueOf(value));
	}

	public StoredField(String name, long value) {
		super(name, TYPE, Long.valueOf(value));
	}

	public StoredField(String name, double value) {
		super(name, TYPE, Double.valueOf(value));
	}
	
}
