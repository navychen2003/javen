package org.javenstudio.provider.app.flickr;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.people.BaseGroupListProvider;
import org.javenstudio.provider.people.group.GroupFactory;
import org.javenstudio.provider.people.group.GroupListBinder;
import org.javenstudio.provider.people.group.GroupListItem;
import org.javenstudio.provider.people.group.GroupListProvider;

public class FlickrGroupListProvider extends BaseGroupListProvider {
	private static final Logger LOG = Logger.getLogger(FlickrGroupListProvider.class);
	
	private final String mLocation;
	private final boolean mSearchable;
	private String mSearchText = null;
	private boolean mReloaded = false;
	
	private FlickrGroupListProvider(String location, 
			String name, int iconRes, boolean searchable) { 
		super(name, iconRes, new FlickrGroupFactory());
		mLocation = location; //FlickrHelper.GROUPSEARCH_URL;
		mSearchable = searchable;
	}
	
	static class FlickrGroupFactory extends GroupFactory {
		@Override
		public GroupListBinder createGroupListBinder(GroupListProvider p) {
			return new FlickrGroupListBinder((FlickrGroupListProvider)p);
		}
	}
	
	@Override
	public boolean isSearchable() { return mSearchable; }
	
	private String getReloadLocation(ReloadCallback callback) { 
		String queryText = callback.getParam(ReloadCallback.PARAM_QUERYTEXT);
    	String location = mLocation;
    	
    	if (isSearchable()) {
    		if (queryText == null || queryText.length() == 0) 
    			queryText = getQueryText();
    		
	    	if (queryText == null || queryText.length() == 0) 
	    		queryText = "china";
	    	
	    	if (!Utilities.isEquals(queryText, mSearchText)) 
	    		mReloaded = false;
	    	
	    	mSearchText = queryText;
			callback.clearParams();
	    	
	    	if (queryText != null && queryText.length() > 0) { 
	    		try {
	    			location = FlickrHelper.GROUPSEARCH_URL;
	    			location += URLEncoder.encode(queryText, "UTF-8");
	    		} catch (Throwable e) { 
	    			if (LOG.isWarnEnabled()) 
	    				LOG.warn(e.toString(), e);
	    		}
	    	}
    	}
    	
    	return location;
	}
	
	@Override
	public synchronized void reloadOnThread(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		final String location = getReloadLocation(callback);
    	
    	if (mReloaded == false || type == ReloadType.FORCE) {
	    	//if (type == ReloadType.FORCE || getGroupDataSets().getCount() <= 0) {
				HtmlCallback cb = new HtmlCallback() {
						@Override
						public void onHtmlFetched(String content) {
							onFetched(callback, location, content); 
						}
						@Override
						public void onHttpException(HttpException e) { 
							if (e == null) return;
							callback.onActionError(new ActionError(ActionError.Action.GROUPLIST, e)); 
						}
					};
				
				cb.setRefetchContent(type == ReloadType.FORCE);
				cb.setSaveContent(true);
				
				FetchHelper.removeFailed(location);
				FetchHelper.fetchHtml(location, cb);
	    	//}
	    	
			mReloaded = true;
    	}
	}
	
	private synchronized void onFetched(ProviderCallback callback, 
    		String location, String content) { 
    	if (location == null || content == null || content.length() == 0) 
    		return;
    	
    	postClearDataSets();
    	
    	try { 
			NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
			XmlParser parser = new XmlParser(handler); 
			parser.parse(content); 
			
			NodeXml xml = handler.getEntity(); 
			ArrayList<YGroupEntry> entryList = new ArrayList<YGroupEntry>();
			
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node rspChild = xml.getChildAt(i);
				if (rspChild != null && "groups".equalsIgnoreCase(rspChild.getName())) {
					for (int j=0; j < rspChild.getChildCount(); j++) {
						Node photosChild = rspChild.getChildAt(j);
						if (photosChild != null && "group".equalsIgnoreCase(photosChild.getName())) {
							try { 
								YGroupEntry entry = YGroupEntry.parseEntry(photosChild, getIconRes()); 
								if (entry != null)
									entryList.add(entry);
							} catch (Throwable e) { 
								if (LOG.isWarnEnabled())
									LOG.warn("parse entry error: " + e.toString(), e); 
							}
						}
					}
				}
			}
			
			YGroupEntry[] entries = sortEntries(entryList);
			if (entries != null && entries.length > 0) { 
				for (int i=0; i < entries.length; i++) { 
					postAddGroupItem(callback, newGroupListItem(entries[i]));
				}
			}
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.GROUPLIST, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	postNotifyChanged();
    }
	
	private GroupListItem newGroupListItem(YGroupEntry entry) { 
		return entry != null ? new GroupListItem(this, entry) : null;
	}
	
	private YGroupEntry[] sortEntries(List<YGroupEntry> entryList) { 
		if (entryList == null || entryList.size() == 0) 
			return null;
		
		YGroupEntry[] entries = entryList.toArray(new YGroupEntry[entryList.size()]);
		
		if (entries != null && entries.length > 0) { 
			Arrays.sort(entries, new Comparator<YGroupEntry>() {
					@Override
					public int compare(YGroupEntry lhs, YGroupEntry rhs) {
						if (lhs.getMemberCount() != rhs.getMemberCount()) { 
							if (lhs.getMemberCount() > rhs.getMemberCount())
								return -1;
							else
								return 1;
						}
						
						if (lhs.getTopicCount() != rhs.getTopicCount()) { 
							if (lhs.getTopicCount() > rhs.getTopicCount())
								return -1;
							else
								return 1;
						}
						
						if (lhs.name != null || rhs.name != null) {
							if (lhs.name != null && rhs.name != null) 
								return lhs.name.compareTo(rhs.name);
							
							if (lhs.name == null) 
								return 1;
							
							return -1;
						}
						
						if (lhs.groupId != null || rhs.groupId != null) {
							if (lhs.groupId != null && rhs.groupId != null) 
								return lhs.groupId.compareTo(rhs.groupId);
							
							if (lhs.groupId == null)
								return 1;
							
							return -1;
						}
						
						return 0;
					}
				});
		}
		
		return entries;
	}
	
	public static FlickrGroupListProvider newSearchProvider(DataApp app, String name, int iconRes) { 
		return new FlickrGroupListProvider(FlickrHelper.GROUPSEARCH_URL, name, iconRes, true);
	}
	
	public static FlickrGroupListProvider newUserProvider(String userId, String name, int iconRes) { 
		return new FlickrGroupListProvider(FlickrHelper.USER_GROUPS_URL + userId, name, iconRes, false);
	}
	
}
