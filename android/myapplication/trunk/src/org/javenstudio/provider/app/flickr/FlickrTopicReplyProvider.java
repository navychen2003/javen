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
import org.javenstudio.provider.publish.BaseTopicReplyProvider;
import org.javenstudio.provider.publish.discuss.IReplyData;
import org.javenstudio.provider.publish.discuss.ReplyBinder;
import org.javenstudio.provider.publish.discuss.ReplyFactory;
import org.javenstudio.provider.publish.discuss.ReplyItem;
import org.javenstudio.provider.publish.discuss.ReplyProvider;

public class FlickrTopicReplyProvider extends BaseTopicReplyProvider {
	private static final Logger LOG = Logger.getLogger(FlickrTopicProvider.class);

	private final FlickrUserClickListener mUserClickListener;
	private final String mTopicId;
	private final String mLocation;
	private boolean mReloaded = false;
	
	public FlickrTopicReplyProvider(String topicId, String name, int iconRes, 
			FlickrUserClickListener listener) { 
		super(name, iconRes, new FlickrReplyFactory());
		mTopicId = topicId;
		mUserClickListener = listener;
		mLocation = FlickrHelper.GROUP_REPLIES_URL + topicId;
	}
	
	static class FlickrReplyFactory extends ReplyFactory { 
		@Override
		public ReplyBinder createReplyBinder(ReplyProvider p) { 
			return new FlickrTopicReplyBinder((FlickrTopicReplyProvider)p);
		}
	}
	
	public String getTopicId() { return mTopicId; }
	public FlickrUserClickListener getUserClickListener() { return mUserClickListener; }

	private String getReloadLocation(ReloadCallback callback) { 
		String location = mLocation;
		return location;
	}
	
	@Override
	public synchronized void reloadOnThread(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		final String location = getReloadLocation(callback);
    	
    	if (mReloaded == false || type == ReloadType.FORCE) {
	    	if (type == ReloadType.FORCE || getReplyDataSets().getCount() <= 0) {
				HtmlCallback cb = new HtmlCallback() {
						@Override
						public void onHtmlFetched(String content) {
							onFetched(callback, location, content); 
						}
						@Override
						public void onHttpException(HttpException e) { 
							if (e == null) return;
							callback.onActionError(new ActionError(ActionError.Action.TOPIC_REPLY, e)); 
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
			ArrayList<YTopicReplyEntry> entryList = new ArrayList<YTopicReplyEntry>();
			YTopicReplyEntry.TopicInfo topicInfo = null;
			
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node rspChild = xml.getChildAt(i);
				if (rspChild != null && "replies".equalsIgnoreCase(rspChild.getName())) {
					for (int j=0; j < rspChild.getChildCount(); j++) {
						Node child = rspChild.getChildAt(j);
						if (child == null) continue;
						if ("topic".equalsIgnoreCase(child.getName())) {
							topicInfo = YTopicReplyEntry.parseInfo(child, 
									getUserClickListener(), getIconRes());
							
						} else if ("reply".equalsIgnoreCase(child.getName())) {
							try { 
								YTopicReplyEntry entry = YTopicReplyEntry.parseEntry(child, 
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
			
			if (topicInfo != null) 
				postAddReplyItem(callback, newReplyItem(topicInfo));
			
			if (entryList.size() > 0) { 
				YTopicReplyEntry[] entries = entryList.toArray(new YTopicReplyEntry[entryList.size()]);
				Arrays.sort(entries, new Comparator<YTopicReplyEntry>() {
						@Override
						public int compare(YTopicReplyEntry lhs, YTopicReplyEntry rhs) {
							if (lhs != null || rhs != null) { 
								if (lhs == null) return -1;
								if (rhs == null) return 1;
								
								long lhsTime = lhs.datecreate; 
								long rhsTime = rhs.datecreate;
								
								if (lhsTime > rhsTime) return -1;
								else if (lhsTime < rhsTime) return 1;
							}
							return 0;
						}
					});
				
				for (int i=0; entries != null && i < entries.length; i++) { 
					YTopicReplyEntry entry = entries[i];
					if (entry != null)
						postAddReplyItem(callback, newReplyItem(entry));
				}
			}
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.TOPIC_REPLY, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	postNotifyChanged();
    }
	
	private ReplyItem newReplyItem(IReplyData entry) { 
		return entry != null ? new ReplyItem(this, entry) : null;
	}
	
}
