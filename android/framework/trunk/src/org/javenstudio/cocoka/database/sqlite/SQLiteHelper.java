package org.javenstudio.cocoka.database.sqlite;

import java.util.Map;

import android.content.ContentValues;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.rdb.DBField.FieldProperty;
import org.javenstudio.common.entitydb.rdb.DBField.FieldType;
import org.javenstudio.common.entitydb.rdb.DBValues;

public class SQLiteHelper {

	public static String fieldTypeToString(FieldType type) {
		if (type == FieldType.INTEGER) 
			return "INTEGER"; 
		
		if (type == FieldType.FLOAT) 
			return "FLOAT"; 
		
		if (type == FieldType.DOUBLE) 
			return "DOUBLE"; 
		
		if (type == FieldType.TEXT) 
			return "TEXT"; 
		
		if (type == FieldType.BYTEARRAY)
			return "BLOB"; 
		
		throw new DBException("not supported FieldType: "+type); 
	}
	
	public static String fieldPropertyToString(FieldProperty prop) {
		if (prop == FieldProperty.PRIMARY_KEY) 
			return "PRIMARY KEY"; 
		
		if (prop == FieldProperty.AUTOINCREMENT) 
			return "AUTOINCREMENT"; 
		
		if (prop == FieldProperty.NOT_NULL) 
			return "NOT NULL"; 
		
		throw new DBException("not supported FieldProperty: "+prop); 
	}
	
	public static ContentValues toContentValues(DBValues values) { 
		if (values != null) { 
			if (values instanceof SQLiteValues) 
				return ((SQLiteValues)values).mValues;
			
			ContentValues newValues = new ContentValues();
			
			for (Map.Entry<String, Object> entry : values.valueSet()) { 
				String key = entry.getKey();
				Object value = entry.getValue();
				
				if (value == null) { 
					newValues.putNull(key); 
					continue;
				}
				
				if (value instanceof String)
					newValues.put(key, (String)value);
				else if (value instanceof Byte) 
					newValues.put(key, (Byte)value);
				else if (value instanceof Short) 
					newValues.put(key, (Short)value);
				else if (value instanceof Integer) 
					newValues.put(key, (Integer)value);
				else if (value instanceof Long) 
					newValues.put(key, (Long)value);
				else if (value instanceof Float) 
					newValues.put(key, (Float)value);
				else if (value instanceof Double) 
					newValues.put(key, (Double)value);
				else if (value instanceof byte[]) 
					newValues.put(key, (byte[])value);
				else if (value instanceof Boolean) 
					newValues.put(key, (Boolean)value);
			}
		}
		
		return null;
	}
	
}
