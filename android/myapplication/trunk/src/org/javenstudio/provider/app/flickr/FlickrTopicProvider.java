package org.javenstudio.provider.app.flickr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.publish.BaseTopicProvider;
import org.javenstudio.provider.publish.discuss.TopicBinder;
import org.javenstudio.provider.publish.discuss.TopicFactory;
import org.javenstudio.provider.publish.discuss.TopicItem;
import org.javenstudio.provider.publish.discuss.TopicProvider;

public class FlickrTopicProvider extends BaseTopicProvider {
	private static final Logger LOG = Logger.getLogger(FlickrTopicProvider.class);

	private final FlickrUserClickListener mUserClickListener;
	private final String mGroupId;
	private final String mLocation;
	private YTopicEntry.TopicsInfo mTopicsInfo = null;
	private boolean mReloaded = false;
	
	public FlickrTopicProvider(String groupId, String name, int iconRes, 
			FlickrUserClickListener listener) { 
		super(name, iconRes, new FlickrTopicFactory());
		mGroupId = groupId;
		mUserClickListener = listener;
		mLocation = FlickrHelper.GROUP_TOPICS_URL + groupId;
	}
	
	static class FlickrTopicFactory extends TopicFactory { 
		@Override
		public TopicBinder createTopicBinder(TopicProvider p) { 
			return new FlickrTopicBinder((FlickrTopicProvider)p);
		}
	}
	
	public String getGroupId() { return mGroupId; }
	public FlickrUserClickListener getUserClickListener() { return mUserClickListener; }
	public YTopicEntry.TopicsInfo getTopicsInfo() { return mTopicsInfo; }

	private String getReloadLocation(ReloadCallback callback) { 
		String location = mLocation;
		return location;
	}
	
	@Override
	public synchronized void reloadOnThread(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		final String location = getReloadLocation(callback);
    	
    	if (mReloaded == false || type == ReloadType.FORCE) {
	    	if (type == ReloadType.FORCE || getTopicDataSets().getCount() <= 0) {
				HtmlCallback cb = new HtmlCallback() {
						@Override
						public void onHtmlFetched(String content) {
							onFetched(callback, location, content); 
						}
						@Override
						public void onHttpException(HttpException e) { 
							if (e == null) return;
							callback.onActionError(new ActionError(ActionError.Action.TOPIC, e)); 
						}
					};
				
				cb.setRefetchContent(type == ReloadType.FORCE);
				cb.setSaveContent(true);
				
				FetchHelper.removeFailed(location);
				FetchHelper.fetchHtml(location, cb);
	    	}
	    	
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
			ArrayList<YTopicEntry> entryList = new ArrayList<YTopicEntry>();
			
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node rspChild = xml.getChildAt(i);
				if (rspChild != null && "topics".equalsIgnoreCase(rspChild.getName())) {
					mTopicsInfo = YTopicEntry.parseInfo(rspChild);
					
					for (int j=0; j < rspChild.getChildCount(); j++) {
						Node child = rspChild.getChildAt(j);
						if (child != null && "topic".equalsIgnoreCase(child.getName())) {
							try { 
								YTopicEntry entry = YTopicEntry.parseEntry(child, 
										getUserClickListener(), getIconRes()); 
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
			
			if (entryList.size() > 0) { 
				YTopicEntry[] entries = entryList.toArray(new YTopicEntry[entryList.size()]);
				Arrays.sort(entries, new Comparator<YTopicEntry>() {
						@Override
						public int compare(YTopicEntry lhs, YTopicEntry rhs) {
							if (lhs != null || rhs != null) { 
								if (lhs == null) return -1;
								if (rhs == null) return 1;
								
								long lhsTime = lhs.datelastpost > 0 ? lhs.datelastpost : lhs.datecreate; 
								long rhsTime = rhs.datelastpost > 0 ? rhs.datelastpost : rhs.datecreate;
								
								if (lhsTime > rhsTime) return -1;
								else if (lhsTime < rhsTime) return 1;
								
								if (lhs.count_replies > rhs.count_replies)
									return -1;
								else if (lhs.count_replies < rhs.count_replies)
									return 1;
							}
							return 0;
						}
					});
				
				for (int i=0; entries != null && i < entries.length; i++) { 
					YTopicEntry entry = entries[i];
					if (entry != null)
						postAddTopicItem(callback, newTopicItem(entry));
				}
			}
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.TOPIC, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	postNotifyChanged();
    }
	
	private TopicItem newTopicItem(YTopicEntry entry) { 
		return entry != null ? new TopicItem(this, entry) : null;
	}
	
}
