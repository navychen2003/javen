package org.javenstudio.falcon.publication;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.publication.table.TPublicationHelper;

public class PublicationManager {
	private static final Logger LOG = Logger.getLogger(PublicationManager.class);

	public static final String TYPE_APP = "app";
	public static final String TYPE_SUBSCRIPTION = "subscription";
	public static final String TYPE_POST = "post";
	public static final String TYPE_FEATURED = "featured";
	public static final String TYPE_COMMENT = "comment";
	
	private final IPublicationStore mStore;
	
	private final Map<String, IPublicationService> mServices = 
			new HashMap<String, IPublicationService>();
	
	private volatile boolean mClosed = false;
	
	public PublicationManager(IPublicationStore store) {
		if (store == null) throw new NullPointerException();
		mStore = store;
	}
	
	public IPublicationStore getStore() { return mStore; }
	
	public void initUserServices() {
		synchronized (mServices) { 
			if (mServices.size() > 0) {
				throw new IllegalArgumentException("Service already inited");
			}
			addService(TPublicationHelper.createComment(this));
			addService(TPublicationHelper.createPost(this));
			addService(TPublicationHelper.createSubscription(this));
		}
	}
	
	public void initSystemServices() {
		synchronized (mServices) { 
			if (mServices.size() > 0) {
				throw new IllegalArgumentException("Service already inited");
			}
			addService(TPublicationHelper.createComment(this));
			addService(TPublicationHelper.createPost(this));
			addService(TPublicationHelper.createSubscription(this));
			addService(TPublicationHelper.createFeatured(this));
			addService(TPublicationHelper.createApp(this));
		}
	}
	
	private void addService(IPublicationService service) {
		if (service == null) return;
		
		synchronized (mServices) { 
			final String type = service.getType();
			IPublicationService ms = mServices.get(type);
			if (ms == service) return;
			if (ms != null && ms != service) {
				throw new IllegalArgumentException("Service: " 
						+ type + " already existed");
			}
			mServices.put(type, service);
		}
	}
	
	public IPublicationService getService(String type) { 
		if (type == null || type.length() == 0)
			return null;
		
		synchronized (mServices) { 
			return mServices.get(type);
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
		
		synchronized (mServices) { 
			IPublicationService[] services = mServices.values()
					.toArray(new IPublicationService[mServices.size()]);
			
			if (services != null) {
				for (IPublicationService service : services) { 
					if (service != null) service.close();
				}
			}
		}
	}
	
	public boolean isClosed() { 
		return mClosed; 
	}
	
}
