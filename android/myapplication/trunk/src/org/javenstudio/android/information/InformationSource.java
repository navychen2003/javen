package org.javenstudio.android.information;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.android.SimpleHtmlHrefParser;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class InformationSource implements InformationOne.Item {
	private static final Logger LOG = Logger.getLogger(InformationSource.class);
	
	public static final String ATTR_DEFAULTCHARSET = "defaultCharset";
	
	public static interface SourceBinder { 
		public InformationBinder getBinder(InformationSource source);
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	private final SourceBinder mBinder;
	private final InformationDataSets mDataSets; 
	private final Set<String> mFetchedLocations; 
	private final Map<String, Object> mAttrs;
	private final String mLocation; 
	
	private final List<InformationPage> mPageItems;
	private ReloadType mReloadType = null;
	
	public InformationSource(SourceBinder binder, String location) { 
		this(binder, location, new HashMap<String, Object>()); 
	}
	
	public InformationSource(SourceBinder binder, String location, 
			Map<String, Object> attrs) { 
		mBinder = binder;
		mDataSets = createDataSets(); 
		mFetchedLocations = new HashSet<String>(); 
		mPageItems = new ArrayList<InformationPage>();
		mLocation = location; 
		mAttrs = attrs; 
		
		if (mLocation == null) 
			throw new NullPointerException("location is null");
		
		if (mAttrs == null) 
			throw new NullPointerException("attrs is null");
	}
	
	public final String getLocation() { return mLocation; }
	public final InformationDataSets getInformationDataSets() { return mDataSets; }
	public final InformationBinder getBinder() { return mBinder.getBinder(this); }
	
	public boolean hasAttribute(String key) { 
		return mAttrs.containsKey(key);
	}
	
	public Object getAttribute(String key) { 
		return mAttrs.get(key);
	}
	
	public void setAttribute(String key, Object value) { 
		mAttrs.put(key, value);
	}
	
	public Object removeAttribute(String key) { 
		return mAttrs.remove(key);
	}
	
	public Collection<String> getAttributeNames() { 
		return mAttrs.keySet();
	}
	
	public String getDefaultCharset() { 
		return (String)getAttribute(ATTR_DEFAULTCHARSET);
	}
	
	public String getDefaultCharset(String location) { 
		return getDefaultCharset(); 
	}
	
	public final synchronized void addFetchedLocation(String location) { 
		if (location != null && location.length() > 0) 
			mFetchedLocations.add(location); 
	}
	
	public final synchronized boolean existFetchedLocation(String location) { 
		if (location == null || location.length() == 0) 
			return false; 
		
		return mFetchedLocations.contains(location); 
	}
	
	protected InformationDataSets createDataSets() { 
		return new InformationDataSets(createCursorFactory()); 
	}
	
	protected InformationCursorFactory createCursorFactory() { 
		return new InformationCursorFactory(); 
	}
	
	public boolean shouldReload() { 
		return getInformationDataSets().getCount() <= 0; 
	}
	
	public abstract void onFetched(InformationModel model, 
			String location, String content, boolean first); 
	
	public abstract void onSubContentFetched(InformationModel model, 
			Information data, String location, String content); 
	
	public boolean hasNextPage() { 
		String location = getNextLocation();
		if (location != null && location.length() > 0)
			return true;
		
		return false; 
	}
	
	public ReloadType getReloadType() { return mReloadType; }
	
	public void onReloadPages(ReloadType type) { 
		if (type == ReloadType.NEXTPAGE) return;
		
		synchronized (mPageItems) { 
			mReloadType = type;
			
			for (InformationPage item : mPageItems) { 
				item.setStatus(InformationPage.Status.UNFETCH);
			}
		}
	}
	
	public String getNextLocation() { 
		synchronized (mPageItems) { 
			for (InformationPage item : mPageItems) { 
				if (item.getStatus() == InformationPage.Status.ERROR)
					return null;
				
				if (item.getStatus() == InformationPage.Status.UNFETCH) 
					return item.getLocation();
			}
		}
		
		return null;
	}
	
	public void addPages(Object data) { 
		if (data != null) { 
			if (data instanceof String[]) { 
				addPageLocation((String[])data);
			} else if (data instanceof String) { 
				addPageLocation((String)data);
			} else { 
				addPageLocation(data.toString());
			}
		}
	}
	
	public void addPageLocation(String... locations) { 
		if (locations == null || locations.length == 0)
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("addPageLocation: id=" + getIdentity() 
					+ " location=" + getLocation() + ", add locations: " + locations);
		}
		
		for (int i=0; i < locations.length; i++) { 
			String location = locations[i];
			if (location == null || location.length() == 0)
				continue;
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("addPageLocation: id=" + getIdentity() 
						+ ", page location: " + location);
			}
			
			location = SimpleHtmlHrefParser.normalizeHref(location, getLocation());
			if (location == null || location.equals(getLocation()))
				continue;
			
			synchronized (mPageItems) { 
				boolean found = false;
				
				for (InformationPage item : mPageItems) { 
					if (location.equals(item.getLocation())) { 
						found = true;
						break;
					}
				}
				
				if (!found) {
					mPageItems.add(new InformationPage(location));
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("addPageLocation: id=" + getIdentity() 
								+ ", add location: " + location);
					}
				}
			}
		}
	}
	
	protected void onAddInformationBegin(InformationModel model, 
			String location, boolean first) { 
		if (model == null || location == null) 
			return; 
		
		if (location.equals(getLocation())) { 
			synchronized (this) { 
				mFetchedLocations.clear(); 
				//if (first) postClearDataSets(); 
			}
		}
		
		if (first) { //location != null && location.equals(getLocation())) {
			if (LOG.isDebugEnabled())
				LOG.debug("onAddInformationBegin: clearDataSets, location=" + location);
			
			postClearDataSets(); 
		}
		
		synchronized (mPageItems) { 
			for (InformationPage item : mPageItems) { 
				if (location != null && location.equals(item.getLocation())) {
					if (LOG.isDebugEnabled())
						LOG.debug("onAddInformationBegin: page fetched, location=" + location);
					
					item.setStatus(InformationPage.Status.FETCHED);
				}
			}
		}
	}
	
	protected void onAddInformationEnd(final InformationModel model, 
			String location, boolean first) { 
		postNotifyDataSets();
	}
	
	public String getSubContentPath(Information data, String content) { 
		return null; // do nothing
	}
	
	protected synchronized void clearDataSets() { 
		getInformationDataSets().clear(); 
	}
	
	protected synchronized void notifyDataSets() { 
		getInformationDataSets().notifyContentChanged(true);
		getInformationDataSets().notifyDataSetChanged();
	}
	
	protected void postAddInformation(InformationModel model, Information data) { 
		model.postAddInformation(this, data, false);
	}
	
	protected void postClearDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() { 
				public void run() { 
					clearDataSets(); 
				}
			});
	}
	
	protected void postNotifyDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() { 
				public void run() { 
					notifyDataSets();
				}
			});
	}
	
}
