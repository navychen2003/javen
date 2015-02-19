package org.javenstudio.falcon.publication.table;

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
import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.PublicationManager;

final class TPublicationTable {
	//private static final Logger LOG = Logger.getLogger(TPublicationTable.class);

	/** The publication table's name. */
	static final byte[] PUBLICATION_TABLE_NAME = Bytes.toBytes("publication");
	
	static final byte[] ATTR_FAMILY = Bytes.toBytes("attr");
	static final byte[] HEADER_FAMILY = Bytes.toBytes("header");
	static final byte[] CONTENT_FAMILY = Bytes.toBytes("content");
	
	/** Table descriptor for <code>publication</code> catalog table */
	static final DBTableDescriptor PUBLICATION_TABLEDESC = new DBTableDescriptor(
			PUBLICATION_TABLE_NAME, 
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
	
	static final DBRegionInfo PUBLICATION_REGIONINFO = 
			new DBRegionInfo(2000L, PUBLICATION_TABLEDESC);
	
	static final TableRegionInfo PUBLICATION_TABLEINFO = 
			new TableRegionInfo(PUBLICATION_REGIONINFO);
	
	static void addColumn(IDatabase.Row row, byte[] family, TNameValue<?> nameVal) {
		if (row == null || family == null || nameVal == null)
			throw new NullPointerException();
		
		TNameType type = nameVal.getType();
		Class<?> clazz = type.getValueClass();
		if (clazz == Integer.class) {
			row.addColumn(family, type.getNameBytes(), nameVal.getInt());
		} else if (clazz == Long.class) {
			row.addColumn(family, type.getNameBytes(), nameVal.getLong());
		} else if (clazz == Float.class) {
			row.addColumn(family, type.getNameBytes(), nameVal.getFloat());
		} else if (clazz == Boolean.class) {
			row.addColumn(family, type.getNameBytes(), nameVal.getBool());
		} else if (clazz == String.class) {
			row.addColumn(family, type.getNameBytes(), nameVal.getString());
		} else if (clazz == byte[].class) {
			row.addColumn(family, type.getNameBytes(), nameVal.getBytes());
		} else {
			throw new IllegalArgumentException("Unsupported value class: " + clazz);
		}
	}
	
	static Object getColumn(IDatabase.Result res, byte[] family, TNameType type) {
		if (res == null || family == null || type == null)
			throw new NullPointerException();
		
		Class<?> clazz = type.getValueClass();
		if (clazz == Integer.class) {
			return res.getColumnInt(family, type.getNameBytes());
		} else if (clazz == Long.class) {
			return res.getColumnLong(family, type.getNameBytes());
		} else if (clazz == Float.class) {
			return res.getColumnFloat(family, type.getNameBytes());
		} else if (clazz == Boolean.class) {
			return res.getColumnBool(family, type.getNameBytes());
		} else if (clazz == String.class) {
			return res.getColumnString(family, type.getNameBytes());
		} else if (clazz == byte[].class) {
			return res.getColumn(family, type.getNameBytes());
		} else {
			throw new IllegalArgumentException("Unsupported value class: " + clazz);
		}
	}
	
	static void addAttr(IDatabase.Row row, TNameValue<?> nameVal) { 
		addColumn(row, ATTR_FAMILY, nameVal);
	}
	
	static Object getAttr(IDatabase.Result res, TNameType type) {
		return getColumn(res, ATTR_FAMILY, type);
	}
	
	static void addHeader(IDatabase.Row row, TNameValue<?> nameVal) { 
		addColumn(row, HEADER_FAMILY, nameVal);
	}
	
	static Object getHeader(IDatabase.Result res, TNameType type) {
		return getColumn(res, HEADER_FAMILY, type);
	}
	
	static void addContent(IDatabase.Row row, TNameValue<?> nameVal) { 
		addColumn(row, CONTENT_FAMILY, nameVal);
	}
	
	static Object getContent(IDatabase.Result res, TNameType type) {
		return getColumn(res, CONTENT_FAMILY, type);
	}
	
	static void addAttr(IDatabase.Row row, byte[] qualifier, long value) { 
		row.addColumn(ATTR_FAMILY, qualifier, value);
	}
	
	static void addAttr(IDatabase.Row row, byte[] qualifier, String value) { 
		row.addColumn(ATTR_FAMILY, qualifier, value);
	}
	
	static String getAttrString(IDatabase.Result res, byte[] qualifier) {
		return res.getColumnString(ATTR_FAMILY, qualifier);
	}
	
	static void flushPublications(TPublicationService service) 
			throws ErrorException {
		if (service == null) throw new NullPointerException();
		
		PublicationManager manager = service.getManager();
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(PUBLICATION_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open publication table");
		}
		
		synchronized (table) {
			table.flush(true);
			//table.close();
		}
	}
	
	static void deletePublication(TPublication publication) throws ErrorException { 
		if (publication == null) throw new NullPointerException();
		
		PublicationManager manager = publication.getService().getManager();
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(PUBLICATION_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open publication table");
		}
		
		synchronized (table) {
			String publishId = publication.getId();
			IDatabase.Row row = table.newRow(Bytes.toBytes(publishId));
			
			table.delete(row);
			//table.close();
		}
	}
	
	static void movePublication(TPublication publication, String channelTo) 
			throws ErrorException { 
		if (publication == null || channelTo == null) 
			throw new NullPointerException();
		
		if (!publication.getService().hasChannelName(channelTo)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Publication channel: " + channelTo + " not found");
		}
		
		String channelName = publication.getAttrString(IPublication.ATTR_CHANNEL);
		if (channelName == null) channelName = "";
		
		if (channelName.equals(channelTo)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Publication channel: " + channelTo + " not changed");
		}
		
		PublicationManager manager = publication.getService().getManager();
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(PUBLICATION_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open publication table");
		}
		
		synchronized (table) {
			String publishId = publication.getId();
			IDatabase.Row row = table.newRow(Bytes.toBytes(publishId));
			
			addAttr(row, TNameType.ATTR_CHANNEL.getNameBytes(), channelTo);
			addAttr(row, TNameType.ATTR_CHANNELFROM.getNameBytes(), channelName);
			addAttr(row, TNameType.ATTR_UPDATETIME.getNameBytes(), System.currentTimeMillis());
			
			table.update(row);
			//table.close();
		}
	}
	
	@SuppressWarnings("unused")
	static void savePublication(TPublication publication) throws ErrorException { 
		if (publication == null) throw new NullPointerException();
		
		PublicationManager manager = publication.getService().getManager();
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(PUBLICATION_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open publication table");
		}
		
		synchronized (table) {
			String publishId = publication.getId();
			IDatabase.Row row = table.newRow(Bytes.toBytes(publishId));
			
			TNameValue<?>[] attrs = publication.getAttrs();
			boolean foundId = false, foundType = false;
			boolean foundChannel = false, foundStream = false;
			
			if (attrs != null) { 
				for (TNameValue<?> nameVal : attrs) {
					if (nameVal == null) continue;
					addAttr(row, nameVal);
					
					final TNameType type = nameVal.getType();
					if (type == TNameType.ATTR_PUBLISHID) 
						foundId = true;
					else if (type == TNameType.ATTR_SERVICETYPE)
						foundType = true;
					else if (type == TNameType.ATTR_CHANNEL)
						foundChannel = true;
					else if (type == TNameType.ATTR_STREAMID)
						foundStream = true;
				}
			}
			
			TNameValue<?>[] headers = publication.getHeaders();
			if (headers != null) { 
				for (TNameValue<?> nameVal : headers) {
					if (nameVal == null) continue;
					addHeader(row, nameVal);
				}
			}
			
			TNameValue<?>[] contents = publication.getContents();
			if (contents != null) { 
				for (TNameValue<?> nameVal : contents) {
					if (nameVal == null) continue;
					addContent(row, nameVal);
				}
			}
			
			//if (foundId == false) {
			//	addAttr(row, TNameType.ATTR_PUBLISHID.getNameBytes(), 
			//			publication.getId());
			//}
			//if (foundType == false) {
			//	addAttr(row, TNameType.ATTR_SERVICETYPE.getNameBytes(), 
			//			service.getType());
			//}
			//if (foundChannel == false) {
			//	addAttr(row, TNameType.ATTR_CHANNEL.getNameBytes(), 
			//			IPublication.DRAFT);
			//}
			//if (foundStream == false) {
			//	addAttr(row, TNameType.ATTR_STREAMID.getNameBytes(), 
			//			PublicationHelper.getStreamKey(publication.getId()));
			//}
			
			table.update(row);
			//table.close();
		}
	}
	
	static TPublication getPublication(TPublicationService service, 
			String publishId) throws ErrorException {
		if (service == null || publishId == null) 
			throw new NullPointerException();
		
		PublicationManager manager = service.getManager();
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(PUBLICATION_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open publication table");
		}
		
		synchronized (table) {
			IDatabase.Query query = table.newQuery();
			query.setRow(Bytes.toBytes(publishId));
			
			IDatabase.Result result = table.get(query);
			if (result != null)
				return toPublication(service, result);
		}
		
		return null;
	}
	
	static interface Collector {
		public void addPublication(TPublication publication) throws ErrorException;
	}
	
	static void scanPublications(TPublicationService service, String owner, 
			String channelName, String streamId, String status, String flag, 
			int rowSize, long maxStamp, long minStamp, 
			Collector collector) throws ErrorException {
		if (service == null || collector == null) throw new NullPointerException();
		if (rowSize <= 0) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Publication row size: " + rowSize + " must > 0");
		}
		
		PublicationManager manager = service.getManager();
		IDatabase database = manager.getStore().getDatabase();
		IDatabase.Table table = database.getTable(PUBLICATION_TABLEINFO);
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open publication table");
		}
		
