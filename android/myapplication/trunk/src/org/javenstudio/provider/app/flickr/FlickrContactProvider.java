package org.javenstudio.provider.app.flickr;

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
import org.javenstudio.provider.people.BaseContactProvider;
import org.javenstudio.provider.people.contact.ContactBinder;
import org.javenstudio.provider.people.contact.ContactFactory;
import org.javenstudio.provider.people.contact.ContactItem;
import org.javenstudio.provider.people.contact.ContactProvider;

public class FlickrContactProvider extends BaseContactProvider {
	private static final Logger LOG = Logger.getLogger(FlickrContactProvider.class);

	private final FlickrUserClickListener mUserClickListener;
	private final String mUserId;
	private final String mLocation;
	private boolean mReloaded = false;
	
	public FlickrContactProvider(String userId, 
			String name, int iconRes, FlickrUserClickListener listener) { 
		super(name, iconRes, new FlickrContactFactory());
		mUserId = userId;
		mUserClickListener = listener;
		mLocation = FlickrHelper.USER_CONTACTS_URL + userId;
	}
	
	static class FlickrContactFactory extends ContactFactory { 
		@Override
		public ContactBinder createContactBinder(ContactProvider p) { 
			return new FlickrContactBinder((FlickrContactProvider)p);
		}
	}
	
	public String getUserId() { return mUserId; }
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
	    	if (type == ReloadType.FORCE || getContactDataSets().getCount() <= 0) {
				HtmlCallback cb = new HtmlCallback() {
						@Override
						public void onHtmlFetched(String content) {
							onFetched(callback, location, content); 
						}
						@Override
						public void onHttpException(HttpException e) {
							if (e == null) return;
							callback.onActionError(new ActionError(ActionError.Action.CONTACT, e)); 
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
			for (int i=0; i < xml.getChildCount(); i++) { 
				Node rspChild = xml.getChildAt(i);
				if (rspChild != null && "contacts".equalsIgnoreCase(rspChild.getName())) {
					for (int j=0; j < rspChild.getChildCount(); j++) {
						Node child = rspChild.getChildAt(j);
						if (child != null && "contact".equalsIgnoreCase(child.getName())) {
							try { 
								YContactEntry entry = YContactEntry.parseEntry(child, 
										getUserClickListener(), getIconRes()); 
								if (entry != null)
									postAddContactItem(callback, newContactItem(entry));
							} catch (Throwable e) { 
								if (LOG.isWarnEnabled())
									LOG.warn("parse entry error: " + e.toString(), e); 
							}
						}
					}
				}
			}
    	} catch (Throwable e) { 
			callback.onActionError(new ActionError(ActionError.Action.CONTACT, e));
			
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	postNotifyChanged();
    }
	
	private ContactItem newContactItem(YContactEntry entry) { 
		return entry != null ? new ContactItem(this, entry) : null;
	}
	
}
