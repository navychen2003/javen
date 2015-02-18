package org.javenstudio.falcon.datum.bdb;

import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.util.Bytes;

public class BdbTableRow {
	private static final Logger LOG = Logger.getLogger(BdbTableRow.class);

	private final BdbTable mTable;
	private final String mName;
	private final byte[] mValue;
	private final long mTimestamp;
	
	BdbTableRow(BdbTable table, Result result) { 
		if (table == null || result == null) throw new NullPointerException();
		mTable = table;
		mName = Bytes.toStringBinary(result.getRow());
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadColumns: list columns: " + mName);
		
		byte[] value = null;
		long timestamp = 0;
		
		List<KeyValue> list = result.list();
		if (list != null) { 
			for (KeyValue kv : list) { 
				value = kv.getValue();
				timestamp = kv.getTimestamp();
				break;
			}
		}
		
		if (value == null)
			value = new byte[0];
		
		mValue = value;
		mTimestamp = timestamp;
	}
	
	public BdbTable getTable() { return mTable; }
	public String getName() { return mName; }
	
	public byte[] getContent() { return mValue; }
	public int getContentLength() { return mValue != null ? mValue.length : 0; }
	public long getTimestamp() { return mTimestamp; }
	
}
