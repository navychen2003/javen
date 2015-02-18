package org.javenstudio.falcon.user.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class HistoryGroup {
	private static final Logger LOG = Logger.getLogger(HistoryGroup.class);

	private final TreeSet<HistoryItem> mHistorys = 
		new TreeSet<HistoryItem>(new Comparator<HistoryItem>() {
			@Override
			public int compare(HistoryItem o1, HistoryItem o2) {
				long t1 = o1.getTime();
				long t2 = o2.getTime();
				return t1 > t2 ? (-1) : (t1 < t2 ? 1 : 
					(o1.getContentId().compareTo(o2.getContentId())));
			}
		});
	
	private final HistoryManager mManager;
	private final String mType;
	private final int mMaxSize;
	
	public HistoryGroup(HistoryManager manager, String type, int max) { 
		if (manager == null || type == null) throw new NullPointerException();
		mManager = manager;
		mType = type;
		mMaxSize = max > 0 ? max : 50;
	}

	public HistoryManager getManager() { return mManager; }
	public String getHistoryType() { return mType; }
	public int getMaxSize() { return mMaxSize; }
	
	public void addHistory(HistoryItem history) { 
		if (history == null) return;
		
		synchronized (mHistorys) { 
			if (mHistorys.size() > mMaxSize) mHistorys.pollLast();
			mHistorys.add(history);
		}
	}
	
	public HistoryItem getHistory(String contentId) {
		if (contentId == null) return null;
		
		synchronized (mHistorys) { 
			for (HistoryItem item : mHistorys) {
				if (item != null && contentId.equals(item.getContentId()))
					return item;
			}
			
			return null;
		}
	}
	
	public void removeHistory(HistoryItem history) {
		if (history == null) return;
		
		synchronized (mHistorys) { 
			mHistorys.remove(history);
		}
	}
	
	public HistoryItem[] getHistoryItems() { 
		synchronized (mHistorys) { 
			return mHistorys.toArray(new HistoryItem[mHistorys.size()]);
		}
	}
	
	public void clearHistoryItems() { 
		synchronized (mHistorys) { 
			mHistorys.clear();
		}
	}
	
	public int getHistorySize() { 
		synchronized (mHistorys) { 
			return mHistorys.size();
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("close: type=" + getHistoryType());
		
		clearHistoryItems();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{type=" + mType + "}";
	}
	
	static void loadHistoryGroup(HistoryManager manager, 
			NamedList<Object> item) throws ErrorException { 
		if (manager == null || item == null) return;
		
		String type = SettingConf.getString(item, "type");
		HistoryItem[] items = loadHistoryItems(item.get("items"));
		
		if (type == null || type.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"HistoryGroup type: " + type + " is wrong");
		}
		
		if (items != null && items.length > 0) { 
			for (HistoryItem history : items) { 
				manager.addHistory(history, type);
			}
		}
	}
	
	static NamedList<Object> toNamedList(HistoryGroup item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedList<Object> info = new NamedMap<Object>();
		
		info.add("type", item.getHistoryType());
		info.add("items", toNamedLists(item.getHistoryItems()));
		
		return info;
	}
	
	private static HistoryItem[] loadHistoryItems(Object listVal) 
			throws ErrorException { 
		ArrayList<HistoryItem> list = new ArrayList<HistoryItem>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadHistoryItems: class=" + (listVal!=null?listVal.getClass():null) 
					+ " list=" + listVal);
		}
		
		if (listVal != null && listVal instanceof List) { 
			List<?> listItem = (List<?>)listVal;
			
			for (int j=0; j < listItem.size(); j++) { 
				Object val = listItem.get(j);
				
				if (LOG.isDebugEnabled())
					LOG.debug("loadHistoryItems: listItem=" + val);
				
				if (val != null && val instanceof NamedList) { 
					@SuppressWarnings("unchecked")
					NamedList<Object> item = (NamedList<Object>)val;
					
					HistoryItem history = HistoryItem.loadHistoryItem(item);
					if (history != null)
						list.add(history);
				}
			}
		}
		
		return list.toArray(new HistoryItem[list.size()]);
	}
	
	@SuppressWarnings("unchecked")
	private static NamedList<Object>[] toNamedLists(HistoryItem[] historys) 
			throws ErrorException { 
		ArrayList<NamedList<?>> items = new ArrayList<NamedList<?>>();
		
		for (int i=0; historys != null && i < historys.length; i++) { 
			HistoryItem history = historys[i];
			NamedList<Object> historyInfo = HistoryItem.toNamedList(history);
			if (history != null && historyInfo != null) 
				items.add(historyInfo);
		}
		
		return items.toArray(new NamedList[items.size()]);
	}
	
}
