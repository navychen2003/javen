package org.javenstudio.falcon.datum.bdb;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.client.DBAdmin;
import org.javenstudio.raptor.conf.Configuration;

public class BdbAdmin {
	private static final Logger LOG = Logger.getLogger(BdbAdmin.class);

	public static final int DEFAULT_PORT = DBConstants.DEFAULT_PAXOS_CLIENT_PORT; 
	
	private final Configuration mConf;
	private DBAdmin mAdmin = null;
	private BdbTable[] mTables = null;
	
	public BdbAdmin(Configuration conf) { 
		if (conf == null) throw new NullPointerException();
		mConf = conf;
	}
	
	public Configuration getConf() { return mConf; }
	
	private synchronized DBAdmin getAdmin() throws IOException { 
		if (mAdmin == null)
			mAdmin = new DBAdmin(mConf);
		return mAdmin;
	}
	
	public synchronized BdbTable[] getTables() throws IOException { 
		if (mTables == null) { 
			if (LOG.isDebugEnabled())
				LOG.debug("getTables: listTables");
			
			ArrayList<BdbTable> list = new ArrayList<BdbTable>();
			DBTableDescriptor[] tables = getAdmin().listTables();
			for (int i=0; tables != null && i < tables.length; i++) { 
				DBTableDescriptor table = tables[i];
				if (table != null)
					list.add(new BdbTable(this, table));
			}
			mTables = list.toArray(new BdbTable[list.size()]);
		}
		return mTables;
	}
	
}
