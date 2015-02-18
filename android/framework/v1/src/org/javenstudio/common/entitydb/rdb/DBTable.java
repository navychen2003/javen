package org.javenstudio.common.entitydb.rdb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.entitydb.DBException;
import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.rdb.DBField.FieldProperty;
import org.javenstudio.common.entitydb.rdb.DBField.FieldType;
import org.javenstudio.common.entitydb.type.StreamType;
import org.javenstudio.common.util.Logger;

public abstract class DBTable<K extends IIdentity, T extends IEntity<K>> {
	private static Logger LOG = Logger.getLogger(DBTable.class);

	public static class FieldInfo {
		public final String name; 
		public final Field entityField; 
		public final DBField dbField; 
		
		public FieldInfo(String name, Field ef, DBField df) {
			this.name = name; 
			this.entityField = ef; 
			this.dbField = df; 
		}
	}
	
	private final DBOpenHelper mHelper; 
	private final List<FieldInfo> mFieldList; 
	private final Map<String, FieldInfo> mFieldMap; 
	private final Set<String> mStreamFields; 
	
	private final Class<T> mEntityClass; 
	private final String[] mEntityFieldNames; 
	
	private String[] mColumnNames; 
	private DBField mIdentityField;
	private boolean mFieldsInited; 
	
	public DBTable(final DBOpenHelper helper, Class<T> entityClass) {
		mHelper = helper; 
		mFieldList = new ArrayList<FieldInfo>(); 
		mFieldMap = new HashMap<String, FieldInfo>(); 
		mStreamFields = new HashSet<String>(); 
		mEntityClass = entityClass; 
		mColumnNames = null; 
		mFieldsInited = false; 
		
		if (entityClass == null || !IEntity.class.isAssignableFrom(entityClass)) 
			throw new DBException("entity class: "+entityClass+" is not a IEntity class");

		Field[] fields = entityClass.getDeclaredFields(); 
		if (fields != null) {
			mEntityFieldNames = new String[fields.length]; 
			for (int i=0; i < fields.length; i++) {
				Field field = fields[i]; 
				field.setAccessible(true); // private accessable
				mEntityFieldNames[i] = field.getName(); 
			}
		} else 
			mEntityFieldNames = null; 
		
		initFields(); 
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("DBTable: " + getClass().getName() + " created with entityClass: " + entityClass.getName() 
					+ " with identityField: " + getIdentityFieldName());
		}
	}
	
	public final DBOpenHelper getDBOpenHelper() {
		return mHelper; 
	}
	
	public final Database getWritableDatabase() {
		return mHelper.getWritableDatabase(); 
	}
	
	public final Database getReadableDatabase() {
		return mHelper.getReadableDatabase(); 
	}
	
	public final Class<? extends IEntity<K>> getEntityClass() {
		return mEntityClass; 
	}
	
	public final String[] getEntityFieldNames() {
		return mEntityFieldNames; 
	}
	
	public final Field getEntityField(String name) {
		try {
			return mEntityClass.getField(name); 
		} catch (NoSuchFieldException e) {
			return null; 
		}
	}
	
	public final String[] getStreamFieldNames() {
		return mStreamFields.toArray(new String[0]); 
	}
	
	public final List<FieldInfo> getFieldList() {
		return mFieldList; 
	}
	
	public final DBField getField(String name) {
		synchronized (this) {
			if (mFieldMap.containsKey(name)) 
				return mFieldMap.get(name).dbField; 
			else
				return null; 
		}
	}
	
	public final String getIdentityFieldName() { 
		return mIdentityField != null ? mIdentityField.getName() : null;
	}
	
	private final void initFields() {
		if (mFieldsInited) return; 
		
		synchronized (this) { 
			//addField(Constants.IDENTITY_FIELDNAME, FieldType.INTEGER, 
			//		FieldProperty.PRIMARY_KEY, FieldProperty.AUTOINCREMENT); 
			
			mIdentityField = addIdentityField();
			if (mIdentityField == null) 
				throw new DBException(getClass().getSimpleName()+" must has a identity field");
			
			FieldInfo info = mFieldMap.get(mIdentityField.getName());
			if (info == null || info.dbField != mIdentityField) 
				throw new DBException(getClass().getSimpleName()+" must add a identity field");
			
			if (mFieldMap.size() != 1) 
				throw new DBException(getClass().getSimpleName()+".addIdentityField must add only one identity field");
		}
		
		onInitFields(); 
		
		mFieldsInited = true; 
	}
	
	protected abstract DBField addIdentityField();
	
	protected final DBField addField(String name, FieldType type, FieldProperty... props) {
		if (name == null || mFieldsInited) 
			return null; 
		
		synchronized (this) {
			if (mStreamFields.contains(name)) 
				throw new DBException("field: "+name+" already set as stream field"); 
			
			if (mFieldMap.containsKey(name)) 
				return mFieldMap.get(name).dbField; 
			
			Field entityField = getEntityField(name); 
			checkEntityField(name, entityField); 
			
			DBField dbField = new DBField(this, name, type, props); 
			FieldInfo info = new FieldInfo(name, entityField, dbField); 
			
			mFieldList.add(info); 
			mFieldMap.put(name, info); 
			
			return dbField; 
		}
	}
	
	protected final void addStreamField(String name) {
		if (name == null || mFieldsInited) 
			return; 
		
		synchronized (this) {
			if (mFieldMap.containsKey(name)) 
				throw new DBException("field: "+name+" already set as entity field"); 
			
			if (mStreamFields.contains(name)) 
				throw new DBException("field: "+name+" already set"); 
			
			mStreamFields.add(name); 
		}
	}
	
	private void checkEntityField(String name, Field entityField) {
		if (entityField == null) {
			String identityFieldName = getIdentityFieldName();
			if (identityFieldName != null && !name.equals(identityFieldName))
				throw new DBException("entity field: "+name+" not found"); 
			return; 
		}
		
		Class<?> cls = entityField.getType(); 
		if (cls == String.class || cls == Integer.class || cls == Long.class || 
			cls == Float.class || cls == Double.class) 
			return; 
		
		if (cls == DBField.BYTEARRAY_TYPE) return; 
		
		throw new DBException("entity field: "+name+" type: "+cls.getName()+" not support"); 
	}
	
	public abstract String getTableName(); 
	protected abstract String getNullColumnHack(); 
	
	protected void onInitFields() {
		Field[] fields = mEntityClass.getDeclaredFields(); 
		for (int i=0; fields != null && i < fields.length; i++) {
			Field field = fields[i]; 
			String name = field.getName(); 
			
			if (name.equals(getIdentityFieldName())) 
				continue; 
			
			if (field.getType() == String.class) {
				addField(name, DBField.FieldType.TEXT); 
				
			} else if (field.getType() == Integer.class || field.getType() == Long.class) {
				addField(name, DBField.FieldType.INTEGER); 
				
			} else if (field.getType() == Float.class) { 
				addField(name, DBField.FieldType.FLOAT); 
				
			} else if (field.getType() == Double.class) { 
				addField(name, DBField.FieldType.DOUBLE); 
				
			} else if (field.getType() == DBField.BYTEARRAY_TYPE) {
				addField(name, DBField.FieldType.BYTEARRAY); 
				
			} else 
				throw new IllegalArgumentException("unsupported field type: "+field.getType()); 
		}
	}
	
	protected DBValues newDBValues() { 
		return new DefaultDBValues();
	}
	
	protected DBValues mappingValues(T data) {
		DBValues values = newDBValues(); 
		
		for (FieldInfo info : mFieldList) {
			String name = info.name; 
			Field field = info.entityField; 
			DBField df = info.dbField; 
			
			if (name.equals(getIdentityFieldName())) 
				continue; 
			
			try { 
				Object value = field.get(data); 

				if (df.getType() == DBField.FieldType.INTEGER) {
					mappingIntegerValue(values, name, value); 
					
				} else if (df.getType() == DBField.FieldType.FLOAT) { 
					mappingFloatValue(values, name, value); 
					
				} else if (df.getType() == DBField.FieldType.DOUBLE) { 
					mappingDoubleValue(values, name, value); 
					
				} else if (df.getType() == DBField.FieldType.TEXT) {
					mappingTextValue(values, name, value); 
					
				} else if (df.getType() == DBField.FieldType.BYTEARRAY) { 
					mappingBytesValue(values, name, value); 
					
				} else 
					throw new IllegalArgumentException("unknown field type: "+df.getType()); 

			} catch (Exception e) {
				throw new DBException("mapping value from field: "+name+" error: "+e); 
			}
		}
		
		return values; 
	}
	
	private void mappingIntegerValue(DBValues values, String name, Object value) {
		if (value == null) return; 
		
		if (value instanceof Integer) {
			values.put(name, (Integer)value); 
			
		} else if (value instanceof Long) {
			values.put(name, (Long)value); 
			
		} else 
			throw new DBException("field: "+name+" is "+value.getClass()+", should be Integer or Long"); 
	}
	
	private void mappingFloatValue(DBValues values, String name, Object value) {
		if (value == null) return; 
		
		if (value instanceof Float) {
			values.put(name, (Float)value); 
			
		} else 
			throw new DBException("field: "+name+" is "+value.getClass()+", should be Float"); 
	}
	
	private void mappingDoubleValue(DBValues values, String name, Object value) {
		if (value == null) return; 
		
		if (value instanceof Double) {
			values.put(name, (Double)value); 
			
		} else 
			throw new DBException("field: "+name+" is "+value.getClass()+", should be Double"); 
	}
	
	private void mappingTextValue(DBValues values, String name, Object value) {
		if (value == null) return; 
		
		if (value instanceof String) {
			values.put(name, (String)value); 
			
		} else
			throw new DBException("field: "+name+" is "+value.getClass()+", should be String");
	}
	
	private void mappingBytesValue(DBValues values, String name, Object value) {
		if (value == null) return; 
		
		if (value instanceof byte[]) {
			values.put(name, (byte[])value); 
			
		} else
			throw new DBException("field: "+name+" is "+value.getClass()+", should be byte[]");
	}
	
	public String[] getColumnNames() {
		synchronized (this) {
			if (mColumnNames == null) { 
				ArrayList<String> names = new ArrayList<String>(); 
				for (FieldInfo field : mFieldList) {
					names.add(field.name); 
				}
				mColumnNames = names.toArray(new String[names.size()]); 
			}
			return mColumnNames; 
		}
	}
	
	private void saveStreams(T data, String id) {
		if (data == null) return; 
		
		String[] names = data.getStreamFieldNames(); 
		for (int i=0; names != null && i < names.length; i++) {
			String name = names[i]; 
			saveStream(data, name, id); 
		}
	}
	
	public String getStreamPath(String fieldName, IIdentity id) {
		return getStreamPath(fieldName, id.toString()); 
	}
	
	public String getStreamPath(String fieldName, String id) {
		String dbpath = mHelper.getDatabaseDirectory(); 
		String dbname = mHelper.getDatabaseName(); 
		int pos = dbname.lastIndexOf('.'); 
		if (pos > 0) dbname = dbname.substring(0, pos); 
		
		return dbpath + "/" + dbname + "/" + getTableName() + "/" + 
			getTableName() + "_" + fieldName + "_" + id + ".dat"; 
	}
	
	private void saveStream(T data, String name, String id) {
		if (name == null) return; 
		
		StreamType stream = data.getAsStreamType(name); 
		if (stream == null) return; 
		
		InputStream in = null; 
		try {
			String filepath = getStreamPath(name, id); 
			if (!stream.isSaved()) {
				stream.saveTo(filepath); 
				
			} else {
				StreamType.makeFilePath(filepath); 
				
				in = new BufferedInputStream(stream.newInputStream()); 
				OutputStream out = new BufferedOutputStream(new FileOutputStream(filepath)); 
				
				byte[] buffer = new byte[4096]; 
				int readbytes = 0, totalsize = 0; 

				while ((readbytes = in.read(buffer, 0, buffer.length)) >= 0) {
					if (readbytes > 0) {
						out.write(buffer, 0, readbytes); 
						totalsize += readbytes; 
					}
				}
				
				out.flush(); 
				out.close(); 
				
				LOG.info("saved "+totalsize+" bytes stream field to "+filepath); 
			}
			
		} catch (IOException e) {
			throw new DBException("save stream field: "+name+" failed: "+e); 
			
		} finally {
			try {
				if (in != null) in.close(); 
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final K insertRow(DBValues values) {
		return (K)getWritableDatabase().insert(getTableName(), getNullColumnHack(), values); 
	}
	
	public K insert(T data) {
		if (data == null) return null; 
		K id = insertRow(mappingValues(data)); 
		if (id == null) throw new DBException("DBTable: insertRow to table: "+getTableName()+" error return "+id); 
		saveStreams(data, id.toString()); 
		return id; 
	}
	
	public int update(K id, T data) {
		if (data == null) return 0; 
		if (data.getIdentity() == null) 
			data.setIdentity(id); 
		return update(data); 
	}
	
	public int update(T data) {
		if (data == null) return 0; 
		if (data.getIdentity() == null) 
			throw new DBException("entity has null identity"); 
		saveStreams(data, data.getIdentity().toString()); 
		return getWritableDatabase().update(getTableName(), mappingValues(data), data.getIdentity()); 
	}
	
	public int delete(K id) {
		if (id == null) return 0; 
		return getWritableDatabase().delete(getTableName(), id); 
	}
	
	public int deleteMany(String whereClause) {
		return deleteMany(whereClause, null); 
	}
	
	public int deleteMany(String whereClause, String[] whereArgs) {
		return getWritableDatabase().delete(getTableName(), whereClause, whereArgs); 
	}
	
	public DBCursor query(String[] columns, String selection, String[] selectionArgs, 
			String groupBy, String having, String orderBy, String limit) {
		return getWritableDatabase().query(false, getTableName(), 
				columns, selection, selectionArgs, groupBy, having, orderBy, limit); 
	}
	
	public DBCursor query(String[] columns, String selection, String[] selectionArgs) {
		return query(columns, selection, selectionArgs, null, null, null, null); 
	}
	
	public DBCursor query(String[] columns, String selection) {
		return query(columns, selection, null, null, null, null, null); 
	}
	
	public DBCursor query(String selection) {
		return query(getColumnNames(), selection, null, null, null, null, null); 
	}
	
	public int queryCount(String selection) {
		return getWritableDatabase().queryCount(getTableName(), selection); 
	}
	
	public DBCursor queryById(K id) {
		return query(getIdentityFieldName() + "=" + id.toSQL()); 
	}
	
}
