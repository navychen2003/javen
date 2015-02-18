package org.javenstudio.cocoka.database.sqlite;

import android.database.Cursor;

import org.javenstudio.common.entitydb.rdb.DBCursor;

public class SQLiteCursor implements DBCursor {

	private final Cursor mCursor;
	public SQLiteCursor(Cursor cursor) { mCursor = cursor; }
	
	public Object getIdentityValue(String fieldName) { 
		return mCursor.getLong(mCursor.getColumnIndex(fieldName));
	}
	
	public int getCount() { return mCursor.getCount(); }
	public int getPosition() { return mCursor.getPosition(); }
	
	public boolean move(int offset) { return mCursor.move(offset); }
	public boolean moveToPosition(int position) { return mCursor.moveToPosition(position); }
	public boolean moveToFirst() { return mCursor.moveToFirst(); }
	public boolean moveToLast() { return mCursor.moveToLast(); }
	public boolean moveToNext() { return mCursor.moveToNext(); }
	public boolean moveToPrevious() { return mCursor.moveToPrevious(); }
	public boolean isFirst() { return mCursor.isFirst(); }
	public boolean isLast() { return mCursor.isLast(); }
	public boolean isBeforeFirst() { return mCursor.isBeforeFirst(); }
	public boolean isAfterLast() { return mCursor.isAfterLast(); }
	
	public int getColumnIndex(String columnName) { return mCursor.getColumnIndex(columnName); }
	public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException { 
		return mCursor.getColumnIndexOrThrow(columnName);
	}
	public String getColumnName(int columnIndex) { return mCursor.getColumnName(columnIndex); }
	public String[] getColumnNames() { return mCursor.getColumnNames(); }
	public int getColumnCount() { return mCursor.getColumnCount(); }
	
	public byte[] getBlob(int columnIndex) { return mCursor.getBlob(columnIndex); }
	public String getString(int columnIndex) { return mCursor.getString(columnIndex); }
	public short getShort(int columnIndex) { return mCursor.getShort(columnIndex); }
	public int getInt(int columnIndex) { return mCursor.getInt(columnIndex); }
	public long getLong(int columnIndex) { return mCursor.getLong(columnIndex); }
	public float getFloat(int columnIndex) { return mCursor.getFloat(columnIndex); }
	public double getDouble(int columnIndex) { return mCursor.getDouble(columnIndex); }
	public boolean isNull(int columnIndex) { return mCursor.isNull(columnIndex); }
	
	public boolean requery() { return mCursor.requery(); }
	public void close() { mCursor.close(); }
	public boolean isClosed() { return mCursor.isClosed(); }
	
}
