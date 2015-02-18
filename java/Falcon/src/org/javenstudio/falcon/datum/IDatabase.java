package org.javenstudio.falcon.datum;

import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.Path;

public interface IDatabase {

	public static interface Manager {
		public String getUserKey();
		public String getUserName();
		public ILockable.Lock getLock();
		public Configuration getConfiguration();
		public IDatabaseStore getDatabaseStore() throws ErrorException;
		public Path getDatabasePath() throws ErrorException;
	}
	
	public static interface TableInfo { 
		public byte[] getTableName();
		public String getTableNameAsString();
		public long getTableId();
	}
	
	public static interface Table { 
		public IDatabase getDatabase();
		public TableInfo getTableInfo();
		public String getTableName();
		
		public Row newRow(byte[] rowKey) throws ErrorException;
		public Query newQuery() throws ErrorException;
		
		public void update(Row row) throws ErrorException;
		public void delete(Row row) throws ErrorException;
		
		public Result get(Query query) throws ErrorException;
		public List<Result> query(Query query) throws ErrorException;
		
		public void flush(boolean syncLog) throws ErrorException;
		public void close() throws ErrorException;
	}
	
	public static interface Row { 
		public byte[] getRowName();
		public void addColumn(Value value);
		public void addColumn(byte[] family, byte[] qualifier, byte[] value);
		public void addColumn(byte[] family, byte[] qualifier, long value);
		public void addColumn(byte[] family, byte[] qualifier, int value);
		public void addColumn(byte[] family, byte[] qualifier, float value);
		public void addColumn(byte[] family, byte[] qualifier, double value);
		public void addColumn(byte[] family, byte[] qualifier, boolean value);
		public void addColumn(byte[] family, byte[] qualifier, String value);
	}
	
	public static interface Result { 
		public byte[] getRowName();
		public String[] getColumnNames(byte[] family);
		public byte[] getColumn(byte[] family, byte[] qualifier);
		public long getColumnLong(byte[] family, byte[] qualifier);
		public int getColumnInt(byte[] family, byte[] qualifier);
		public float getColumnFloat(byte[] family, byte[] qualifier);
		public double getColumnDouble(byte[] family, byte[] qualifier);
		public boolean getColumnBool(byte[] family, byte[] qualifier);
		public String getColumnString(byte[] family, byte[] qualifier);
	}
	
	public static interface Query {
		public void setRow(byte[] key);
		public void setColumn(MatchOp op, Value... values);
		public void setRowSize(long size);
		public void setTimeRange(long minStamp, long maxStamp);
		public void setTimeStamp(long timestamp);
		public void setMaxVersions(int maxVersions);
	}
	
	public static enum MatchOp {
	    /** less than */
	    LESS,
	    /** less than or equal to */
	    LESS_OR_EQUAL,
	    /** equals */
	    EQUAL,
	    /** not equal */
	    NOT_EQUAL,
	    /** greater than or equal to */
	    GREATER_OR_EQUAL,
	    /** greater than */
	    GREATER,
	    /** no operation */
	    NO_OP,
	}
	
	public static class Value { 
		private final byte[] mFamily;
		private final byte[] mQualifier;
		private final byte[] mValue;
		
		public Value(byte[] family, byte[] qualifier, byte[] value) { 
			if (family == null) throw new NullPointerException();
			mFamily = family;
			mQualifier = qualifier;
			mValue = value;
		}
		
		public final byte[] getFamily() { return mFamily; }
		public final byte[] getQualifier() { return mQualifier; }
		public final byte[] getValue() { return mValue; }
	}
	
	public Table getTable(TableInfo info) throws ErrorException;
	public boolean closeTable(TableInfo info) throws ErrorException;
	
	public int getOpenedCount();
	public void syncLog(boolean force) throws ErrorException;
	public void close();
	
}
