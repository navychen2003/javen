package org.javenstudio.falcon.message;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.table.TMessageHelper;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IUser;

public final class MessageManager {
	private static final Logger LOG = Logger.getLogger(MessageManager.class);

	public static final String TYPE_MAIL = "mail";
	public static final String TYPE_CHAT = "chat";
	public static final String TYPE_NOTICE = "notice";
	
	private final IUser mUser;
	private final MessageJob mJob;
	
	private final Map<String, IMessageService> mServices = 
			new HashMap<String, IMessageService>();
	
	private volatile boolean mClosed = false;
	
	public MessageManager(IUser user) {
		if (user == null) throw new NullPointerException();
		mUser = user;
		mJob = new MessageJob(this);
		
		if (user instanceof IGroup) {
			addService(TMessageHelper.createChat(this));
			addService(TMessageHelper.createNotice(this));
		} else {
			addService(TMessageHelper.createMail(this));
			addService(TMessageHelper.createNotice(this));
		}
	}
	
	public IUser getUser() { return mUser; }
	public MessageJob getJob() { return mJob; }
	
	private void addService(IMessageService service) {
		if (service == null) return;
		
		synchronized (mServices) { 
			final String type = service.getType();
			IMessageService ms = mServices.get(type);
			if (ms == service) return;
			if (ms != null && ms != service) {
				throw new IllegalArgumentException("Service: " 
						+ type + " already existed");
			}
			mServices.put(type, service);
		}
	}
	
	public IMessageSet getNewMessages() throws ErrorException {
		if (mUser instanceof IGroup) 
			return getService(TYPE_CHAT).getNewMessages();
		else
			return getService(TYPE_MAIL).getNewMessages();
	}
	
	public IMessageService getService(String type) { 
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
			getJob().close();
			
			IMessageService[] services = mServices.values()
					.toArray(new IMessageService[mServices.size()]);
			
			if (services != null) {
				for (IMessageService service : services) { 
					if (service != null) service.close();
				}
			}
		}
	}
	
	public boolean isClosed() { 
		return mClosed; 
	}
	
}
