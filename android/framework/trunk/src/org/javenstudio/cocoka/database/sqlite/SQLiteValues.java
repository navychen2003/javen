package org.javenstudio.cocoka.database.sqlite;

import java.util.Map;
import java.util.Set;

import android.content.ContentValues;

import org.javenstudio.common.entitydb.rdb.DBValues;

public class SQLiteValues implements DBValues {

	final ContentValues mValues;
	
	public SQLiteValues() { 
		this(new ContentValues());
	}
	
	public SQLiteValues(ContentValues values) { 
		mValues = values;
	}
	
	public void put(String key, String value) { mValues.put(key, value); }
	public void put(String key, Byte value) { mValues.put(key, value); }
	public void put(String key, Short value) { mValues.put(key, value); }
	public void put(String key, Integer value) { mValues.put(key, value); }
	public void put(String key, Long value) { mValues.put(key, value); }
	public void put(String key, Float value) { mValues.put(key, value); }
	public void put(String key, Double value) { mValues.put(key, value); }
	public void put(String key, Boolean value) { mValues.put(key, value); }
	public void put(String key, byte[] value) { mValues.put(key, value); }
	public void putNull(String key) { mValues.putNull(key); }
	
	public String getAsString(String key) { return mValues.getAsString(key); }
	public Long getAsLong(String key) { return mValues.getAsLong(key); }
	public Integer getAsInteger(String key) { return mValues.getAsInteger(key); }
	public Short getAsShort(String key) { return mValues.getAsShort(key); }
	public Byte getAsByte(String key) { return mValues.getAsByte(key); }
	public Double getAsDouble(String key) { return mValues.getAsDouble(key); }
	public Float getAsFloat(String key) { return mValues.getAsFloat(key); }
	public Boolean getAsBoolean(String key) { return mValues.getAsBoolean(key); }
	public byte[] getAsByteArray(String key) { return mValues.getAsByteArray(key); }
	
	public boolean containsKey(String key) { return mValues.containsKey(key); }
	public void remove(String key) { mValues.remove(key); }
	public void clear() { mValues.clear(); }
	public int size() { return mValues.size(); }
	
	public Set<Map.Entry<String, Object>> valueSet() { return mValues.valueSet(); }
	
	@Override
	public String toString() { 
		return (mValues != null) ? mValues.toString() : "SQLiteValues:null";
	}
	
}
