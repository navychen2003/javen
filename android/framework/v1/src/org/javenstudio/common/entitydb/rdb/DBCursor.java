package org.javenstudio.common.entitydb.rdb;

public interface DBCursor {

	public Object getIdentityValue(String fieldName);
	
	public int getCount();
	public int getPosition();
	
	public boolean move(int offset);
	public boolean moveToPosition(int position);
	public boolean moveToFirst();
	public boolean moveToLast();
	public boolean moveToNext();
	public boolean moveToPrevious();
	public boolean isFirst();
	public boolean isLast();
	public boolean isBeforeFirst();
	public boolean isAfterLast();
	
	public int getColumnIndex(String columnName);
	public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException;
	public String getColumnName(int columnIndex);
	public String[] getColumnNames();
	public int getColumnCount();
	
	public byte[] getBlob(int columnIndex);
	public String getString(int columnIndex);
	public short getShort(int columnIndex);
	public int getInt(int columnIndex);
	public long getLong(int columnIndex);
	public float getFloat(int columnIndex);
	public double getDouble(int columnIndex);
	public boolean isNull(int columnIndex);
	
	public boolean requery();
	public void close();
	public boolean isClosed();
	
}
