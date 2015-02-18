package org.javenstudio.android.information.subscribe;

import org.javenstudio.android.information.BaseInformationNavItem;
import org.javenstudio.cocoka.widget.model.NavigationInfo;

public class SubscribeNavItem extends BaseInformationNavItem {

	public static interface Factory {
		public SubscribeNavItem create(NavigationInfo info, boolean selected); 
	}
	
	public static class RdfNavItem extends SubscribeNavItem { 
		public RdfNavItem(NavBinder res, NavigationInfo info) { 
			this(res, info, false); 
		}
		
		public RdfNavItem(NavBinder res, NavigationInfo info, boolean selected) { 
			super(res, info, selected); 
		}
		
		@Override 
		protected void parseXmlContent(String location, String content) { 
			SubscribeHelper.onRdfXmlFetched(getModel(), this, content); 
		}
	}
	
	public static class FeedNavItem extends SubscribeNavItem { 
		public FeedNavItem(NavBinder res, NavigationInfo info) { 
			this(res, info, false); 
		}
		
		public FeedNavItem(NavBinder res, NavigationInfo info, boolean selected) { 
			super(res, info, selected); 
		}
		
		@Override 
		protected void parseXmlContent(String location, String content) { 
			SubscribeHelper.onFeedXmlFetched(getModel(), this, content); 
		}
	}
	
	public SubscribeNavItem(NavBinder res, NavigationInfo info) { 
		this(res, info, false); 
	}
	
	public SubscribeNavItem(NavBinder res, NavigationInfo info, boolean selected) { 
		super(res, info, selected); 
	}
	
	@Override 
	protected final void parseInformation(String location, String content, boolean first) { 
		if (location == null || content == null || content.length() == 0) 
			return; 
		
		parseXmlContent(location, content); 
	}
	
	protected void parseXmlContent(String location, String content) { 
		SubscribeHelper.onRssXmlFetched(getModel(), this, content); 
	}
	
	protected SubscribeEntry newSubscribeEntry() { 
		return new SubscribeEntry(this, getLocation()); 
	}

}
