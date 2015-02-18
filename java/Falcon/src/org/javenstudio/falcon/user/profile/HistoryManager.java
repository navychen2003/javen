package org.javenstudio.falcon.user.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class HistoryManager {
	private static final Logger LOG = Logger.getLogger(HistoryManager.class);

	private static final String TYPE_ACCESS = "access";
	
	private final Member mUser;
	private final IHistoryStore mStore;
	
	private final Map<String, HistoryGroup> mHistorys = 
			new HashMap<String, HistoryGroup>();
	
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return HistoryManager.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "HistoryManager(" + HistoryManager.this.getUser().getUserName() + ")";
			}
		};
	
	public HistoryManager(Member user, IHistoryStore store) throws ErrorException { 
		if (user == null || store == null) throw new NullPointerException();
		mUser = user;
		mStore = store;
		loadHistoryItems(false);
	}
	
	public Member getUser() { return mUser; }
	public IHistoryStore getStore() { return mStore; }
	public ILockable.Lock getLock() { return mLock; }
	
	public void addHistory(HistoryItem item) { 
		addHistory(item, TYPE_ACCESS);
	}
	
	public synchronized void addHistory(HistoryItem item, String type) { 
		if (item == null) return;
		
		synchronized (mUser) { 
			synchronized (mHistorys) { 
				if (type == null || type.length() == 0)
					type = TYPE_ACCESS;
				
				HistoryGroup group = mHistorys.get(type);
				if (group == null) {
					group = new HistoryGroup(this, type, 50);
					mHistorys.put(type, group);
				}
				
				group.addHistory(item);
			}
		}
	}
	
	public synchronized HistoryItem removeHistory(String contentId) {
		if (contentId == null) return null;
		
		synchronized (mUser) { 
			synchronized (mHistorys) { 
				HistoryItem removed = null;
				
				for (HistoryGroup group : mHistorys.values()) {
					if (group != null) {
						HistoryItem item = group.getHistory(contentId);
						if (item != null) {
							group.removeHistory(item);
							removed = item;
						}
					}
				}
				
				return removed;
			}
		}
	}
	
	public synchronized HistoryGroup[] getHistoryGroups() { 
		synchronized (mUser) { 
			synchronized (mHistorys) { 
				return mHistorys.values().toArray(
						new HistoryGroup[mHistorys.size()]);
			}
		}
	}
	
	public synchronized String[] getHistoryTypes() { 
		synchronized (mUser) { 
			synchronized (mHistorys) { 
				return mHistorys.keySet().toArray(
						new String[mHistorys.size()]);
			}
		}
	}
	
	public synchronized HistoryGroup getHistoryGroup(String type) { 
		synchronized (mUser) { 
			synchronized (mHistorys) { 
				return mHistorys.get(type);
			}
		}
	}
	
	public synchronized void saveHistoryItems() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveHistoryItems: user=" + mUser);
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (mUser) {
				synchronized (mHistorys) { 
					getStore().saveHistoryList(this, toNamedList(getHistoryGroups()));
				}
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadHistoryItems(boolean force) 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadHistoryItems: user=" + mUser);
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (mUser) {
				synchronized (mHistorys) { 
					if (mLoaded && force == false)
						return;
					
					mHistorys.clear();
					loadGroupList(this, getStore().loadHistoryList(this));
					
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
					synchronized (mHistorys) { 
						HistoryGroup[] groups = mHistorys.values().toArray(
								new HistoryGroup[mHistorys.size()]);
						
						if (groups != null) { 
							for (HistoryGroup group : groups) { 
								if (group != null) group.close();
							}
						}
						
						getUser().removeHistoryManager();
						
						mHistorys.clear();
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
	
	public HistorySectionSet getHistorySections(int start, int count) {
		return getHistorySections(TYPE_ACCESS, start, count);
	}
	
	public HistorySectionSet getHistorySections(String type, int start, int count) {
		if (start < 0) start = 0;
		if (count <= 0) return null;
		
		final HistoryGroup group = getHistoryGroup(type);
		if (group == null) return null;
		
		HistoryItem[] items = group.getHistoryItems();
		if (items == null) return null;
		
		ArrayList<HistorySection> list = new ArrayList<HistorySection>();
		
		for (int i=start; i < items.length && i < start+count; i++) {
			HistoryItem item = items[i];
			if (item != null) { 
				try {
					IData data = SectionHelper.getData(getUser(), 
							item.getContentId(), IData.Access.DETAILS, null);
					IUser owner = UserHelper.getLocalUserByName(item.getOwner());
					if (data != null && data instanceof ISection) {
						ISection section = (ISection)data;
						list.add(new HistorySection(item, section, owner));
					}
				} catch (Throwable e) {
					if (LOG.isWarnEnabled())
						LOG.warn("getHistorySections: error: " + e, e);
				}
			}
		}
		
		return new HistorySectionSet(list.toArray(new HistorySection[list.size()]), 
				start, items.length);
	}
	
	static void loadGroupList(HistoryManager manager, 
			NamedList<Object> items) throws ErrorException { 
		if (manager == null || items == null) return;
		
		for (int i=0; i < items.size(); i++) { 
			String name = items.getName(i);
			Object value = items.getVal(i);
			
			if (LOG.isDebugEnabled()) 
				LOG.debug("loadGroupList: name=" + name + " value=" + value);
			
			if (value != null && value instanceof NamedList) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> item = (NamedList<Object>)value;
				
				HistoryGroup.loadHistoryGroup(manager, item);
			}
		}
	}
	
	static NamedList<Object> toNamedList(HistoryGroup[] list) 
			throws ErrorException { 
		NamedList<Object> items = new NamedMap<Object>();
		
		for (int i=0; list != null && i < list.length; i++) { 
			HistoryGroup group = list[i];
			NamedList<Object> item = HistoryGroup.toNamedList(group);
			if (group != null && item != null) 
				items.add(group.getHistoryType(), item);
		}
		
		return items;
	}
	
}
