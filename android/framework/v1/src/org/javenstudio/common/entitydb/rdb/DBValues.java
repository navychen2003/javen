package org.javenstudio.common.entitydb.rdb;

import java.util.Map;
import java.util.Set;

public interface DBValues {

	public void put(String key, String value);
	public void put(String key, Byte value);
	public void put(String key, Short value);
	public void put(String key, Integer value);
	public void put(String key, Long value);
	public void put(String key, Float value);
	public void put(String key, Double value);
	public void put(String key, Boolean value);
	public void put(String key, byte[] value);
	public void putNull(String key);
	
	public String getAsString(String key);
	public Long getAsLong(String key);
	public Integer getAsInteger(String key);
	public Short getAsShort(String key);
	public Byte getAsByte(String key);
	public Double getAsDouble(String key);
	public Float getAsFloat(String key);
	public Boolean getAsBoolean(String key);
	public byte[] getAsByteArray(String key);
	
	public boolean containsKey(String key);
	public void remove(String key);
	public void clear();
	public int size();
	
	public Set<Map.Entry<String, Object>> valueSet();
	
}
