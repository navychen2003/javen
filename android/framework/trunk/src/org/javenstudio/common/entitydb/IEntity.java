package org.javenstudio.common.entitydb;

import java.io.InputStream;

import org.javenstudio.common.entitydb.type.StreamType;

public interface IEntity<K extends IIdentity> {

	public ITable<K, ? extends IEntity<K>> getTable(); 
	public void setTable(ITable<K, ? extends IEntity<K>> table); 
	
	public K getIdentity(); 
	public void setIdentity(K id); 
	public void setIdentity(ITable<K, ? extends IEntity<K>> table); 
	
	public void setAsStream(String fieldName, InputStream is); 
	public void setAsStream(String fieldName, String filePath); 
	
	public InputStream getAsStream(String fieldName); 
	public StreamType getAsStreamType(String fieldName); 
	
	public String[] getStreamFieldNames(); 
	public int scanStreams(); 
	public void removeStream(String fieldName); 
	
	public boolean saveStreams(); 
	public boolean saveStreams(boolean notify); 
	public boolean saveStreams(IEntity<K> entity); 
	public boolean saveStreams(IEntity<K> entity, boolean notify); 
	
	public Object getAsObject(String fieldName); 
	public Object getAsObject(String fieldName, Object defaultValue); 
	
	public String getAsString(String fieldName); 
	public String getAsString(String fieldName, String defaultValue); 
	
	public short getAsShort(String fieldName); 
	public short getAsShort(String fieldName, short defaultValue); 
	
	public int getAsInt(String fieldName); 
	public int getAsInt(String fieldName, int defaultValue); 
	
	public long getAsLong(String fieldName); 
	public long getAsLong(String fieldName, long defaultValue); 
	
	public float getAsFloat(String fieldName); 
	public float getAsFloat(String fieldName, float defaultValue); 
	
	public double getAsDouble(String fieldName); 
	public double getAsDouble(String fieldName, double defaultValue); 
	
	public char getAsChar(String fieldName); 
	public char getAsChar(String fieldName, char defaultValue); 
	
	public byte getAsByte(String fieldName); 
	public byte getAsByte(String fieldName, byte defaultValue); 
	
	public void updateFrom(IEntity<K> data); 
	
}
