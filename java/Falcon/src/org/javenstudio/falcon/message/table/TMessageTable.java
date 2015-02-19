package org.javenstudio.falcon.message.table;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.store.Bytes;
import org.javenstudio.falcon.datum.table.store.Compression;
import org.javenstudio.falcon.datum.table.store.DBColumnDescriptor;
import org.javenstudio.falcon.datum.table.store.DBConstants;
import org.javenstudio.falcon.datum.table.store.DBTableDescriptor;
import org.javenstudio.falcon.datum.table.store.StoreFile;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.message.MessageManager;

public final class TMessageTable {
	//private static final Logger LOG = Logger.getLogger(TMessageTable.class);
	
	public static final byte[] ATTR_FAMILY = Bytes.toBytes("attr");
	public static final byte[] HEADER_FAMILY = Bytes.toBytes("header");
	public static final byte[] CONTENT_FAMILY = Bytes.toBytes("content");
	
	public static final byte[] FROM_QUALIFIER = Bytes.toBytes("from");
	public static final byte[] TO_QUALIFIER = Bytes.toBytes("to");
	public static final byte[] CC_QUALIFIER = Bytes.toBytes("cc");
	public static final byte[] BCC_QUALIFIER = Bytes.toBytes("bcc");
	public static final byte[] REPLYTO_QUALIFIER = Bytes.toBytes("replyto");
	public static final byte[] SUBJECT_QUALIFIER = Bytes.toBytes("subject");
	public static final byte[] CONTENTTYPE_QUALIFIER = Bytes.toBytes("contenttype");
	public static final byte[] HEADERS_QUALIFIER = Bytes.toBytes("headers");
	
	public static final byte[] REPLYID_QUALIFIER = Bytes.toBytes("replyid");
	public static final byte[] STREAMID_QUALIFIER = Bytes.toBytes("streamid");
	public static final byte[] MESSAGEID_QUALIFIER = Bytes.toBytes("messageid");
	public static final byte[] MESSAGETYPE_QUALIFIER = Bytes.toBytes("messagetype");
	public static final byte[] MESSAGETIME_QUALIFIER = Bytes.toBytes("messagetime");
	public static final byte[] CREATEDTIME_QUALIFIER = Bytes.toBytes("createdtime");
	public static final byte[] UPDATETIME_QUALIFIER = Bytes.toBytes("updatetime");
	
	public static final byte[] ACCOUNT_QUALIFIER = Bytes.toBytes("account");
	public static final byte[] FLAG_QUALIFIER = Bytes.toBytes("flag");
	public static final byte[] STATUS_QUALIFIER = Bytes.toBytes("status");
	public static final byte[] TYPE_QUALIFIER = Bytes.toBytes("type");
	public static final byte[] FOLDER_QUALIFIER = Bytes.toBytes("folder");
	public static final byte[] FOLDERFROM_QUALIFIER = Bytes.toBytes("folderfrom");
	
	public static final byte[] BODY_QUALIFIER = Bytes.toBytes("body");
	public static final byte[] SOURCE_QUALIFIER = Bytes.toBytes("source");
	public static final byte[] ATTACHMENTS_QUALIFIER = Bytes.toBytes("attachments");
	
	/** Table descriptor for <code>message</code> catalog table */
	static final DBTableDescriptor createTableDescriptor(byte[] tableName) {
		return new DBTableDescriptor(tableName, 
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
	}
	
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
	
	static void flushMessages(TMessageService service) 
			throws ErrorException {
		if (service == null) throw new NullPointerException();
		
		MessageManager manager = service.getManager();
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		IDatabase.Table table = database.getTable(service.getTableInfo());
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open message table");
		}
		
		synchronized (table) {
			table.flush(true);
			//table.close();
		}
	}
	
	static void deleteMessage(TMessage message) throws ErrorException { 
		if (message == null) throw new NullPointerException();
		
		MessageManager manager = message.getService().getManager();
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		IDatabase.Table table = database.getTable(message.getService().getTableInfo());
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open message table");
		}
		
