package org.javenstudio.falcon.datum.bdb;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.client.DBTable;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.client.Result;
import org.javenstudio.raptor.bigdb.client.ResultScanner;
import org.javenstudio.raptor.bigdb.client.Scan;
import org.javenstudio.raptor.bigdb.util.Bytes;

public class BdbTable {
	private static final Logger LOG = Logger.getLogger(BdbTable.class);

	private final BdbAdmin mAdmin;
	private final DBTableDescriptor mDescriptor;
	private DBTable mTable = null;
	
	BdbTable(BdbAdmin admin, DBTableDescriptor dscr) { 
		if (admin == null || dscr == null) throw new NullPointerException();
		mAdmin = admin;
		mDescriptor = dscr;
	}
	
	public BdbAdmin getAdmin() { return mAdmin; }
	public DBTableDescriptor getDescriptor() { return mDescriptor; }
	
	public String getTableName() { 
		return getDescriptor().getNameAsString(); 
	}
	
	public synchronized DBTable getTable() throws IOException { 
		if (mTable == null)
			mTable = new DBTable(getAdmin().getConf(), getDescriptor().getName());
		return mTable;
	}
	
	public BdbTableRow[] listRows() throws IOException { 
		if (LOG.isDebugEnabled())
			LOG.debug("listRows: table=" + getTableName());
		
		ArrayList<BdbTableRow> list = new ArrayList<BdbTableRow>();
		
		ResultScanner scaner = getTable().getScanner(new Scan());
		Result result = null;
		while ((result = scaner.next()) != null) { 
			list.add(new BdbTableRow(this, result));
		}
		
		return list.toArray(new BdbTableRow[list.size()]);
	}
	
	public void putRow(String name, byte[] content) throws IOException { 
		putRow(name, content, System.currentTimeMillis());
	}
	
	public void putRow(String name, byte[] content, long ts) throws IOException { 
		if (name == null || content == null)
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("putRow: name=" + name);
		
		Put put = new Put(Bytes.toBytes(name));
		put.add(Bytes.toBytes("cf"), Bytes.toBytes("a"), ts, content);
		
		getTable().put(put);
	}
	
}
