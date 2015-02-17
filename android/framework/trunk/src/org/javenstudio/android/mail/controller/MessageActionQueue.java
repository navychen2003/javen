package org.javenstudio.android.mail.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessageActionQueue {

	private static MessageActionQueue sInstance = null; 
	public synchronized static MessageActionQueue getInstance() { 
		if (sInstance == null) 
			sInstance = new MessageActionQueue(); 
		return sInstance; 
	}
	
	public class MessageActions { 
		private final AccountMessages mAccount; 
		private final long mMessageId; 
		private Long mMailboxKey = null; 
		private Boolean mFlagRead = null; 
		private Boolean mFlagFavorite = null; 
		private Boolean mFlagAnswered = null; 
		private Boolean mDelete = null; 
		
		public MessageActions(AccountMessages account, long messageId) { 
			mAccount = account; 
			mMessageId = messageId; 
		}
		
		public final AccountMessages getAccount() { return mAccount; }
		public final long getMessageId() { return mMessageId; }
		
		public synchronized void setMailboxKey(long mailboxId) { mMailboxKey = mailboxId; }
		public synchronized void setFlagRead(boolean flag) { mFlagRead = flag; }
		public synchronized void setFlagFavorite(boolean flag) { mFlagFavorite = flag; }
		public synchronized void setFlagAnswered(boolean flag) { mFlagAnswered = flag; }
		public synchronized void setDelete(boolean delete) { mDelete = delete; }
		
		public synchronized boolean updateMailboxKey() { return mMailboxKey != null; }
		public synchronized boolean updateFlagRead() { return mFlagRead != null; }
		public synchronized boolean updateFlagFavorite() { return mFlagFavorite != null; }
		public synchronized boolean updateFlagAnswered() { return mFlagAnswered != null; }
		
		public synchronized long getMailboxKey() { 
			return mMailboxKey != null ? mMailboxKey.longValue() : -1; 
		}
		
		public synchronized boolean getFlagRead() { 
			return mFlagRead != null ? mFlagRead.booleanValue() : false; 
		}
		
		public synchronized boolean getFlagFavorite() { 
			return mFlagFavorite != null ? mFlagFavorite.booleanValue() : false; 
		}
		
		public synchronized boolean getFlagAnswered() { 
			return mFlagAnswered != null ? mFlagAnswered.booleanValue() : false; 
		}
		
		public synchronized boolean isDelete() { 
			return mDelete != null ? mDelete.booleanValue() : false; 
		}
	}
	
	public class AccountMessages { 
		private final long mAccountId; 
		private final Map<Long, MessageActions> mMessages; 
		
		public AccountMessages(long accountId) { 
			mAccountId = accountId; 
			mMessages = new HashMap<Long, MessageActions>(); 
		}
		
		public final long getAccountId() { return mAccountId; }
		public synchronized int getMessageCount() { return mMessages.size(); }
		
		public synchronized MessageActions[] removeMessages() { 
			MessageActions[] actions = mMessages.values().toArray(
					new MessageActions[mMessages.size()]); 
			mMessages.clear(); 
			return actions; 
		}
		
		public synchronized MessageActions[] removeDeleteMessages() { 
			ArrayList<MessageActions> actions = new ArrayList<MessageActions>(); 
			Long[] keys = mMessages.keySet().toArray(new Long[mMessages.size()]); 
			for (int i=0; keys != null && i < keys.length; i++) { 
				Long key = keys[i]; 
				MessageActions action = mMessages.get(key); 
				if (action != null && action.isDelete()) { 
					actions.add(action); 
					mMessages.remove(key); 
				}
			}
			return actions.toArray(new MessageActions[actions.size()]);
		}
		
		public synchronized MessageActions getMessageActions(long messageId) { 
			MessageActions actions = mMessages.get(messageId); 
			if (actions == null) { 
				actions = new MessageActions(this, messageId); 
				mMessages.put(messageId, actions); 
			}
			return actions; 
		}
	}
	
	private final Map<Long, AccountMessages> mAccounts; 
	
	private MessageActionQueue() { 
		mAccounts = new HashMap<Long, AccountMessages>(); 
	}
	
	public synchronized AccountMessages getAccountMessages(long accountId) { 
		AccountMessages account = mAccounts.get(accountId); 
		if (account == null) { 
			account = new AccountMessages(accountId); 
			mAccounts.put(accountId, account); 
		}
		return account; 
	}
	
	public synchronized MessageActions getAccountMessageActions(long accountId, long messageId) { 
		AccountMessages account = getAccountMessages(accountId); 
		if (account == null) 
			return null; 
		
		synchronized (account) { 
			return account.getMessageActions(messageId); 
		}
	}
	
	public synchronized void setMessageFlag(long accountId, long messageId, boolean read, boolean favorite, boolean answered) { 
		MessageActions actions = getAccountMessageActions(accountId, messageId); 
		if (actions == null) 
			return; 
		
		synchronized (actions) { 
			actions.setFlagRead(read); 
			actions.setFlagFavorite(favorite); 
			actions.setFlagAnswered(answered); 
		}
	}
	
	public synchronized void setMessageFolder(long accountId, long messageId, long mailboxId) { 
		MessageActions actions = getAccountMessageActions(accountId, messageId); 
		if (actions == null) 
			return; 
		
		synchronized (actions) { 
			actions.setMailboxKey(mailboxId); 
		}
	}
	
	public synchronized void setMessageDelete(long accountId, long messageId, boolean delete) { 
		MessageActions actions = getAccountMessageActions(accountId, messageId); 
		if (actions == null) 
			return; 
		
		synchronized (actions) { 
			actions.setDelete(delete); 
		}
	}
	
}
