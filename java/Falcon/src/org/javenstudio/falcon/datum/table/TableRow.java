package org.javenstudio.falcon.datum.table;

import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.store.Put;
import org.javenstudio.raptor.bigdb.util.Bytes;

public class TableRow implements IDatabase.Row {

	private final TableRegion mTable;
	private final byte[] mKey;
	
	private Put mPut = null;
	
	TableRow(TableRegion table, byte[] key) { 
		if (table == null || key == null) throw new NullPointerException();
		if (key.length == 0) throw new IllegalArgumentException("Row key is empty");
		mTable = table;
		mKey = key;
	}
	
	public TableRegion getTable() { return mTable; }
	public byte[] getKey() { return mKey; }
	public Put getPut() { return mPut; }
	
	@Override
	public byte[] getRowName() {
		return mKey;
	}

	@Override
	public synchronized void addColumn(byte[] family, 
			byte[] qualifier, byte[] value) { 
		if (family == null || value == null) throw new NullPointerException();
		
		if (mPut == null) mPut = new Put(getKey());
		mPut.add(family, qualifier, value);
	}
	
	@Override
	public void addColumn(IDatabase.Value val) {
		if (val == null || val.getValue() == null) throw new NullPointerException();
		addColumn(val.getFamily(), val.getQualifier(), val.getValue());
	}

	@Override
	public void addColumn(byte[] family, byte[] qualifier, long value) { 
		addColumn(family, qualifier, Bytes.toBytes(value));
	}
	
	@Override
	public void addColumn(byte[] family, byte[] qualifier, int value) { 
		addColumn(family, qualifier, Bytes.toBytes(value));
	}
	
	@Override
	public void addColumn(byte[] family, byte[] qualifier, float value) { 
		addColumn(family, qualifier, Bytes.toBytes(value));
	}
	
	@Override
	public void addColumn(byte[] family, byte[] qualifier, double value) { 
		addColumn(family, qualifier, Bytes.toBytes(value));
	}
	
	@Override
	public void addColumn(byte[] family, byte[] qualifier, boolean value) { 
		addColumn(family, qualifier, Bytes.toBytes(value));
	}
	
	@Override
	public void addColumn(byte[] family, byte[] qualifier, String value) { 
		if (value != null)
			addColumn(family, qualifier, Bytes.toBytes(value));
	}
	
}
