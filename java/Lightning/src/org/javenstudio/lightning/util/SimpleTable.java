package org.javenstudio.lightning.util;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.datum.table.store.Bytes;
import org.javenstudio.falcon.datum.table.store.Compression;
import org.javenstudio.falcon.datum.table.store.DBColumnDescriptor;
import org.javenstudio.falcon.datum.table.store.DBConstants;
import org.javenstudio.falcon.datum.table.store.DBLog;
import org.javenstudio.falcon.datum.table.store.DBRegion;
import org.javenstudio.falcon.datum.table.store.DBRegionInfo;
import org.javenstudio.falcon.datum.table.store.DBTableDescriptor;
import org.javenstudio.falcon.datum.table.store.Delete;
import org.javenstudio.falcon.datum.table.store.Get;
import org.javenstudio.falcon.datum.table.store.InternalScanner;
import org.javenstudio.falcon.datum.table.store.KeyValue;
import org.javenstudio.falcon.datum.table.store.Put;
import org.javenstudio.falcon.datum.table.store.Result;
import org.javenstudio.falcon.datum.table.store.Scan;
import org.javenstudio.falcon.datum.table.store.StoreFile;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public class SimpleTable extends SimpleShell {

	public static void main(String[] args) throws Exception {
		Configuration conf = loadConf().getConf();
		conf.set("bigdb.tmp.dir", ".");
		
		String action = "";
		ArrayList<String> list = new ArrayList<String>();
		for (int i=0; args != null && i < args.length; i++) { 
			String arg = args[i];
			if (i == 0) action = arg;
			else list.add(arg);
		}
		
		String[] args2 = list.toArray(new String[list.size()]);
		
		if (action.equalsIgnoreCase("-log")) { 
			logMain(args2, conf);
		} else if (action.equalsIgnoreCase("-put") || action.equalsIgnoreCase("-scan") || 
				action.equalsIgnoreCase("-get") || action.equalsIgnoreCase("-set") || 
				action.equalsIgnoreCase("-del") || action.equalsIgnoreCase("-compact")) { 
			regionMain(action, args2, conf);
		} else { 
			System.out.println("Usage: SimpleTable -log/put/scan/get/set/del/compact [...]");
		}
	}
	
	static void logMain(String[] args, Configuration conf) throws Exception {
		final Path filepath = new Path(args[0]);
		DBLog.dump(conf, filepath);
	}
	
	static void regionMain(String action, String[] args, Configuration conf) throws Exception {
		final Path tableDir = new Path(args[0]);
		
		final FileSystem fs = FileSystem.getLocal(conf);
		final Path logdir = new Path(conf.get("bigdb.tmp.dir"),
				".logtmp/.dblog" + tableDir.getName() + System.currentTimeMillis()); 
		final Path oldLogDir = new Path(conf.get("bigdb.tmp.dir"),
				".logtmp/" + DBConstants.DBREGION_OLDLOGDIR_NAME);
		final DBLog log = new DBLog(fs, logdir, oldLogDir, conf, null);
		
		try {
			DBTableDescriptor tableDesc = new DBTableDescriptor("meta");
			tableDesc.addFamily(new DBColumnDescriptor(
					DBConstants.CATALOG_FAMILY,
					DBConstants.ALL_VERSIONS, Compression.Algorithm.NONE.getName(), 
					true, true, 8 * 1024,
					DBConstants.FOREVER, StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				));
			tableDesc.addFamily(new DBColumnDescriptor(
					DBConstants.CATALOG_HISTORIAN_FAMILY,
					DBConstants.ALL_VERSIONS, Compression.Algorithm.NONE.getName(),
					false, false,  8 * 1024,
					DBConstants.WEEK_IN_SECONDS,StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				));
			
			DBRegionInfo regionInfo = new DBRegionInfo(1L, tableDesc);
			DBRegion region = DBRegion.newDBRegion(tableDir, log, fs, conf, regionInfo, null);
			region.initialize();
			
			if (action.equalsIgnoreCase("-put")) {
				final int count = getArgInt(args, 1, 100000);
				final int num = getArgInt(args, 2, 100);
				
				for (int i=0; i < count; i++) {
					Put put = new Put(Bytes.toBytes("row" + (i%num)));
					put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER, 
							Bytes.toBytes("data-" + i + "-info-" + System.currentTimeMillis()));
					put.add(DBConstants.CATALOG_HISTORIAN_FAMILY, DBConstants.SERVER_QUALIFIER, 
							Bytes.toBytes("data-" + i + "-historian-" + System.currentTimeMillis()));
					region.put(put);
				}
			} else if (action.equalsIgnoreCase("-set")) {
				final String row = getArgStr(args, 1, "");
				final String dataInfo = getArgStr(args, 2, "");
				final String dataHist = getArgStr(args, 3, "");
				
				Put put = new Put(Bytes.toBytes(row));
				put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER, 
						Bytes.toBytes(dataInfo));
				put.add(DBConstants.CATALOG_HISTORIAN_FAMILY, DBConstants.SERVER_QUALIFIER, 
						Bytes.toBytes(dataHist));
				
				region.put(put);
				
			} else if (action.equalsIgnoreCase("-del")) {
				final String row = getArgStr(args, 1, "");
				
				Delete del = new Delete(Bytes.toBytes(row));
				
				region.delete(del, null, true);
				
			} else if (action.equalsIgnoreCase("-compact")) {
				region.compactStores(true);
				
			} else if (action.equalsIgnoreCase("-get")) {
				final String row = getArgStr(args, 1, "");
				final int maxVer = getArgInt(args, 2, 1);
				
				Get get = new Get(Bytes.toBytes(row));
				get.setMaxVersions(maxVer);
				
				Result result = region.get(get, null);
				System.out.println(">>> result=" + result);
				
			} else { 
				//final int startRow = getArgInt(args, 1, 1);
				//final int stopRow = getArgInt(args, 2, 10);
				
				// Default behavior
				Scan scan = new Scan();
				// scan.addFamily(DBConstants.CATALOG_FAMILY);
				
				InternalScanner scanner = region.getScanner(scan);
				try {
					List<KeyValue> kvs = new ArrayList<KeyValue>();
					boolean done = false;
					do {
						kvs.clear();
						done = scanner.next(kvs);
						
						System.out.println(">>> keyValues=" + kvs);
					} while (done);
				} finally {
					scanner.close();
				}
			}
			
			if (region.flushcache()) region.compactStores();
			region.close();
			region.getLog().closeAndDelete();
		} finally {
			log.close();
			StoreFile.shutdownBlockCache();
		}
	}
	
	static String getArgStr(String[] args, int idx, String def) { 
		if (args != null && idx >= 0 && idx < args.length)
			return args[idx];
		else
			return def;
	}
	
	static int getArgInt(String[] args, int idx, int def) { 
		if (args != null && idx >= 0 && idx < args.length)
			return Integer.valueOf(args[idx]).intValue();
		else
			return def;
	}
	
}
