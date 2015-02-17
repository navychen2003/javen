package org.javenstudio.common.entitydb.db;

import java.lang.reflect.Field;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.IQuery;
import org.javenstudio.common.entitydb.ITable;

public abstract class BaseCursor<K extends IIdentity, T extends IEntity<K>> 
		extends AbstractCursor<K,T> {

	public BaseCursor(ITable<K,T> table, IQuery<K,T> query) {
		super(table, query); 
	}
	
	public boolean isBeforeFirst() {
		synchronized (this) {
			return mPosition < 0; 
		}
	}
	
	public boolean isAfterLast() {
		synchronized (this) {
			return mPosition >= getCount(); 
		}
	}
	
	public int getColumnIndex(String columnName) {
		return getTable().getColumnIndex(columnName); 
	}
	
	public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
		int index = getColumnIndex(columnName); 
		if (index < 0) throw new IllegalArgumentException("column: "+columnName+" not found"); 
		return index; 
	}
	
	public String getColumnName(int columnIndex) {
		return getTable().getColumnName(columnIndex); 
	}
	
	public String[] getColumnNames() {
		return getTable().getColumnNames(); 
	}
	
	public int getColumnCount() {
		return getTable().getColumnCount(); 
	}
	
	public Object getObject(int columnIndex) {
		try {
			T data = get(); 
			if (columnIndex == 0) 
				return data.getIdentity(); 
			
			String name = getColumnName(columnIndex); 
			Field field = getTable().getEntityField(name); 
			return field.get(data); 
			
		} catch (NullPointerException e) {
			throw new DBException("column: "+columnIndex+" not found"); 
		} catch (IllegalAccessException e) {
			throw new DBException("column: "+columnIndex+" not found"); 
		}
	}
	
	public byte[] getBlob(int columnIndex) {
		return (byte[])getObject(columnIndex); 
	}
	
	public String getString(int columnIndex) {
		return (String)getObject(columnIndex); 
	}
	
	public short getShort(int columnIndex) {
		Object obj = getObject(columnIndex); 
		if (obj == null) return 0; 
		if (obj instanceof Number) 
			return ((Number)obj).shortValue(); 
		else 
			return 0; 
	}
	
	public int getInt(int columnIndex) {
		Object obj = getObject(columnIndex); 
		if (obj == null) return 0; 
		if (obj instanceof Number) 
			return ((Number)obj).intValue(); 
		else 
			return 0; 
	}
	
	public long getLong(int columnIndex) {
		Object obj = getObject(columnIndex); 
		if (obj == null) return 0; 
		if (obj instanceof Number) 
			return ((Number)obj).longValue(); 
		else 
			return 0; 
	}
	
	public float getFloat(int columnIndex) {
		Object obj = getObject(columnIndex); 
		if (obj == null) return 0; 
		if (obj instanceof Number) 
			return ((Number)obj).floatValue(); 
		else 
			return 0; 
	}
	
	public double getDouble(int columnIndex) {
		Object obj = getObject(columnIndex); 
		if (obj == null) return 0; 
		if (obj instanceof Number) 
			return ((Number)obj).doubleValue(); 
		else 
			return 0; 
	}
	
	public boolean isNull(int columnIndex) {
		return getObject(columnIndex) == null; 
	}
	
}
