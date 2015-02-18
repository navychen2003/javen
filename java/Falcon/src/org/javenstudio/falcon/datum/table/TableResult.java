package org.javenstudio.falcon.datum.table;

import java.util.ArrayList;
import java.util.NavigableMap;

import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.store.Bytes;
import org.javenstudio.falcon.datum.table.store.KeyValue;
import org.javenstudio.falcon.datum.table.store.Result;

public class TableResult implements IDatabase.Result {

	private final TableRegion mTable;
	private final Result mResult;
	private final byte[] mKey;
	
	TableResult(TableRegion table, byte[] key, KeyValue[] kvs) { 
		if (table == null || key == null) throw new NullPointerException();
		if (key.length == 0) throw new IllegalArgumentException("Row key is empty");
		mTable = table;
		mResult = new Result(kvs);
		mKey = key;
	}
	
	TableResult(TableRegion table, Result res) {
		if (table == null || res == null) throw new NullPointerException();
		mTable = table;
		mResult = res;
		mKey = res.getRow();
	}
	
	public TableRegion getTable() { return mTable; }
	public Result getResult() { return mResult; }
	public byte[] getKey() { return mKey; }
	
	@Override
	public byte[] getRowName() {
		return mKey;
	}

	@Override
	public String[] getColumnNames(byte[] family) {
		NavigableMap<byte[], byte[]> map = mResult.getFamilyMap(family);
		if (map != null && map.size() > 0) {
			byte[][] keys = map.keySet().toArray(new byte[map.size()][]);
			if (keys != null && keys.length > 0) {
				ArrayList<String> names = new ArrayList<String>();
				for (byte[] key : keys) {
					names.add(Bytes.toString(key));
				}
				return names.toArray(new String[names.size()]);
			}
		}
		return null;
	}
	
	@Override
	public byte[] getColumn(byte[] family, byte[] qualifier) { 
		return mResult.getValue(family, qualifier);
	}
	
	@Override
	public long getColumnLong(byte[] family, byte[] qualifier) { 
		if (family != null) {
			byte[] val = getColumn(family, qualifier);
			if (val != null) return Bytes.toLong(val);
		}
		return 0;
	}
	
	@Override
	public int getColumnInt(byte[] family, byte[] qualifier) { 
		if (family != null) {
			byte[] val = getColumn(family, qualifier);
			if (val != null) return Bytes.toInt(val);
		}
		return 0;
	}
	
	@Override
	public float getColumnFloat(byte[] family, byte[] qualifier) { 
		if (family != null) {
			byte[] val = getColumn(family, qualifier);
			if (val != null) return Bytes.toFloat(val);
		}
		return 0;
	}
	
	@Override
	public double getColumnDouble(byte[] family, byte[] qualifier) { 
		if (family != null) {
			byte[] val = getColumn(family, qualifier);
			if (val != null) return Bytes.toDouble(val);
		}
		return 0;
	}
	
	@Override
	public boolean getColumnBool(byte[] family, byte[] qualifier) { 
		if (family != null) {
			byte[] val = getColumn(family, qualifier);
			if (val != null) return Bytes.toBoolean(val);
		}
		return false;
	}
	
	@Override
	public String getColumnString(byte[] family, byte[] qualifier) { 
		if (family != null) {
			byte[] val = getColumn(family, qualifier);
			if (val != null) return Bytes.toString(val);
		}
		return null;
	}
	
}
