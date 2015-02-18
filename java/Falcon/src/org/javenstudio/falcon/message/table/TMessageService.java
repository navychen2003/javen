package org.javenstudio.falcon.message.table;

import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.table.TableRegionInfo;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.IMessageQuery;
import org.javenstudio.falcon.message.IMessageService;
import org.javenstudio.falcon.message.IMessageSet;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.message.MessageManager;
import org.javenstudio.falcon.message.MessageSender;

final class TMessageService implements IMessageService {
	private static final Logger LOG = Logger.getLogger(TMessageService.class);
	
	private final Set<String> mFolderSet = new HashSet<String>();
	
	private final MessageManager mManager;
	private final TableRegionInfo mTableInfo;
	private final TMessageCache mCache;
	private final String[] mFolderNames;
	private final String mIncomingFolder;
	private final String mType;
	
	public TMessageService(MessageManager manager, 
			String type, TableRegionInfo tableInfo, String[] folderNames) {
		this(manager, type, tableInfo, folderNames, null);
	}
	
	public TMessageService(MessageManager manager, 
			String type, TableRegionInfo tableInfo, String[] folderNames, 
			String incomingFolder) { 
		if (manager == null || type == null || tableInfo == null || 
			folderNames == null || folderNames.length == 0) {
			throw new NullPointerException();
		}
		
		if (incomingFolder == null || incomingFolder.length() == 0)
			incomingFolder = folderNames[0];
		
		mCache = new TMessageCache(this);
		mTableInfo = tableInfo;
		mManager = manager;
		mFolderNames = folderNames;
		mIncomingFolder = incomingFolder;
		mType = type;
		
		for (String name : folderNames) {
			mFolderSet.add(name);
		}
	}
	
	public MessageManager getManager() { return mManager; }
	public TableRegionInfo getTableInfo() { return mTableInfo; }
	public TMessageCache getCache() { return mCache; }
	
	public String getType() { return mType; }
	public String getIncomingFolder() { return mIncomingFolder; }
	
	public String newMessageId(String streamKey) throws ErrorException { 
		if (streamKey == null || streamKey.length() == 0)
			streamKey = MessageHelper.newStreamKey();
		
		return MessageHelper.newMessageId(streamKey, 
				MessageHelper.newMessageKey(), getType());
	}
	
	@Override
	public String[] getFolderNames() { 
		return mFolderNames;
	}
	
	public boolean hasFolderName(String name) {
		if (name == null || name.length() == 0) return false;
		return mFolderSet.contains(name);
	}

	@Override
	public IMessage.Builder newMessage(String folderName, String streamId) 
			throws ErrorException {
		if (!hasFolderName(folderName)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Message folder: " + folderName + " not found");
		}
		
		long current = System.currentTimeMillis();
		
		TMessage message = new TMessage(this, newMessageId(streamId));
		message.setAccount(getManager().getUser().getUserName());
		message.setFolder(folderName);
		message.setFrom(getManager().getUser().getUserName());
		message.setCreatedTime(current);
		message.setMessageTime(current);
		message.setUpdateTime(current);
		
		return new TMessageBuilder(message);
	}
	
	@Override
	public IMessage.Builder modifyMessage(String messageId) throws ErrorException {
		TMessage found = getCache().getMessage(messageId);
		if (found == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Message: " + messageId + " not found");
		}
		
		TMessage message = new TMessage(this, messageId);
		message.setUpdateTime(System.currentTimeMillis());
		
		return new TMessageBuilder(message);
	}
	
	@Override
	public IMessage getMessage(String messageId) throws ErrorException {
		return getCache().getMessage(messageId);
	}
	
	@Override
	public IMessage postSend(IMessage.Builder builder) throws ErrorException {
		if (builder == null) return null;
		
		builder.setFrom(getManager().getUser().getUserName());
		builder.setFolder(IMessage.OUTBOX);
		builder.setStatus(IMessage.STATUS_QUEUED);
		
		if (LOG.isDebugEnabled())
			LOG.debug("postSend: builder=" + builder);
		
		IMessage message = builder.save();
		if (message != null) {
			getManager().getJob().startJob(new MessageSender(message));
			return message;
		}
		
		return null;
	}
	
	@Override
	public IMessage moveMessage(String messageId, String folderTo) 
			throws ErrorException {
		if (messageId == null || folderTo == null)
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("moveMessage: messageId=" + messageId 
					+ " folderTo=" + folderTo);
		}
		
		if (!hasFolderName(folderTo)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Message folder: " + folderTo + " not found");
		}
		
		return getCache().moveMessage(messageId, folderTo);
	}

	@Override
	public IMessage deleteMessage(String messageId) throws ErrorException {
		if (messageId == null) return null;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("deleteMessage: messageId=" + messageId);
		
		return getCache().deleteMessage(messageId);
	}
	
	@Override
	public IMessageSet getMessages(IMessageQuery query) throws ErrorException {
		return getCache().getMessages(query);
	}
	
	@Override
	public IMessageSet getMessages(String streamId, String folderName) 
			throws ErrorException {
		return getCache().getMessages(streamId, folderName);
	}
	
	@Override
	public IMessageSet getNewMessages() throws ErrorException {
		return getCache().getNewMessages();
	}
	
	@Override
	public void flushMessages() throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("flushMessages");
		getCache().flushMessages();
	}
	
	@Override
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close: type=" + mType);
		try {
			flushMessages();
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("close: flush error: " + e, e);
		}
		mCache.close();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() 
				+ "{user=" + getManager().getUser().getUserKey() 
				+ ",type=" + mType + "}";
	}
	
}
