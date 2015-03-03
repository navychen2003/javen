package org.javenstudio.falcon.message;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.cluster.IClusterManager;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.setting.cluster.IHostInfo;
import org.javenstudio.falcon.setting.cluster.IHostUserName;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserName;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.job.JobContext;

public class MessageSender implements MessageSource {
	private static final Logger LOG = Logger.getLogger(MessageSender.class);

	private static enum Status { SENDING, SENT, FAILED }
	
	public static class FailedResult {
		private final IUser mUser;
		private final Throwable mException;
		
		private FailedResult(IUser user, Throwable e) {
			if (user == null) throw new NullPointerException();
			mUser = user;
			mException = e;
		}
		
		public final IUser getUser() { return mUser; }
		public final Throwable getException() { return mException; }
		
		public String toDetails() {
			StringBuilder sbuf = new StringBuilder();
			sbuf.append("Send to ");
			sbuf.append(mUser.getUserName());
			sbuf.append(" failed:\n");
			Throwable e = mException;
			if (e != null) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				pw.flush();
				sbuf.append(sw.toString());
			}
			return sbuf.toString();
		}
	}
	
	private final List<FailedResult> mFailes = 
			new ArrayList<FailedResult>();
	
	private String mSentSubject = null;
	private String mSentBody = null;
	private int mSentCount = 0;
	
	private final IMessage mMsg;
	
	public MessageSender(IMessage message) {
		if (message == null) throw new NullPointerException();
		mMsg = message;
	}
	
	public IMessage getMsg() { return mMsg; }
	public IMessageService getService() { return getMsg().getService(); }
	public IUser getUser() { return getService().getManager().getUser(); }
	
	protected final boolean sendtoLocal(IUser user) throws ErrorException { 
		if (user == null) return false;
		
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("sendto: send message: " + getMsg() + " to user: " + user);
			
			if (user instanceof IGroup) {
				IGroup group = (IGroup)user;
				boolean isGroupMember = false;
				
				MemberManager manager = group.getMemberManager();
				if (manager != null) { 
					manager.loadMembers(false);
					
					MemberManager.GroupMember gm = manager.getMember(getUser().getUserKey());
					if (gm != null) isGroupMember = true;
				}
				
				if (isGroupMember == false) {
					throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
							"User: " + getUser().getUserName() + " is not member of group: " 
									+ group.getUserName());
				}
				
				return doSendtoLocal(user, MessageManager.TYPE_CHAT, 
						IMessage.INBOX);
			} else {
				
				return doSendtoLocal(user, MessageManager.TYPE_MAIL, 
						IMessage.INBOX);
			}
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("sendto: user=" + user + " error: " + e, e);
			
			synchronized (mFailes) {
				mFailes.add(new FailedResult(user, e));
			}
			
			return false;
		}
	}
	
	protected int doSend(MessageJob job, JobContext jc) 
			throws ErrorException {
		if (!IMessage.STATUS_QUEUED.equals(getMsg().getStatus())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Message: " + getMsg().getMessageId() + " has wrong status: " 
							+ getMsg().getStatus());
		}
		
		if (!getUser().getUserName().equals(getMsg().getFrom())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Message: " + getMsg().getMessageId() + " has wrong FROM: " 
							+ getMsg().getFrom());
		}
		
		Map<String,IUser> usersLocal = new HashMap<String,IUser>();
		Map<String,IHostUserName> usersHttp = new HashMap<String,IHostUserName>();
		
		String to = getMsg().getTo();
		String[] usernames = MessageHelper.splitAddresses(to);
		int sentcount = 0;
		
		if (usernames != null) { 
			IClusterManager cm = getUser().getUserManager().getStore().getClusterManager();
			IHostCluster cluster = cm.getClusterSelf();
			
			for (String username : usernames) { 
				if (job.isCanceled() || jc.isCancelled()) 
					return sentcount;
				
				if (username == null || username.length() == 0)
					continue;
				
				if (usersLocal.containsKey(username) || usersHttp.containsKey(username))
					continue;
				
				IUserName uname = cm.getClusterSelf().parseUserName(username);
				if (uname == null) continue;
				
				IHostUserName userName = cluster.getHostUserName(uname);
				IHostInfo userHost = userName != null ? userName.getHostNode() : null;
				
				if (userName == null || userHost == null) {
					//throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
					//		"Host of user: " + reqfrom + " not found");
					//continue;
				} else {
					usersHttp.put(username, userName);
					continue;
				}
				
				IUser user = UserHelper.getLocalUserByName(username);
				if (user != null)
					usersLocal.put(user.getUserName(), user);
			}
		}
		
		if (usersLocal.size() == 0) { 
			//throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
			//		"Message: " + getMsg().getMessageId() + " has wrong TO: " 
			//				+ getMsg().getTo());
			
			if (LOG.isWarnEnabled()) {
				LOG.warn("doSend: user: " + getUser().getUserName() 
						+ " message: " + getMsg().getMessageId() + " has wrong TO: " + to);
			}
			
			IMessage.Builder builder = getService().modifyMessage(getMsg().getMessageId());
			builder.setStatus(IMessage.STATUS_FAILED);
			builder.setMessageTime(System.currentTimeMillis());
			builder.save();
			
		} else {
			for (IUser user : usersLocal.values()) { 
				if (job.isCanceled() || jc.isCancelled()) return sentcount;
				if (sendtoLocal(user)) sentcount ++;
			}
			
			if (sentcount > 0) {
				IMessage.Builder builder = getService().modifyMessage(getMsg().getMessageId());
				builder.setStatus(IMessage.STATUS_SENT);
				builder.setMessageTime(System.currentTimeMillis());
				builder.save();
				
			} else {
				if (LOG.isWarnEnabled()) {
					LOG.warn("doSend: user: " + getUser().getUserName() 
							+ " message: " + getMsg().getMessageId() + " cannot send to: " + to);
				}
				
				IMessage.Builder builder = getService().modifyMessage(getMsg().getMessageId());
				builder.setStatus(IMessage.STATUS_FAILED);
				builder.setMessageTime(System.currentTimeMillis());
				builder.save();
			}
		}
		
		return sentcount;
	}
	
	protected boolean doSendtoLocal(IUser user, String type, String folder) 
			throws IOException, ErrorException { 
		if (user == null || type == null || folder == null) 
			return false;
		
		IMessageService service = user.getMessageManager().getService(type);
		if (service != null) { 
			IMessage.Builder builder = service.newMessage(IMessage.INBOX, 
					getMsg().getStreamId());
			
			if (builder != null) {
				builder.setFrom(getMsg().getFrom());
				builder.setTo(getMsg().getTo());
				builder.setCc(getMsg().getCc());
				//builder.setBcc(getMsg().getBcc());
				builder.setReplyTo(getMsg().getReplyTo());
				builder.setSubject(getMsg().getSubject());
				//builder.setHeaderLines(getMsg().getHeaderLines());
				builder.setBody(getMsg().getBody());
				builder.setContentType(getMsg().getContentType());
				builder.setAttachmentFiles(getMsg().getAttachmentFiles());
				builder.setStreamId(getMsg().getStreamId());
				builder.setStatus(IMessage.STATUS_NEW);
				
				if (MessageManager.TYPE_CHAT.equals(type))
					builder.setReplyTo(user.getUserName());
				
				if (builder.save() != null) return true;
				return false;
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"User: " + user.getUserName() + " has no message folder: " 
						+ type + "/" + folder);
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + user.getUserName() + " has no message service: " + type);
		}
	}
	
	private synchronized void setMessage(Status status, 
			Throwable e) throws ErrorException { 
		String text = null;
		String failes = null;
		String error = null;
		
		synchronized (mFailes) {
			StringBuilder errors = new StringBuilder();
			if (e != null) {
				StringWriter sbuf = new StringWriter();
				PrintWriter pw = new PrintWriter(sbuf);
				e.printStackTrace(pw);
				pw.flush();
				
				error = e.getMessage();
				errors.append(sbuf.toString());
				errors.append("\n");
			}
			int count = 0;
			for (FailedResult failed : mFailes) {
				if (failed != null) {
					errors.append(failed.toDetails());
					errors.append("\n");
					count ++;
				}
			}
			failes = errors.toString();
			if (error == null || error.length() == 0) {
				if (count > 0) error = "" + count + " has failed";
			}
		}
		
		switch (status) { 
		case SENDING: {
			text = Strings.get(getUser().getPreference().getLanguage(), 
					"Sending message to \"%1$s\"");
			text = String.format(text, getMsg().getTo());
			if (error != null && error.length() > 0)
				text += ": " + error;
			break;
		}
		case SENT: {
			text = Strings.get(getUser().getPreference().getLanguage(), 
					"Sent message to \"%1$s\"");
			text = String.format(text, getMsg().getTo());
			if (error != null && error.length() > 0)
				text += ": " + error;
			break;
		}
		case FAILED: {
			if (error == null || error.length() == 0)
				error = "has error";
			text = Strings.get(getUser().getPreference().getLanguage(), 
					"Send message to \"%1$s\" failed: %2$s");
			text = String.format(text, getMsg().getTo(), error);
			break;
		}}
		
		mSentSubject = text;
		mSentBody = failes;
	}
	
	@Override
	public synchronized final String getMessage() {
		return mSentSubject;
	}

	@Override
	public synchronized final String getMessageDetails() {
		return mSentBody;
	}
	
	@Override
	public final int getSentCount() {
		return mSentCount;
	}
	
	public final FailedResult[] getFailes() { 
		synchronized (mFailes) {
			return mFailes.toArray(new FailedResult[mFailes.size()]);
		}
	}
	
	@Override
	public final void process(MessageJob job, JobContext jc) 
			throws IOException, ErrorException {
		boolean success = false;
		Throwable exception = null;
		try { 
			setMessage(Status.SENDING, exception);
			int sentcount = doSend(job, jc);
			mSentCount = sentcount;
			success = sentcount > 0 ? true : false;
		} catch (ErrorException ee) {
			exception = ee;
		} finally { 
			setMessage(success?Status.SENT:Status.FAILED, exception);
		}
	}

	@Override
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close");
	}

}
