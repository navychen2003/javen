package org.javenstudio.android.information;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.android.SimpleHtmlHrefParser;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;
import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.cocoka.widget.model.NavigationItem;
import org.javenstudio.common.util.Logger;

public abstract class InformationNavItem extends NavigationItem 
		implements InformationOne.Item {
	private static final Logger LOG = Logger.getLogger(InformationNavItem.class);

	public static interface NavModel { 
		public void postAddInformation(InformationNavItem item, Information data, boolean notify);
		public void postNotifyChanged(InformationNavItem item);
		public void onExceptionCatched(Throwable e);
	}
	
	public static interface NavBinder { 
		public InformationBinder getBinder(InformationNavItem item);
		public IMenuOperation getMenuOperation();
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	private final InformationDataSets mDataSets; 
	private final List<InformationPage> mPageItems;
	private ReloadType mReloadType = null;
	
	public InformationNavItem(NavigationInfo info, boolean selected) { 
		super(info, selected); 
		mDataSets = new InformationDataSets(createInformationCursorFactory()); 
		mPageItems = new ArrayList<InformationPage>();
		
		Object data = info.getAttribute(Information.ATTR_LOCATIONS);
		addPages(data);
	}
	
	protected InformationCursorFactory createInformationCursorFactory() { 
		return new InformationCursorFactory(); 
	}
	
	@Override 
	public AbstractDataSets<?> getDataSets() { 
		return getInformationDataSets(); 
	}
	
	public InformationDataSets getInformationDataSets() { 
		return mDataSets; 
	}
	
	@Override
	public Object getAttribute(String key) { 
		return getInfo().getAttribute(key);
	}
	
	@Override
	public void setAttribute(String key, Object val) { 
		getInfo().setAttribute(key, val);
	}
	
	public String getLocation() { 
		return (String)getInfo().getAttribute(Information.ATTR_LOCATION); 
	}
	
	public String getLocation(String name) { 
		return (String)getInfo().getAttribute(name); 
	}
	
	public String getDefaultCharset() { 
		return (String)getInfo().getAttribute(Information.ATTR_DEFAULTCHARSET);
	}
	
	public String getDefaultCharset(String location) { 
		return getDefaultCharset(); 
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
	
	public IMenuOperation getMenuOperation() { return null; }
	
	public abstract InformationBinder getBinder();
	public abstract void onFetched(NavModel model, String location, String content, boolean first); 
	
	protected void onAddInformationBegin(NavModel model, String location, boolean first) { 
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
	
	protected void onAddInformationEnd(NavModel model, String location, boolean first) { 
		postNotifyDataSets();
	}
	
	protected void postAddInformation(NavModel model, Information data) { 
		if (model != null && data != null) 
			model.postAddInformation(this, data, false);
	}
	
}
