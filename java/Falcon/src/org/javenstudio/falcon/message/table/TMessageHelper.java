package org.javenstudio.falcon.message.table;

import org.javenstudio.falcon.datum.table.TableRegionInfo;
import org.javenstudio.falcon.datum.table.store.Bytes;
import org.javenstudio.falcon.datum.table.store.DBRegionInfo;
import org.javenstudio.falcon.datum.table.store.DBTableDescriptor;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.MessageManager;

public final class TMessageHelper {

	/** The message table's name. */
	public static final byte[] MESSAGE_TABLE_NAME = Bytes.toBytes("message");
	public static final byte[] NOTICE_TABLE_NAME = Bytes.toBytes("notice");
	
	static final DBTableDescriptor MESSAGE_TABLEDESC = 
			TMessageTable.createTableDescriptor(MESSAGE_TABLE_NAME);
	
	public static final DBRegionInfo MESSAGE_REGIONINFO = 
			new DBRegionInfo(1000L, MESSAGE_TABLEDESC);
	
	public static final TableRegionInfo MESSAGE_TABLEINFO = 
			new TableRegionInfo(MESSAGE_REGIONINFO);
	
	static final DBTableDescriptor NOTICE_TABLEDESC = 
			TMessageTable.createTableDescriptor(NOTICE_TABLE_NAME);
	
	public static final DBRegionInfo NOTICE_REGIONINFO = 
			new DBRegionInfo(1001L, NOTICE_TABLEDESC);
	
	public static final TableRegionInfo NOTICE_TABLEINFO = 
			new TableRegionInfo(NOTICE_REGIONINFO);
	
	public static TMessageService createMail(MessageManager manager) { 
		return new TMessageService(manager, 
			MessageManager.TYPE_MAIL, MESSAGE_TABLEINFO, new String[] {
				IMessage.INBOX, IMessage.OUTBOX, IMessage.DRAFT, IMessage.TRASH
			});
	}
	
	public static TMessageService createChat(MessageManager manager) { 
		return new TMessageService(manager, 
			MessageManager.TYPE_CHAT, MESSAGE_TABLEINFO, new String[] {
					IMessage.INBOX, IMessage.TRASH
			});
	}
	
	public static TMessageService createNotice(MessageManager manager) { 
		return new TMessageService(manager, 
			MessageManager.TYPE_NOTICE, NOTICE_TABLEINFO, new String[] {
					IMessage.DEFAULT, IMessage.LOGON, IMessage.SYSTEM
			});
	}
	
}
