package org.javenstudio.lightning.util;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.datum.table.store.DBLog;
import org.javenstudio.falcon.datum.table.store.DBRegion;
import org.javenstudio.falcon.datum.table.store.DBRegionInfo;
import org.javenstudio.falcon.datum.table.store.InternalScanner;
import org.javenstudio.falcon.datum.table.store.KeyValue;
import org.javenstudio.falcon.datum.table.store.Result;
import org.javenstudio.falcon.datum.table.store.Scan;
import org.javenstudio.falcon.datum.table.store.StoreFile;
import org.javenstudio.falcon.datum.util.TimeUtils;
import org.javenstudio.falcon.message.table.TMessageHelper;
import org.javenstudio.falcon.message.table.TMessageTable;
import org.javenstudio.falcon.user.global.UnitTable;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

public class SimpleScanner extends SimpleShell {

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
		
		if (action.equalsIgnoreCase("-unit")) {
			regionMain(conf, UnitTable.UNIT_REGIONINFO, args2, new UnitResultListener());
		} else if (action.equalsIgnoreCase("-message")) {
			regionMain(conf, TMessageHelper.MESSAGE_REGIONINFO, args2, new MessageResultListener());
		} else if (action.equalsIgnoreCase("-notice")) {
			regionMain(conf, TMessageHelper.NOTICE_REGIONINFO, args2, new MessageResultListener());
		}
	}
	
	static interface ResultListener {
		public void onResult(Result result);
		public void onDone();
	}
	
	static class UnitItem {
		public final String key;
		public final String name;
		public final String type;
		
		public UnitItem(String key, String name, String type) {
			this.key = key;
			this.name = name;
			this.type = type;
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{key=" + key 
					+ ",name=" + name + ",type=" + type + "}";
		}
	}
	
	static class UnitResultListener implements ResultListener {
		private int mCount = 0;
		
		@Override
		public void onResult(Result result) {
			mCount ++;
			
			String key = Bytes.toString(result.getValue(UnitTable.ATTR_FAMILY, UnitTable.KEY_QUALIFIER));
			String name = Bytes.toString(result.getValue(UnitTable.ATTR_FAMILY, UnitTable.NAME_QUALIFIER));
			String type = Bytes.toString(result.getValue(UnitTable.ATTR_FAMILY, UnitTable.TYPE_QUALIFIER));
			
			System.out.println(">>>> onResult: key=" + key + " name=" + name + " type=" + type);
		}
		
		@Override
		public void onDone() {
			System.out.println(">>>> onDone: count=" + mCount);
		}
	}
	
	static class MessageItem {
		public final String key;
		public final String from;
		public final String subject;
		public final long mtime;
		
		public MessageItem(String key, String from, String subject, long mtime) {
			this.key = key;
			this.from = from;
			this.subject = subject;
			this.mtime = mtime;
		}
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{key=" + key 
					+ ",from=" + from + ",subject=" + subject 
					+ ",mtime=" + mtime + " " + TimeUtils.formatDate(mtime) + "}";
		}
	}
	
	static class MessageResultListener implements ResultListener {
		private int mCount = 0;
		private MessageItem mFirst = null;
		private MessageItem mLast = null;
		
		@Override
		public void onResult(Result result) {
			mCount ++;
			
			String key = Bytes.toString(result.getValue(TMessageTable.ATTR_FAMILY, TMessageTable.MESSAGEID_QUALIFIER));
			String from = Bytes.toString(result.getValue(TMessageTable.HEADER_FAMILY, TMessageTable.FROM_QUALIFIER));
			String subject = Bytes.toString(result.getValue(TMessageTable.HEADER_FAMILY, TMessageTable.SUBJECT_QUALIFIER));
			long mtime = Bytes.toLong(result.getValue(TMessageTable.ATTR_FAMILY, TMessageTable.MESSAGETIME_QUALIFIER));
			
			MessageItem item = new MessageItem(key, from, subject, mtime);
			System.out.println(">>>> onResult: " + item);
			
			if (mFirst == null || mFirst.mtime > mtime) mFirst = item;
			if (mLast == null || mLast.mtime < mtime) mLast = item;
		}
		
		@Override
		public void onDone() {
			System.out.println(">>>> onDone: count=" + mCount);
			System.out.println(">>>> onDone: first=" + mFirst);
			System.out.println(">>>> onDone: last=" + mLast);
		}
	}
	
	static void regionMain(Configuration conf, DBRegionInfo regionInfo, 
			String[] args, ResultListener listener) throws Exception {
		final Path tableDir = new Path(args[0]);
		
		final FileSystem fs = FileSystem.getLocal(conf);
		final Path logdir = new Path(conf.get("bigdb.tmp.dir"),
				".logtmp/.dblog." + tableDir.getName() + System.currentTimeMillis()); 
		final Path oldLogDir = new Path(conf.get("bigdb.tmp.dir"),
				".logtmp/.oldlogs");
		final DBLog log = new DBLog(fs, logdir, oldLogDir, conf, null);
		
		try {
			//DBRegionInfo regionInfo = new DBRegionInfo(1L, tableDesc);
			DBRegion region = DBRegion.newDBRegion(tableDir, log, fs, conf, regionInfo, null);
			region.initialize();
			
			if (true) {
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
						
						if (kvs.size() > 0) { 
							KeyValue[] vals = kvs.toArray(new KeyValue[kvs.size()]);
							Result result = new Result(vals);
							listener.onResult(result);
						}
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
			
			listener.onDone();
		}
	}
	
}
