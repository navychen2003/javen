package org.javenstudio.falcon.user.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class AnnouncementManager {
	private static final Logger LOG = Logger.getLogger(AnnouncementManager.class);

	private final Map<String, Announcement> mAnnouncements = 
			new HashMap<String, Announcement>();
	
	private final IUnitStore mManager;
	private final IAnnouncementStore mStore;
	
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return AnnouncementManager.this.getManager().getLock();
			}
			@Override
			public String getName() {
				return "AnnouncementManager(" + AnnouncementManager.this.getManager().getUserName() + ")";
			}
		};
	
	public AnnouncementManager(IUnitStore manager, IAnnouncementStore store) { 
		if (manager == null || store == null) throw new NullPointerException();
		mManager = manager;
		mStore = store;
	}

	public IUnitStore getManager() { return mManager; }
	public IAnnouncementStore getStore() { return mStore; }
	public ILockable.Lock getLock() { return mLock; }
	
	public Announcement addAnnouncement(Announcement anno) { 
		if (anno == null) return null;
		
		synchronized (mAnnouncements) { 
			String key = anno.getKey();
			
			if (LOG.isDebugEnabled())
				LOG.debug("addAnnouncement: announcement=" + anno);
			
			mAnnouncements.put(key, anno);
			return anno;
		}
	}
	
	public Announcement removeAnnouncement(String key) { 
		if (key == null) return null;
		
		synchronized (mAnnouncements) { 
			Announcement anno = mAnnouncements.remove(key);
			
			if (LOG.isDebugEnabled())
				LOG.debug("removeAnnouncement: announcement=" + anno);
			
			return anno;
		}
	}
	
	public Announcement getAnnouncement(String key) { 
		if (key == null) return null;
		
		synchronized (mAnnouncements) { 
			return mAnnouncements.get(key);
		}
	}
	
	public String[] getAnnouncementKeys() { 
		synchronized (mAnnouncements) { 
			return mAnnouncements.keySet().toArray(new String[mAnnouncements.size()]);
		}
	}
	
	public Announcement[] getAnnouncements() {
		return getAnnouncements((String)null);
	}
	
	public Announcement[] getAnnouncements(String lang) { 
		synchronized (mAnnouncements) { 
			ArrayList<Announcement> list = new ArrayList<Announcement>();
			for (Announcement item : mAnnouncements.values()) {
				if (item == null) continue;
				if (lang == null || lang.length() == 0 || 
					lang.equalsIgnoreCase(item.getLanguage())) {
					list.add(item);
				}
			}
			return list.toArray(new Announcement[list.size()]);
		}
	}
	
	public void clearAnnouncements() { 
		synchronized (mAnnouncements) { 
			if (LOG.isDebugEnabled())
				LOG.debug("clearAnnouncements");
			
			mAnnouncements.clear();
		}
	}
	
	public int getAnnouncementCount() { 
		synchronized (mAnnouncements) { 
			return mAnnouncements.size();
		}
	}
	
	public synchronized void saveAnnouncements() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveAnnouncements");
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (mStore) {
				getStore().saveAnnouncementList(this, toNamedList(this));
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadAnnouncements(boolean force) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadAnnouncements");
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (mStore) {
				if (mLoaded && force == false)
					return;
				
				mAnnouncements.clear();
				
				NamedList<Object> items = getStore().loadAnnouncementList(this);
				loadNamedList(this, items);
				
				mLoaded = true;
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
				mAnnouncements.clear();
				mLoaded = false;
			} finally { 
				getLock().unlock(ILockable.Type.READ);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{size=" + getAnnouncementCount() + "}";
	}
	
	static void loadNamedList(AnnouncementManager manager, 
			NamedList<Object> item) throws ErrorException { 
		if (manager == null || item == null) return;
		
		//String type = SettingConf.getString(item, "type");
		Announcement[] annos = loadAnnouncements(item.get("announcements"));
		
		//if (type == null || type.length() == 0) { 
		//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
		//			"AnnouncementGroup type: " + type + " is wrong");
		//}
		
		if (annos != null && annos.length > 0) { 
			for (Announcement anno : annos) { 
				manager.addAnnouncement(anno);
			}
		}
	}
	
	static NamedList<Object> toNamedList(AnnouncementManager item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		//info.add("type", "default");
		info.add("announcements", toNamedLists(item.getAnnouncements()));
		
		return info;
	}
	
	private static Announcement[] loadAnnouncements(Object listVal) 
			throws ErrorException { 
		ArrayList<Announcement> list = new ArrayList<Announcement>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadAnnouncements: class=" + (listVal!=null?listVal.getClass():null) 
					+ " list=" + listVal);
		}
		
		if (listVal != null && listVal instanceof List) { 
			List<?> listItem = (List<?>)listVal;
			
			for (int j=0; j < listItem.size(); j++) { 
				Object val = listItem.get(j);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadAnnouncements: listItem=" + val);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> item = (NamedList<Object>)val;
					
					Announcement anno = loadAnnouncement(item);
					if (anno != null)
						list.add(anno);
				}
			}
		}
		
		return list.toArray(new Announcement[list.size()]);
	}
	
	@SuppressWarnings("unchecked")
	private static NamedList<Object>[] toNamedLists(Announcement[] annos) 
			throws ErrorException { 
		ArrayList<NamedList<?>> items = new ArrayList<NamedList<?>>();
		
		for (int i=0; annos != null && i < annos.length; i++) { 
			Announcement anno = annos[i];
			NamedList<Object> annoInfo = toNamedList(anno);
			if (anno != null && annoInfo != null) 
				items.add(annoInfo);
		}
		
		return items.toArray(new NamedList[items.size()]);
	}
	
	private static Announcement loadAnnouncement(NamedList<Object> item) 
			throws ErrorException { 
		if (item == null) return null;
		
		Announcement anno = Announcement.loadAnnouncement(item);
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadAnnouncement: anno=" + anno);
		
		return anno;
	}
	
	private static NamedList<Object> toNamedList(Announcement item) 
			throws ErrorException { 
		if (item == null) return null;
		return Announcement.toNamedList(item);
	}
	
}
