package org.javenstudio.falcon.user.global;

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

public class UnitTable {
	//private static final Logger LOG = Logger.getLogger(UnitTable.class);

	/** The unit table's name. */
	public static final byte[] UNIT_TABLE_NAME = Bytes.toBytes("unit");
	
	public static final byte[] ATTR_FAMILY = Bytes.toBytes("attr");
	public static final byte[] HEADER_FAMILY = Bytes.toBytes("header");
	public static final byte[] CONTENT_FAMILY = Bytes.toBytes("content");
	
	public static final byte[] KEY_QUALIFIER = Bytes.toBytes("key");
	public static final byte[] NAME_QUALIFIER = Bytes.toBytes("name");
	public static final byte[] TYPE_QUALIFIER = Bytes.toBytes("type");
	public static final byte[] STATUS_QUALIFIER = Bytes.toBytes("status");
	public static final byte[] OWNER_QUALIFIER = Bytes.toBytes("owner");
	public static final byte[] CATEGORY_QUALIFIER = Bytes.toBytes("category");
	
	/** Table descriptor for <code>unit</code> catalog table */
	static final DBTableDescriptor UNIT_TABLEDESC = new DBTableDescriptor(
			UNIT_TABLE_NAME, 
			new DBColumnDescriptor[] {
				new DBColumnDescriptor(ATTR_FAMILY,
					DBConstants.ALL_VERSIONS, 
					Compression.Algorithm.NONE.getName(), 
					false, false, 8 * 1024,
					DBConstants.WEEK_IN_SECONDS, 
					StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				),
				new DBColumnDescriptor(HEADER_FAMILY,
					DBConstants.ALL_VERSIONS, 
					Compression.Algorithm.NONE.getName(), 
					false, false, 8 * 1024,
					DBConstants.WEEK_IN_SECONDS, 
					StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				),
				new DBColumnDescriptor(CONTENT_FAMILY,
					DBConstants.ALL_VERSIONS, 
					Compression.Algorithm.NONE.getName(), 
					false, false, 8 * 1024,
					DBConstants.WEEK_IN_SECONDS, 
					StoreFile.BloomType.NONE.toString(),
					DBConstants.REPLICATION_SCOPE_LOCAL
				)
			});
	
	public static final DBRegionInfo UNIT_REGIONINFO = 
			new DBRegionInfo(2000L, UNIT_TABLEDESC);
	
	static final TableRegionInfo UNIT_TABLEINFO = 
			new TableRegionInfo(UNIT_REGIONINFO);
	
	static void addAttr(IDatabase.Row row, byte[] qualifier, long value) { 
		row.addColumn(ATTR_FAMILY, qualifier, value);
	}
	
	static void addAttr(IDatabase.Row row, byte[] qualifier, String value) { 
		row.addColumn(ATTR_FAMILY, qualifier, value);
	}
	
	static void addHeader(IDatabase.Row row, byte[] qualifier, long value) { 
		row.addColumn(HEADER_FAMILY, qualifier, value);
	}
	
	static void addHeader(IDatabase.Row row, byte[] qualifier, String value) { 
		row.addColumn(HEADER_FAMILY, qualifier, value);
	}
	
	static void addContent(IDatabase.Row row, byte[] qualifier, long value) { 
		row.addColumn(CONTENT_FAMILY, qualifier, value);
	}
	
	static void addContent(IDatabase.Row row, byte[] qualifier, String value) { 
		row.addColumn(CONTENT_FAMILY, qualifier, value);
	}
	
	static long getAttrLong(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnLong(ATTR_FAMILY, qualifier);
	}
	
	static String getAttrString(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnString(ATTR_FAMILY, qualifier);
	}
	
	static long getHeaderLong(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnLong(HEADER_FAMILY, qualifier);
	}
	
	static String getHeaderString(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnString(HEADER_FAMILY, qualifier);
	}
	
	static long getContentLong(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnLong(CONTENT_FAMILY, qualifier);
	}
	
	static String getContentString(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnString(CONTENT_FAMILY, qualifier);
	}
	
	static void saveUnits(UnitManager manager) 
			throws ErrorException {
		if (manager == null) return;
		
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(UNIT_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open unit table");
		}
		
		synchronized (table) {
			table.flush(true);
			//table.close();
		}
	}
	
	static void addUnit(UnitManager manager, Unit item) 
			throws ErrorException { 
		if (manager == null || item == null) return;
		
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(UNIT_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open unit table");
		}
		
		synchronized (table) {
			String key = item.getKey();
			IDatabase.Row row = table.newRow(Bytes.toBytes(key));
			
			item.putFields(row);
			
			table.update(row);
			//table.close();
		}
	}
	
	static IUnit[] loadUnits(UnitManager manager, String type, 
			String category) throws ErrorException { 
		if (manager == null) return null;
		
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(UNIT_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open unit table");
		}
		
		synchronized (table) {
			IDatabase.Query query = table.newQuery();
			query.setRowSize(1000);
			
			if (type != null && type.length() > 0) {
				query.setColumn(IDatabase.MatchOp.EQUAL, 
						new IDatabase.Value(ATTR_FAMILY, TYPE_QUALIFIER, 
								Bytes.toBytes(type))
					);
			}
			
			if (category != null && category.length() > 0) {
				query.setColumn(IDatabase.MatchOp.EQUAL, 
						new IDatabase.Value(ATTR_FAMILY, CATEGORY_QUALIFIER, 
								Bytes.toBytes(category))
					);
			}
			
			ArrayList<IUnit> list = new ArrayList<IUnit>();
			List<IDatabase.Result> results = table.query(query);
			
			if (results != null) { 
				for (IDatabase.Result result : results) { 
					if (result == null) continue;
					IUnit item = toUnit(result);
					if (item != null) list.add(item);
				}
			}
			
			return list.toArray(new IUnit[list.size()]);
		}
	}
	
	static IUnit toUnit(IDatabase.Result result) throws ErrorException { 
		if (result == null) return null;
		
		String key = getAttrString(result, KEY_QUALIFIER);
		String name = getAttrString(result, NAME_QUALIFIER);
		String type = getAttrString(result, TYPE_QUALIFIER);
		
		if (key == null || key.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unit key is empty");
		}
		
		if (name == null || name.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unit name is empty");
		}
		
		if (type == null || type.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unit type is empty");
		}
		
		final Unit item;
		if (type.equals(IUnit.TYPE_GROUP)) {
			item = new GroupUnit(key, name);
		} else if (type.equals(IUnit.TYPE_MEMBER)) {
			item = new MemberUnit(key, name);
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unit type: " + type + " not supported");
		}
		
		item.getFields(result);
		return item;
	}
	
}
