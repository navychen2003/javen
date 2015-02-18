package org.javenstudio.falcon.user.profile;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class ContactManager {
	private static final Logger LOG = Logger.getLogger(ContactManager.class);

	private final Member mUser;
	private final IContactStore mStore;
	
	private final Map<String, ContactGroup> mContacts = 
			new HashMap<String, ContactGroup>();
	
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return ContactManager.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "ContactManager(" + ContactManager.this.getUser().getUserName() + ")";
			}
		};
	
	public ContactManager(Member user, IContactStore store) throws ErrorException { 
		if (user == null || store == null) throw new NullPointerException();
		mUser = user;
		mStore = store;
		loadContacts(false);
	}
	
	public Member getUser() { return mUser; }
	public IContactStore getStore() { return mStore; }
	public ILockable.Lock getLock() { return mLock; }
	
	public synchronized int getContactCount() { 
		synchronized (mUser) { 
			synchronized (mContacts) { 
				int count = 0;
				
				for (ContactGroup group : mContacts.values()) { 
					if (group != null) count += group.getContactCount();
				}
				
				return count;
			}
		}
	}
	
	public synchronized Contact addContact(Contact contact, String type) { 
		if (contact == null || type == null) return null;
		
		synchronized (mUser) { 
			synchronized (mContacts) { 
				ContactGroup group = mContacts.get(type);
				if (group == null) { 
					group = new ContactGroup(this, type);
					mContacts.put(type, group);
				}
				
				return group.addContact(contact);
			}
		}
	}
	
	public synchronized ContactGroup[] getContactGroups() { 
		synchronized (mUser) { 
			synchronized (mContacts) { 
				return mContacts.values().toArray(new ContactGroup[mContacts.size()]);
			}
		}
	}
	
	public synchronized String[] getContactTypes() { 
		synchronized (mUser) { 
			synchronized (mContacts) { 
				return mContacts.keySet().toArray(new String[mContacts.size()]);
			}
		}
	}
	
	public synchronized ContactGroup getContactGroup(String type) { 
		synchronized (mUser) { 
			synchronized (mContacts) { 
				return mContacts.get(type);
			}
		}
	}
	
	public synchronized void saveContacts() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveContacts: user=" + mUser);
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (mUser) {
				synchronized (mContacts) { 
					getStore().saveContactList(this, 
							toNamedList(getContactGroups()));
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadContacts(boolean force) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadContacts: user=" + mUser);
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (mUser) {
				synchronized (mContacts) { 
					if (mLoaded && force == false)
						return;
					
					mContacts.clear();
					
					NamedList<Object> items = getStore().loadContactList(this);
					loadNamedList(this, items);
					
					mLoaded = true;
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.READ);
		}
	}
	
	public boolean isClosed() { return mClosed; }
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
		
		try {
			getLock().lock(ILockable.Type.READ, null);
			try {
				synchronized (mUser) {
					synchronized (mContacts) { 
						ContactGroup[] groups = mContacts.values().toArray(
								new ContactGroup[mContacts.size()]);
						
						if (groups != null) { 
							for (ContactGroup group : groups) { 
								if (group != null) group.close();
							}
						}
						
						getUser().removeContactManager();
						
						mContacts.clear();
						mLoaded = false;
					}
				}
			} finally { 
				getLock().unlock(ILockable.Type.READ);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	static void loadNamedList(ContactManager manager, 
			NamedList<Object> items) throws ErrorException { 
		if (manager == null || items == null) return;
		
		for (int i=0; i < items.size(); i++) { 
			String name = items.getName(i);
			Object value = items.getVal(i);
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("loadNamedList: name=" + name + " value=" + value);
			
			if (value != null && value instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> item = (NamedList<Object>)value;
				
				ContactGroup.loadContactGroup(manager, item);
			}
		}
	}
	
	static NamedList<Object> toNamedList(ContactGroup[] list) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; list != null && i < list.length; i++) { 
			ContactGroup group = list[i];
			NamedList<Object> item = ContactGroup.toNamedList(group);
			if (group != null && item != null) 
				items.add(group.getContactType(), item);
		}
		
		return items;
	}
	
}
