package org.javenstudio.falcon.setting;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.TableRegionInfo;
import org.javenstudio.falcon.datum.table.store.Bytes;
import org.javenstudio.falcon.datum.table.store.Compression;
import org.javenstudio.falcon.datum.table.store.DBColumnDescriptor;
import org.javenstudio.falcon.datum.table.store.DBConstants;
import org.javenstudio.falcon.datum.table.store.DBRegionInfo;
import org.javenstudio.falcon.datum.table.store.DBTableDescriptor;
import org.javenstudio.falcon.datum.table.store.StoreFile;

public class SettingTable {
	//private static final Logger LOG = Logger.getLogger(SettingTable.class);

	/** The setting table's name. */
	public static final byte[] SETTING_TABLE_NAME = Bytes.toBytes("setting");
	
	public static final byte[] ATTR_FAMILY = Bytes.toBytes("attr");
	public static final byte[] HEADER_FAMILY = Bytes.toBytes("header");
	public static final byte[] CONTENT_FAMILY = Bytes.toBytes("content");
	
	public static final byte[] KEY_QUALIFIER = Bytes.toBytes("key");
	public static final byte[] NAME_QUALIFIER = Bytes.toBytes("name");
	public static final byte[] TYPE_QUALIFIER = Bytes.toBytes("type");
	public static final byte[] CATEGORY_QUALIFIER = Bytes.toBytes("category");
	public static final byte[] STATUS_QUALIFIER = Bytes.toBytes("status");
	public static final byte[] ACTION_QUALIFIER = Bytes.toBytes("action");
	public static final byte[] VERSION_QUALIFIER = Bytes.toBytes("version");
	public static final byte[] LANG_QUALIFIER = Bytes.toBytes("lang");
	public static final byte[] CLIENTKEY_QUALIFIER = Bytes.toBytes("clientkey");
	public static final byte[] AUTHKEY_QUALIFIER = Bytes.toBytes("authkey");
	public static final byte[] AGENT_QUALIFIER = Bytes.toBytes("agent");
	public static final byte[] IPADDR_QUALIFIER = Bytes.toBytes("ipaddr");
	public static final byte[] MTIME_QUALIFIER = Bytes.toBytes("mtime");
	
	/** Table descriptor for <code>setting</code> catalog table */
	static final DBTableDescriptor SETTING_TABLEDESC = new DBTableDescriptor(
			SETTING_TABLE_NAME, 
			new DBColumnDescriptor[] {
				new DBColumnDescriptor(ATTR_FAMILY,
					DBConstants.ALL_VERSIONS, 
					Compression.Algorithm.NONE.getName(), 
					false, false, 8 * 1024,
					DBConstants.FOREVER, 
					StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				),
				new DBColumnDescriptor(HEADER_FAMILY,
					DBConstants.ALL_VERSIONS, 
					Compression.Algorithm.NONE.getName(), 
					false, false, 8 * 1024,
					DBConstants.FOREVER, 
					StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				),
				new DBColumnDescriptor(CONTENT_FAMILY,
					DBConstants.ALL_VERSIONS, 
					Compression.Algorithm.NONE.getName(), 
					false, false, 8 * 1024,
					DBConstants.FOREVER, 
					StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				)
			});
	
	public static final DBRegionInfo SETTING_REGIONINFO = 
			new DBRegionInfo(3000L, SETTING_TABLEDESC);
	
	public static final TableRegionInfo SETTING_TABLEINFO = 
			new TableRegionInfo(SETTING_REGIONINFO);
	
	public static void addAttr(IDatabase.Row row, byte[] qualifier, long value) { 
		row.addColumn(ATTR_FAMILY, qualifier, value);
	}
	
	public static void addAttr(IDatabase.Row row, byte[] qualifier, String value) { 
		row.addColumn(ATTR_FAMILY, qualifier, value);
	}
	
	public static void addHeader(IDatabase.Row row, byte[] qualifier, long value) { 
		row.addColumn(HEADER_FAMILY, qualifier, value);
	}
	
	public static void addHeader(IDatabase.Row row, byte[] qualifier, String value) { 
		row.addColumn(HEADER_FAMILY, qualifier, value);
	}
	
	public static void addContent(IDatabase.Row row, byte[] qualifier, long value) { 
		row.addColumn(CONTENT_FAMILY, qualifier, value);
	}
	
	public static void addContent(IDatabase.Row row, byte[] qualifier, String value) { 
		row.addColumn(CONTENT_FAMILY, qualifier, value);
	}
	
	public static long getAttrLong(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnLong(ATTR_FAMILY, qualifier);
	}
	
	public static String getAttrString(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnString(ATTR_FAMILY, qualifier);
	}
	
	public static long getHeaderLong(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnLong(HEADER_FAMILY, qualifier);
	}
	
	public static String getHeaderString(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnString(HEADER_FAMILY, qualifier);
	}
	
	public static long getContentLong(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnLong(CONTENT_FAMILY, qualifier);
	}
	
	public static String getContentString(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnString(CONTENT_FAMILY, qualifier);
	}
	
	public static interface SettingRow {
		public String getKey();
		public void putFields(IDatabase.Row row) throws ErrorException;
		public void getFields(IDatabase.Result res) throws ErrorException;
	}
	
	public static interface SettingRowFactory { 
		public SettingRow createRow(IDatabase.Result res) throws ErrorException;
	}
	
	public static void saveSettings(IDatabase database) 
			throws ErrorException {
		if (database == null) return;
		
		IDatabase.Table table = database.getTable(SETTING_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open setting table");
		}
		
		table.flush(true);
	}
	
	public static void addSetting(IDatabase database, SettingRow item) 
			throws ErrorException { 
		if (database == null || item == null) return;
		
		IDatabase.Table table = database.getTable(SETTING_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open setting table");
		}
		
		String key = item.getKey();
		IDatabase.Row row = table.newRow(Bytes.toBytes(key));
		
		item.putFields(row);
		table.update(row);
	}
	
	public static SettingRow[] loadSettings(IDatabase database, String type, 
			SettingRowFactory factory) throws ErrorException { 
		if (database == null || factory == null) 
			throw new NullPointerException();
		
		IDatabase.Table table = database.getTable(SETTING_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open setting table");
		}
		
		IDatabase.Query query = table.newQuery();
		query.setRowSize(1000);
		
		if (type != null && type.length() > 0) {
			query.setColumn(IDatabase.MatchOp.EQUAL, 
					new IDatabase.Value(ATTR_FAMILY, TYPE_QUALIFIER, 
							Bytes.toBytes(type))
				);
		}
		
		ArrayList<SettingRow> list = new ArrayList<SettingRow>();
		List<IDatabase.Result> results = table.query(query);
		
		if (results != null) { 
			for (IDatabase.Result result : results) { 
				if (result == null) continue;
				SettingRow item = factory.createRow(result);
				if (item != null) list.add(item);
			}
		}
		
		return list.toArray(new SettingRow[list.size()]);
	}
	
	public static SettingRow loadSetting(IDatabase database, String key, 
			SettingRowFactory factory) throws ErrorException { 
		if (database == null || factory == null || key == null) 
			throw new NullPointerException();
		
		IDatabase.Table table = database.getTable(SETTING_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open setting table");
		}
		
		IDatabase.Query query = table.newQuery();
		query.setRow(Bytes.toBytes(key));
		
		IDatabase.Result result = table.get(query);
		if (result != null)
			return factory.createRow(result);
		
		return null;
	}
	
}