		synchronized (table) {
			String messageId = message.getMessageId();
			IDatabase.Row row = table.newRow(Bytes.toBytes(messageId));
			
			table.delete(row);
			//table.close();
		}
	}
	
	static void moveMessage(TMessage message, String folderTo) 
			throws ErrorException { 
		if (message == null || folderTo == null) 
			throw new NullPointerException();
		
		if (!message.getService().hasFolderName(folderTo)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Message folder: " + folderTo + " not found");
		}
		
		String folderName = message.getFolder();
		if (folderName == null) folderName = "";
		
		if (folderName.equals(folderTo)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Message folder: " + folderTo + " not changed");
		}
		
		MessageManager manager = message.getService().getManager();
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		IDatabase.Table table = database.getTable(message.getService().getTableInfo());
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open message table");
		}
		
		synchronized (table) {
			String messageId = message.getMessageId();
			IDatabase.Row row = table.newRow(Bytes.toBytes(messageId));
			
			addAttr(row, FOLDER_QUALIFIER, folderTo);
			addAttr(row, FOLDERFROM_QUALIFIER, folderName);
			addAttr(row, UPDATETIME_QUALIFIER, System.currentTimeMillis());
			
			table.update(row);
			//table.close();
		}
	}
	
	static void saveMessage(TMessage message) throws ErrorException { 
		if (message == null) throw new NullPointerException();
		
		MessageManager manager = message.getService().getManager();
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		IDatabase.Table table = database.getTable(message.getService().getTableInfo());
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open message table");
		}
		
		synchronized (table) {
			String messageId = message.getMessageId();
			IDatabase.Row row = table.newRow(Bytes.toBytes(messageId));
			
			long messageTime = message.getMessageTime();
			long createdTime = message.getCreatedTime();
			long updateTime = message.getUpdateTime();
			
			if (messageTime > 0) addAttr(row, MESSAGETIME_QUALIFIER, messageTime);
			if (createdTime > 0) addAttr(row, CREATEDTIME_QUALIFIER, createdTime);
			if (updateTime > 0) addAttr(row, UPDATETIME_QUALIFIER, updateTime);
			
			addAttr(row, MESSAGEID_QUALIFIER, messageId);
			addAttr(row, STREAMID_QUALIFIER, message.getStreamId());
			addAttr(row, REPLYID_QUALIFIER, message.getReplyId());
			addAttr(row, MESSAGETYPE_QUALIFIER, message.getMessageType());
			
			addAttr(row, ACCOUNT_QUALIFIER, message.getAccount());
			addAttr(row, FLAG_QUALIFIER, message.getFlag());
			addAttr(row, STATUS_QUALIFIER, message.getStatus());
			addAttr(row, TYPE_QUALIFIER, message.getService().getType());
			addAttr(row, FOLDER_QUALIFIER, message.getFolder());
			addAttr(row, FOLDERFROM_QUALIFIER, message.getFolderFrom());
			
			addHeader(row, FROM_QUALIFIER, message.getFrom());
			addHeader(row, TO_QUALIFIER, message.getTo());
			addHeader(row, CC_QUALIFIER, message.getCc());
			addHeader(row, BCC_QUALIFIER, message.getBcc());
			addHeader(row, REPLYTO_QUALIFIER, message.getReplyTo());
			addHeader(row, SUBJECT_QUALIFIER, message.getSubject());
			addHeader(row, CONTENTTYPE_QUALIFIER, message.getContentType());
			addHeader(row, HEADERS_QUALIFIER, message.getHeaderLines());
			
			addContent(row, BODY_QUALIFIER, message.getBody());
			addContent(row, SOURCE_QUALIFIER, message.getSourceFile());
			addContent(row, ATTACHMENTS_QUALIFIER, MessageHelper.combineValues(
					message.getAttachmentFiles()));
			
			table.update(row);
			//table.close();
		}
	}
	
	static TMessage getMessage(TMessageService service, String messageId) 
			throws ErrorException {
		if (service == null || messageId == null) 
			throw new NullPointerException();
		
		MessageManager manager = service.getManager();
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		IDatabase.Table table = database.getTable(service.getTableInfo());
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open message table");
		}
		
		synchronized (table) {
			IDatabase.Query query = table.newQuery();
			query.setRow(Bytes.toBytes(messageId));
			
			IDatabase.Result result = table.get(query);
			if (result != null)
				return toMessage(service, result);
		}
		
		return null;
	}
	
	static interface Collector {
		public void addMessage(TMessage message) throws ErrorException;
	}
	
	static void scanMessages(TMessageService service, String account, 
			String folderName, String fromVal, String toVal, String streamId, 
			String status, String flag, int rowSize, long maxStamp, long minStamp, 
			Collector collector) throws ErrorException {
		if (service == null || collector == null) throw new NullPointerException();
		if (rowSize <= 0) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Message row size: " + rowSize + " must > 0");
		}
		
		MessageManager manager = service.getManager();
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		IDatabase.Table table = database.getTable(service.getTableInfo());
		if (table == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not open message table");
		}
		
		synchronized (table) {
			ArrayList<IDatabase.Value> values = new ArrayList<IDatabase.Value>();
			values.add(new IDatabase.Value(ATTR_FAMILY, TYPE_QUALIFIER, 
					Bytes.toBytes(service.getType())));
			
			if (account != null && account.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, ACCOUNT_QUALIFIER, 
						Bytes.toBytes(account)));
			}
			
			if (folderName != null && folderName.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, FOLDER_QUALIFIER, 
						Bytes.toBytes(folderName)));
			}
			
			if (streamId != null && streamId.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, STREAMID_QUALIFIER, 
						Bytes.toBytes(streamId)));
			}
			
			if (status != null && status.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, STATUS_QUALIFIER, 
						Bytes.toBytes(status)));
			}
			
			if (flag != null && flag.length() > 0) {
				values.add(new IDatabase.Value(ATTR_FAMILY, FLAG_QUALIFIER, 
						Bytes.toBytes(flag)));
			}
			
			if (fromVal != null && fromVal.length() > 0) {
				values.add(new IDatabase.Value(HEADER_FAMILY, FROM_QUALIFIER, 
						Bytes.toBytes(fromVal)));
			}
			
			if (toVal != null && toVal.length() > 0) {
				values.add(new IDatabase.Value(HEADER_FAMILY, TO_QUALIFIER, 
						Bytes.toBytes(toVal)));
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
					TMessage message = toMessage(service, result);
					if (message != null)
						collector.addMessage(message);
				}
			}
		}
	}
	
	static TMessage toMessage(TMessageService service, 
			IDatabase.Result result) throws ErrorException { 
		if (service == null || result == null) 
			return null;
		
		String messageId = getAttrString(result, MESSAGEID_QUALIFIER);
		if (messageId == null || messageId.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Message Id is empty");
		}
		
		String replyId = getAttrString(result, REPLYID_QUALIFIER);
		String streamId = getAttrString(result, STREAMID_QUALIFIER);
		String messageType = getAttrString(result, MESSAGETYPE_QUALIFIER);
		
		long messageTime = getAttrLong(result, MESSAGETIME_QUALIFIER);
		long createdTime = getAttrLong(result, CREATEDTIME_QUALIFIER);
		long updateTime = getAttrLong(result, UPDATETIME_QUALIFIER);
		
		String account = getAttrString(result, ACCOUNT_QUALIFIER);
		String flag = getAttrString(result, FLAG_QUALIFIER);
		String status = getAttrString(result, STATUS_QUALIFIER);
		String type = getAttrString(result, TYPE_QUALIFIER);
		String folderName = getAttrString(result, FOLDER_QUALIFIER);
		String folderFrom = getAttrString(result, FOLDERFROM_QUALIFIER);
		
		String from = getHeaderString(result, FROM_QUALIFIER);
		String to = getHeaderString(result, TO_QUALIFIER);
		String cc = getHeaderString(result, CC_QUALIFIER);
		String bcc = getHeaderString(result, BCC_QUALIFIER);
		String replyTo = getHeaderString(result, REPLYTO_QUALIFIER);
		String subject = getHeaderString(result, SUBJECT_QUALIFIER);
		String contentType = getHeaderString(result, CONTENTTYPE_QUALIFIER);
		String headers = getHeaderString(result, HEADERS_QUALIFIER);
		
		String body = getContentString(result, BODY_QUALIFIER);
		String source = getContentString(result, SOURCE_QUALIFIER);
		String attachments = getContentString(result, ATTACHMENTS_QUALIFIER);
		
		if (type == null || !type.equals(service.getType())) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Message: " + messageId + " has wrong type: " + type);
		}
		
		if (!service.hasFolderName(folderName)) { 
			//throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
			//		"Message: " + messageId + " has wrong folder: " + folderName);
			
			folderName = IMessage.TRASH;
		}
		
		TMessage message = new TMessage(service, messageId);
		message.setCreatedTime(createdTime);
		message.setUpdateTime(updateTime);
		message.setMessageTime(messageTime);
		message.setMessageType(messageType);
		message.setAccount(account);
		message.setStreamId(streamId);
		message.setReplyId(replyId);
		message.setFolder(folderName);
		message.setFolderFrom(folderFrom);
		message.setFlag(flag);
		message.setStatus(status);
		message.setFrom(from);
		message.setTo(to);
		message.setCc(cc);
		message.setBcc(bcc);
		message.setReplyTo(replyTo);
		message.setHeaderLines(headers);
		message.setSubject(subject);
		message.setContentType(contentType);
		message.setBody(body);
		message.setSourceFile(source);
		message.setAttachmentFiles(MessageHelper.splitValues(attachments));
		
		return message;
	}
	
}
