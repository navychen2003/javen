package org.javenstudio.falcon.message.table;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.IMessageQuery;
import org.javenstudio.falcon.message.IMessageSet;
import org.javenstudio.falcon.message.MessageQuery;

final class TMessageCache {
	private static final Logger LOG = Logger.getLogger(TMessageCache.class);

	private final TMessageService mService;
	private final TMessageStream mStreams = new TMessageStream();
	
	private final Map<String,WeakReference<TMessage>> mMessages = 
			new HashMap<String,WeakReference<TMessage>>();
	
	private TMessageSet mNewMessages = null;
	
	public TMessageCache(TMessageService service) {
		if (service == null) throw new NullPointerException();
		mService = service;
	}
	
	public TMessageService getService() { return mService; }
	
	private void put(TMessage message) {
		if (message == null) return;
		
		synchronized (mMessages) {
			mMessages.put(message.getMessageId(), 
					new WeakReference<TMessage>(message));
			mStreams.putMessage(message);
		}
	}
	
	private TMessage get(String messageId) {
		if (messageId == null) return null;
		
		synchronized (mMessages) {
			WeakReference<TMessage> ref = mMessages.get(messageId);
			return ref != null ? ref.get() : null;
		}
	}
	
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		synchronized (mMessages) {
			mMessages.clear();
		}
	}
	
	public void flushMessages() throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("flushMessages");
		
		TMessageTable.flushMessages(getService());
	}
	
	public TMessage getMessage(String messageId) throws ErrorException {
		if (messageId == null) throw new NullPointerException();
		
		synchronized (mMessages) {
			TMessage message = get(messageId);
			if (message == null) {
				if (LOG.isDebugEnabled())
					LOG.debug("getMessage: load message: " + messageId);
				
				message = TMessageTable.getMessage(getService(), messageId);
				if (message != null) put(message);
			}
			
			return message;
		}
	}
	
	public TMessage deleteMessage(String messageId) throws ErrorException {
		if (messageId == null) throw new NullPointerException();
		
		synchronized (mMessages) {
			TMessage message = getMessage(messageId);
			if (message != null) {
				if (LOG.isDebugEnabled())
					LOG.debug("deleteMessage: delete message: " + messageId);
				
				TMessageTable.deleteMessage(message);
				mMessages.remove(message.getMessageId());
				mStreams.removeMessage(message);
				mNewMessages = null;
			}
			
			return message;
		}
	}
	
	public TMessage moveMessage(String messageId, String folderTo) 
			throws ErrorException {
		if (messageId == null || folderTo == null) 
			throw new NullPointerException();
		
		synchronized (mMessages) {
			TMessage message = getMessage(messageId);
			if (message != null) {
				if (LOG.isDebugEnabled())
					LOG.debug("moveMessage: move message: " + messageId);
				
				TMessageTable.moveMessage(message, folderTo);
				mMessages.remove(message.getMessageId());
				
				message = getMessage(messageId);
				mStreams.putMessage(message);
				
				mNewMessages = null;
			}
			
			return message;
		}
	}
	
	public TMessage saveMessage(TMessage message) throws ErrorException {
		if (message == null) throw new NullPointerException();
		
		synchronized (mMessages) {
			if (LOG.isDebugEnabled())
				LOG.debug("saveMessage: save message: " + message);
			
			TMessageTable.saveMessage(message);
			mMessages.remove(message.getMessageId());
			
			message = getMessage(message.getMessageId());
			mStreams.putMessage(message);
			
			mNewMessages = null;
			
			return message;
		}
	}
	
	public IMessageSet getNewMessages() throws ErrorException {
		synchronized (mMessages) {
			if (mNewMessages == null) {
				MessageQuery query = new MessageQuery(0, 10);
				query.addFolderName(getService().getIncomingFolder());
				query.setStatus(IMessage.STATUS_NEW);
				mNewMessages = getMessages(query);
			}
			return mNewMessages;
		}
	}
	
	public TMessageSet getMessages(String streamId, String folderName) 
			throws ErrorException {
		if (streamId == null) return null;
		
		String[] messageIds = mStreams.getMessageIds(streamId, folderName);
		final Set<TMessage> messageSet = newSet();
		
		if (messageIds != null) {
			for (String messageId : messageIds) {
				TMessage message = getMessage(messageId);
				if (message != null)
					messageSet.add(message);
			}
		}
		
		TMessage[] messages = messageSet.toArray(new TMessage[messageSet.size()]);
		long utime = 0;
		
		if (messages != null) {
			for (TMessage message : messages) {
				if (message != null) {
					if (message.getMessageTime() > utime) 
						utime = message.getMessageTime();
					if (message.getUpdateTime() > utime)
						utime = message.getUpdateTime();
				}
			}
		}
		
		return new TMessageSet(messages, (int)messages.length, (int)0, utime);
	}
	
	public TMessageSet getMessages(IMessageQuery query) throws ErrorException {
		if (query == null) throw new NullPointerException();
		
		if (LOG.isDebugEnabled())
			LOG.debug("getMessages: query=" + query);
		
		String[] folderNames = query.getFolderNames();
		
		String account = query.getAccount();
		String chatuser = query.getChatUser();
		String streamId = query.getStreamId();
		String status = query.getStatus();
		String flag = query.getFlag();
		
		long timeStart = query.getTimeStart();
		long timeEnd = query.getTimeEnd();
		int rowSize = query.getRowSize();
		
		int resultStart = (int)query.getResultStart();
		int resultCount = query.getResultCount();
		
		if (resultStart < 0) resultStart = 0;
		if (resultCount < 0) resultCount = 0;
		
		IMessageQuery.GroupBy groupby = query.getGroupBy();
		int groupRowSize = query.getGroupRowSize();
		
		TMessage[] messages = queryMessages(account, 
				folderNames, chatuser, streamId, status, flag, 
				rowSize, timeStart, timeEnd);
		
		if (groupby == IMessageQuery.GroupBy.STREAM)
			messages = groupbyStream(messages, groupRowSize);
		
		return createSet(messages, resultStart, resultCount);
	}
	
	private TMessageSet createSet(TMessage[] messages, 
			int resultStart, int resultCount) {
		ArrayList<TMessage> list = new ArrayList<TMessage>();
		int resultEnd = resultStart + resultCount;
		int totalCount = 0;
		long utime = 0;
		
		if (messages != null) {
			totalCount = messages.length;
			
			for (int i = resultStart; i < messages.length && i < resultEnd; i++) {
				TMessage message = messages[i];
				list.add(message);
			}
			
			for (TMessage message : messages) {
				if (message != null) {
					if (message.getMessageTime() > utime) 
						utime = message.getMessageTime();
					if (message.getUpdateTime() > utime)
						utime = message.getUpdateTime();
				}
			}
		}
		
		TMessage[] results = list.toArray(new TMessage[list.size()]);
		
		return new TMessageSet(results, (int)totalCount, (int)resultStart, utime);
	}
	
	private TMessage[] groupbyStream(TMessage[] messages, int groupRowSize) {
		if (messages == null) return messages;
		
		StreamSet[] streams = toStreamSet(messages);
		ArrayList<TMessage> list = new ArrayList<TMessage>();
		
		if (streams != null) {
			Arrays.sort(streams, new Comparator<StreamSet>() {
					@Override
					public int compare(StreamSet o1, StreamSet o2) {
						long t1 = o1.mMessageTime;
						long t2 = o2.mMessageTime;
						
						String id1 = o1.mStreamId;
						String id2 = o2.mStreamId;
						
						return t1 > t2 ? -1 : (t1 < t2 ? 1 : id1.compareTo(id2));
					}
				});
			
			for (StreamSet streamSet : streams) {
				int rowCount = 0;
				for (TMessage message : streamSet.mMessages) {
					if (message != null) {
						list.add(message);
						rowCount ++;
						if (groupRowSize > 0 && rowCount >= groupRowSize)
							break;
					}
				}
			}
		}
		
		return list.toArray(new TMessage[list.size()]);
	}
	
	private StreamSet[] toStreamSet(TMessage[] messages) {
		if (messages == null) return null;
		
		Map<String,StreamSet> streams = new HashMap<String,StreamSet>();
		
		for (TMessage message : messages) {
			if (message == null) continue;
			
			String streamId = message.getStreamId();
			if (streamId == null || streamId.length() == 0)
				continue;
			
			StreamSet streamSet = streams.get(streamId);
			if (streamSet == null) {
				streamSet = new StreamSet(streamId);
				streams.put(streamId, streamSet);
			}
			
			streamSet.put(message);
		}
		
		return streams.values().toArray(new StreamSet[streams.size()]);
	}
	
	private TMessage[] queryMessages(String account, 
			String[] folderNames, String chatuser, String streamId, String status, 
			String flag, int rowSize, long minStamp, long maxStamp) 
			throws ErrorException {
		final Set<TMessage> messageSet = newSet();
		
		if (folderNames != null && folderNames.length > 0) {
			for (String folderName : folderNames) {
				if (folderName != null && folderName.length() > 0) {
					scanMessages(messageSet, account, folderName, null, null, null, 
							status, flag, rowSize, minStamp, maxStamp);
				}
			}
		} else if (streamId != null && streamId.length() > 0) {
			scanMessages(messageSet, account, IMessage.INBOX, null, null, streamId, 
					status, flag, rowSize, minStamp, maxStamp);
			scanMessages(messageSet, account, IMessage.OUTBOX, null, null, streamId, 
					status, flag, rowSize, minStamp, maxStamp);
			
		} else if (chatuser != null && chatuser.length() > 0) {
			scanMessages(messageSet, account, IMessage.INBOX, chatuser, null, null, 
					status, flag, rowSize, minStamp, maxStamp);
			scanMessages(messageSet, account, IMessage.OUTBOX, null, chatuser, null, 
					status, flag, rowSize, minStamp, maxStamp);
			
		} else {
			scanMessages(messageSet, account, null, null, null, null, 
					status, flag, rowSize, minStamp, maxStamp);
		}
		
		return messageSet.toArray(new TMessage[messageSet.size()]);
	}
	
	private void scanMessages(final Set<TMessage> messageSet, 
			String account, String folderName, String messageFrom, String messageTo, 
			String streamId, String status, String flag, int rowSize, 
			long minStamp, long maxStamp) throws ErrorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("scanMessages: account=" + account 
					+ ", folderName=" + folderName 
					+ ", from=" + messageFrom + ", to=" + messageTo 
					+ ", status=" + status + ", flag=" + flag
					+ ", rowSize=" + rowSize + ", minstamp=" + minStamp 
					+ ", maxstamp=" + maxStamp);
		}
		
		TMessageTable.scanMessages(getService(), 
			account, folderName, messageFrom, messageTo, streamId, status, flag, 
			rowSize, minStamp, maxStamp, 
			new TMessageTable.Collector() {
				@Override
				public void addMessage(TMessage message) throws ErrorException {
					if (message == null) return;
					put(message); messageSet.add(message);
				}
			});
	}
	
	static class StreamSet {
		private final String mStreamId;
		private final TreeSet<TMessage> mMessages;
		private long mMessageTime = 0;
		
		public StreamSet(String streamId) {
			if (streamId == null) throw new NullPointerException();
			mStreamId = streamId;
			mMessages = newSet();
		}
		
		public void put(TMessage message) {
			if (message == null) return;
			if (!mStreamId.equals(message.getStreamId())) return;
			mMessages.add(message);
			long messageTime = message.getMessageTime();
			if (messageTime > mMessageTime)
				mMessageTime = messageTime;
		}
	}
	
	static TreeSet<TMessage> newSet() {
		return new TreeSet<TMessage>(new Comparator<TMessage>() {
				@Override
				public int compare(TMessage o1, TMessage o2) {
					long t1 = o1.getMessageTime();
					long t2 = o2.getMessageTime();
					
					String id1 = o1.getMessageId();
					String id2 = o2.getMessageId();
					
					return t1 > t2 ? -1 : (t1 < t2 ? 1 : id1.compareTo(id2));
				}
			});
	}
	
}
