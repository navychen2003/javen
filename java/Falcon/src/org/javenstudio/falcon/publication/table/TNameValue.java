package org.javenstudio.falcon.publication.table;

import org.javenstudio.falcon.publication.INameValue;
import org.javenstudio.falcon.setting.ValueHelper;

final class TNameValue<T> extends ValueHelper implements INameValue<T> {

	private final TNameType mType;
	private final T mValue;
	
	public TNameValue(TNameType type, T value) {
		if (type == null || value == null) throw new NullPointerException();
		if (!type.getValueClass().isAssignableFrom(value.getClass()))
			throw new IllegalArgumentException("Wrong class: " + value.getClass());
		mType = type;
		mValue = value;
	}
	
	public TNameType getType() { return mType; }
	public String getName() { return mType.getName(); }
	public T getValue() { return mValue; }
	
	public int getInt() { return toInt(getValue()); }
	public long getLong() { return toLong(getValue()); }
	public float getFloat() { return toFloat(getValue()); }
	public boolean getBool() { return toBool(getValue()); }
	public String getString() { return toString(getValue()); }
	public byte[] getBytes() {return toBytes(getValue()); }
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{type=" + mType 
				+ ",value=" + mValue + "}";
	}
	
	static String toString(TNameValue<?>[] values) {
		StringBuilder sbuf = new StringBuilder();
		if (values != null) {
			for (TNameValue<?> val : values) {
				if (val == null) continue;
				if (sbuf.length() > 0) sbuf.append(',');
				sbuf.append(val.toString());
			}
		}
		return sbuf.toString();
	}
	
}