		synchronized (table) {
			ArrayList<IDatabase.Value> values = new ArrayList<IDatabase.Value>();
			if (service != null) {
				values.add(new IDatabase.Value(ATTR_FAMILY, 
						TNameType.ATTR_SERVICETYPE.getNameBytes(), 
						Bytes.toBytes(service.getType())));
			}
			
			if (owner != null && owner.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, 
						TNameType.ATTR_OWNER.getNameBytes(), 
						Bytes.toBytes(owner)));
			}
			
			if (channelName != null && channelName.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, 
						TNameType.ATTR_CHANNEL.getNameBytes(), 
						Bytes.toBytes(channelName)));
			}
			
			if (streamId != null && streamId.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, 
						TNameType.ATTR_STREAMID.getNameBytes(), 
						Bytes.toBytes(streamId)));
			}
			
			if (status != null && status.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, 
						TNameType.ATTR_STATUS.getNameBytes(), 
						Bytes.toBytes(status)));
			}
			
			if (flag != null && flag.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, 
						TNameType.ATTR_FLAG.getNameBytes(), 
						Bytes.toBytes(flag)));
			}
			
			IDatabase.Query query = table.newQuery();
			query.setRowSize(rowSize);
			query.setColumn(IDatabase.MatchOp.EQUAL, 
					values.toArray(new IDatabase.Value[values.size()])
				);
			
			if (minStamp > 0 && maxStamp > minStamp) 
				query.setTimeRange(minStamp, maxStamp);
			
			List<IDatabase.Result> results = table.query(query);
			
			if (results != null) { 
				for (IDatabase.Result result : results) { 
					if (result == null) continue;
					TPublication publication = toPublication(service, result);
					if (publication != null)
						collector.addPublication(publication);
				}
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static TPublication toPublication(TPublicationService service, 
			IDatabase.Result result) throws ErrorException { 
		if (service == null || result == null) 
			return null;
		
		String publishId = getAttrString(result, TNameType.ATTR_PUBLISHID.getNameBytes());
		String serviceType = getAttrString(result, TNameType.ATTR_SERVICETYPE.getNameBytes());
		
		if (publishId == null || publishId.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Publication Id is empty");
		}
		
		if (serviceType == null || !serviceType.equals(service.getType())) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Publication: " + publishId + " has wrong type: " + serviceType);
		}
		
		TPublication publication = new TPublication(service, publishId);
		
		TNameType[] attrTypes = service.getAttrTypes();
		if (attrTypes != null) {
			for (TNameType type : attrTypes) {
				if (type == null) continue;
				Object val = getAttr(result, type);
				if (val != null)
					publication.setAttr(new TNameValue(type, val));
			}
		}
		
		TNameType[] headerTypes = service.getHeaderTypes();
		if (headerTypes != null) {
			for (TNameType type : headerTypes) {
				if (type == null) continue;
				Object val = getHeader(result, type);
				if (val != null)
					publication.setHeader(new TNameValue(type, val));
			}
		}
		
		TNameType[] contentTypes = service.getContentTypes();
		if (contentTypes != null) {
			for (TNameType type : contentTypes) {
				if (type == null) continue;
				Object val = getContent(result, type);
				if (val != null)
					publication.setContent(new TNameValue(type, val));
			}
		}
		
		return publication;
	}
	
}
