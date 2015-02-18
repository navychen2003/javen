package org.javenstudio.common.entitydb.db;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.type.StreamType;

public abstract class AbstractEntity<K extends IIdentity> implements IEntity<K> {

	private ITable<K, ? extends IEntity<K>> mTable; 
	private K mIdentity; 
	private Map<String, StreamType> mStreams; 
	
	public AbstractEntity() {
		this(null); 
	}
	
	@SuppressWarnings("unchecked")
	public AbstractEntity(K id) {
		mTable = null; 
		mIdentity = (K) (id != null ? id.clone() : null); 
		mStreams = null; 
	}

	private Map<String, StreamType> getStreams() {
		synchronized (this) {
			if (mStreams == null) 
				mStreams = new HashMap<String, StreamType>(); 
			
			return mStreams; 
		}
	}
	
	@Override 
	public final ITable<K, ? extends IEntity<K>> getTable() {
		synchronized (this) {
			return mTable; 
		}
	}
	
	@Override
	public final void setTable(ITable<K, ? extends IEntity<K>> table) {
		synchronized (this) {
			if (mTable != null) 
				throw new DBException("table already set"); 
			
			mTable = table; 
			
			String[] names = getStreamFieldNames(); 
			for (int i=0; names != null && i < names.length; i++) {
				String name = names[i]; 
				Field field = mTable.getEntityField(name); 
				if (field != null) 
					throw new DBException("stream field: "+name+" already set as entity member"); 
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final void setIdentity(K id) {
		synchronized (this) {
			if (mIdentity != null) 
				throw new DBException("identity already set as "+mIdentity); 
			
			mIdentity = (K) id.clone(); 
		}
	}
	
	@Override
	public final void setIdentity(ITable<K, ? extends IEntity<K>> table) {
		synchronized (this) {
			if (mIdentity != null) 
				throw new DBException("identity already set as "+mIdentity); 
			
			mIdentity = table.newIdentity(null); 
		}
	}
	
	@Override
	public final K getIdentity() { 
		return mIdentity; 
	}
	
	@Override
	public int scanStreams() { 
		synchronized (this) {
			if (mTable == null) return 0; 
			
			return mTable.scanStreamFieldsWithCheck(this); 
		}
	}
	
	@Override
	public final void setAsStream(String fieldName, InputStream is) {
		if (fieldName == null || is == null) 
			throw new DBException("fieldName or inputstream is null"); 

		synchronized (this) {
			if (getStreams().containsKey(fieldName)) 
				throw new DBException("field: "+fieldName+" already set stream"); 
			
			getStreams().put(fieldName, new StreamType(fieldName, is)); 
		}
	}
	
	@Override
	public final void setAsStream(String fieldName, String filePath) {
		if (fieldName == null || filePath == null) 
			throw new DBException("fieldName or filePath is null"); 

		synchronized (this) {
			if (getStreams().containsKey(fieldName)) 
				throw new DBException("field: "+fieldName+" already set stream"); 
			
			getStreams().put(fieldName, new StreamType(fieldName, filePath)); 
		}
	}
	
	@Override
	public final void removeStream(String fieldName) { 
		if (fieldName == null) 
			throw new DBException("fieldName is null"); 

		synchronized (this) {
			if (getStreams().containsKey(fieldName)) 
				getStreams().remove(fieldName); 
		}
	}
	
	@Override
	public final InputStream getAsStream(String fieldName) {
		if (fieldName == null) return null; 
		
		synchronized (this) {
			if (mStreams == null) 
				return null; 
			
			try { 
				return getStreams().get(fieldName).newInputStream(); 
			} catch (IOException e) {
				throw new DBException("cannot load field: "+fieldName+" as stream: "+e); 
			}
		}
	}
	
	@Override
	public StreamType getAsStreamType(String fieldName) {
		if (fieldName == null) return null; 
		
		synchronized (this) {
			if (mStreams == null) 
				return null; 
			
			return getStreams().get(fieldName); 
		}
	}
	
	@Override
	public final String[] getStreamFieldNames() {
		synchronized (this) {
			if (mStreams == null) 
				return null; 
			
			return getStreams().keySet().toArray(new String[0]); 
		}
	}
	
	@Override
	public final boolean saveStreams() {
		return saveStreams(this, true); 
	}
	
	@Override
	public final boolean saveStreams(boolean notify) {
		return saveStreams(this, false); 
	}
	
	@Override
	public final boolean saveStreams(IEntity<K> entity) {
		return saveStreams(entity, true); 
	}
	
	@Override
	public final boolean saveStreams(IEntity<K> entity, boolean notify) {
		if (entity == null || !(entity instanceof AbstractEntity)) 
			return false; 

		synchronized (entity) {
			AbstractEntity<K> data = (AbstractEntity<K>)entity; 
			if (data.mStreams == null) 
				return false; 
			
			if (mTable == null) 
				throw new DBException("entity table not set"); 
			
			String[] names = data.getStreamFieldNames(); 
			boolean saved = false; 
			
			for (int i=0; names != null && i < names.length; i++) {
				String name = names[i]; 
				try {
					StreamType type = data.mStreams.get(name); 
					String filepath = mTable.getStreamPath(name, getIdentity()); 
					if (type.saveTo(filepath) > 0) 
						saved = true; 
					
					synchronized (this) {
						if (getAsStreamType(name) != null) 
							getStreams().remove(name); 
						setAsStream(name, filepath); 
					}
				} catch (IOException e) {
					throw new DBException("save entity stream error: "+e); 
				}
			}
			
			if (notify) 
				mTable.notifyEntityChangedWithCheck(entity, DBOperation.ENTITY_UPDATE); 
			
			return saved; 
		}
	}
	
	private Field getEntityField(String fieldName) {
		if (fieldName == null) return null; 
		try {
			if (mTable != null) {
				Field field = mTable.getEntityField(fieldName); 
				return field; 
			} else {
				Field field = getClass().getField(fieldName); 
				return field; 
			}
		} catch (NoSuchFieldException e) {
			return null; 
		}
	}
	
	@Override 
	public Object getAsObject(String fieldName) {
		return getAsObject(fieldName, null); 
	}
	
	@Override 
	public Object getAsObject(String fieldName, Object defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.get(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override 
	public String getAsString(String fieldName) {
		return getAsString(fieldName, null); 
	}
	
	@Override 
	public String getAsString(String fieldName, String defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) {
				Object obj = field.get(this); 
				if (obj != null) {
					if (obj instanceof String) 
						return (String)obj; 
					else
						return obj.toString(); 
				}
			}
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
    }
	
	@Override 
	public short getAsShort(String fieldName) {
		return getAsShort(fieldName, (short)0); 
	}
	
	@Override 
	public short getAsShort(String fieldName, short defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.getShort(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override 
	public int getAsInt(String fieldName) {
		return getAsInt(fieldName, 0); 
	}
	
	@Override 
	public int getAsInt(String fieldName, int defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.getInt(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override 
	public long getAsLong(String fieldName) {
		return getAsLong(fieldName, 0); 
	}
	
	@Override 
	public long getAsLong(String fieldName, long defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.getLong(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override 
	public float getAsFloat(String fieldName) {
		return getAsFloat(fieldName, 0); 
	}
	
	@Override 
	public float getAsFloat(String fieldName, float defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.getFloat(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override 
	public double getAsDouble(String fieldName) {
		return getAsDouble(fieldName, 0); 
	}
	
	@Override 
	public double getAsDouble(String fieldName, double defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.getDouble(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override 
	public char getAsChar(String fieldName) {
		return getAsChar(fieldName, ' '); 
	}
	
	@Override 
	public char getAsChar(String fieldName, char defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.getChar(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override 
	public byte getAsByte(String fieldName) {
		return getAsByte(fieldName, (byte)0); 
	}
	
	@Override 
	public byte getAsByte(String fieldName, byte defaultValue) {
		try {
			Field field = getEntityField(fieldName); 
			if (field != null) 
				return field.getByte(this); 
		} catch (IllegalAccessException e) {
		}
		return defaultValue; 
	}
	
	@Override
	public final void updateFrom(IEntity<K> data) {
		if (data == null) throw new DBException("entity is null"); 
		
		if (data == this) return; 
		
		if (data.getClass() != this.getClass()) 
			throw new DBException("cannot update "+this.getClass()+" from "+data.getClass()); 
		
		onUpdateFrom(data); 
	}
	
	protected void onUpdateFrom(IEntity<K> data) {
		if (mTable == null) throw new DBException("entity is not in table"); 
		
		String[] names = mTable.getEntityFieldNames(); 
		for (int i=0; names != null && i < names.length; i++) {
			String name = names[i]; 
			Field field = mTable.getEntityField(name); 
			try {
				Object value = field.get(data); 
				if (value != null) 
					field.set(this, value); 
			} catch (Exception e) {
				throw new DBException("update field failed: "+e); 
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sbuf = new StringBuilder(); 
		sbuf.append(getClass().getName()); 
		sbuf.append(":"+getIdentity()+":{"); 
		
		Field[] fields = getClass().getFields(); 
		int count = 0; 
		for (int i=0; fields != null && i < fields.length; i++) {
			Field field = fields[i]; 
			try { 
				field.setAccessible(true); 
				Object value = field.get(this); 
				if (value != null) {
					if (count > 0) sbuf.append(' '); 
					sbuf.append(field.getName()); 
					sbuf.append('='); 
					sbuf.append(value); 
					count ++; 
				}
			} catch (IllegalAccessException e) {
				// ignore
			}
		}
		
		Map<String, StreamType> streams = mStreams; 
		if (streams != null) { 
			String[] names = streams.keySet().toArray(new String[0]); 
			for (int i=0; names != null && i < names.length; i++) { 
				String name = names[i]; 
				StreamType type = streams.get(name); 
				if (type != null) { 
					if (count > 0) sbuf.append(' '); 
					sbuf.append("stream/"); 
					sbuf.append(name); 
					sbuf.append('='); 
					sbuf.append(type.getFilePath()); 
				}
			}
		}
		
		sbuf.append('}'); 
		return sbuf.toString(); 
	}
	
}
