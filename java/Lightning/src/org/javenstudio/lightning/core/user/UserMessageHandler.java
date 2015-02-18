package org.javenstudio.lightning.core.user;

import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.IMessageService;
import org.javenstudio.falcon.message.IMessageSet;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.message.MessageQuery;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserMessageHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserMessageHandler(core);
	}
	
	public UserMessageHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "list";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("get")) { 
			handleGet(user, req, rsp);
		} else if (action.equalsIgnoreCase("send")) { 
			handleSend(user, req, rsp);
		} else if (action.equalsIgnoreCase("draft")) { 
			handleDraft(user, req, rsp);
		} else if (action.equalsIgnoreCase("move")) { 
			handleMove(user, req, rsp);
		} else if (action.equalsIgnoreCase("trash")) { 
			handleTrash(user, req, rsp);
		} else if (action.equalsIgnoreCase("delete")) { 
			handleDelete(user, req, rsp);
		} else if (action.equalsIgnoreCase("setflag")) { 
			handleSetflag(user, req, rsp);
		} else if (action.equalsIgnoreCase("list")) { 
			handleList(user, req, rsp);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	static IUser getRequestUser(IMember me, Request req, Response rsp, 
			boolean modify) throws ErrorException {
		String groupname = trim(req.getParam("username"));
		IUser user = me;
		
		if (groupname != null && groupname.length() > 0 && 
			!groupname.equals(me.getUserName()) && !groupname.equals("me")) {
			IUser grp = UserHelper.getLocalUserByName(groupname);
			if (grp != null && grp instanceof IGroup) {
				IGroup group = (IGroup)grp;
				
				if (!me.isManager()) {
					MemberManager mm = group.getMemberManager();
					if (mm != null) { 
						mm.loadMembers(false);
						
						MemberManager.GroupMember gm = mm.getMember(me.getUserKey());
						if (gm == null) { 
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
									"You are not joined group: " + groupname);
						}
						
						boolean isManager = false;
						if (gm != null) { 
							if (MemberManager.ROLE_OWNER.equals(gm.getRole()) || MemberManager.ROLE_MANAGER.equals(gm.getRole()))
								isManager = true;
						}
						
						if (modify && isManager == false) {
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
									"You are not manager of group: " + groupname);
						}
					}
				}
				
				user = group;
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"User: " + groupname + " is not a group");
			}
		}
		
		return user;
	}
	
	private void handleList(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String folder = trim(req.getParam("folder"));
		String chatuser = trim(req.getParam("chatuser"));
		String streamid = trim(req.getParam("streamid"));
		
		String groupby = trim(req.getParam("groupby"));
		int grouprows = parseInt(req.getParam("grouprows"));
		
		long timestart = parseLong(req.getParam("timestart"));
		long timeend = parseLong(req.getParam("timeend"));
		
		int page = parseInt(req.getParam("page"));
		int pagesize = parseInt(req.getParam("pagesize"));
		
		if (page <= 0) page = 1;
		if (pagesize <= 0) pagesize = 20;
		
		int start = (page - 1) * pagesize;
		int count = pagesize;
		
		boolean groupbyStream = false;
		if (groupby != null && groupby.equalsIgnoreCase("stream"))
			groupbyStream = true;
		if (grouprows <= 0) grouprows = 10;
		
		int totalCount = 0;
		String[] folderNames = null;
		IMessage[] messages = null;
		
		IUser chatusr = null;
		IUser user = getRequestUser(me, req, rsp, false);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		if (service != null) { 
			folderNames = service.getFolderNames();
			
			if (streamid != null && streamid.length() > 0) {
				MessageQuery query = new MessageQuery(start, count);
				query.setStreamId(streamid);
				if (timestart > 0 && timeend > timestart)
					query.setTimeRange(timestart, timeend);
				if (groupbyStream) 
					query.setGroupBy(MessageQuery.GroupBy.STREAM, grouprows);
				
				IMessageSet result = service.getMessages(query);
				if (result != null) {
					totalCount = (int)result.getTotalCount();
					messages = result.getMessages();
				}
				
				folder = IMessage.DEFAULT;
				
			} else if (chatuser != null && chatuser.length() > 0) {
				chatusr = UserHelper.getLocalUserByName(chatuser);
				if (chatusr == null) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"User: " + chatuser + " not found");
				}
				
				MessageQuery query = new MessageQuery(start, count);
				query.setChatUser(chatuser);
				if (timestart > 0 && timeend > timestart)
					query.setTimeRange(timestart, timeend);
				if (groupbyStream) 
					query.setGroupBy(MessageQuery.GroupBy.STREAM, grouprows);
				
				IMessageSet result = service.getMessages(query);
				if (result != null) {
					totalCount = (int)result.getTotalCount();
					messages = result.getMessages();
				}
				
				folder = IMessage.DEFAULT;
				
			} else {
				if (folder == null || folder.length() == 0) { 
					if (folderNames != null && folderNames.length > 0)
						folder = folderNames[0];
					else
						folder = IMessage.DEFAULT;
				}
				
				MessageQuery query = new MessageQuery(start, count);
				query.addFolderName(folder);
				if (timestart > 0 && timeend > timestart)
					query.setTimeRange(timestart, timeend);
				if (groupbyStream) 
					query.setGroupBy(MessageQuery.GroupBy.STREAM, grouprows);
				
				IMessageSet result = service.getMessages(query);
				if (result != null) {
					totalCount = (int)result.getTotalCount();
					messages = result.getMessages();
				}
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
		
		int totalPage = totalCount / pagesize;
		if (totalCount % pagesize > 0)
			totalPage += 1;
		
		ArrayList<NamedList<Object>> list = new ArrayList<NamedList<Object>>();
		
		for (int i=0; messages != null && i < messages.length; i++) { 
			IMessage item = messages[i];
			NamedList<Object> info = item != null ? getMessageInfo(item) : null;
			if (info != null && item != null) {
				list.add(info);
				if (list.size() >= count)
					break;
			}
		}
		
		Object[] items = list.toArray(new Object[list.size()]);
		NamedList<Object> userInfo = UserInfoHandler.getUserInfo(user);
		
		rsp.add("user", userInfo);
		rsp.add("type", toString(type));
		rsp.add("folder", toString(folder));
		rsp.add("folders", folderNames);
		rsp.add("groupby", toString(groupby));
		rsp.add("page", page);
		rsp.add("pagesize", pagesize);
		rsp.add("totalpage", totalPage);
		rsp.add("totalcount", totalCount);
		rsp.add("messages", items);
		
		if (chatusr != null) {
			NamedList<Object> chatuserInfo = UserInfoHandler.getUserInfo(chatusr);
			rsp.add("chatuser", chatuserInfo);
		}
		
		if (streamid != null && streamid.length() > 0) 
			rsp.add("streamid", toString(streamid));
	}
	
	private void handleGet(IMember me, Request req, Response rsp)
			throws ErrorException {
		String accesskey = trim(req.getParam("accesskey"));
		String type = trim(req.getParam("type"));
		String folderName = trim(req.getParam("folder"));
		String messageId = trim(req.getParam("messageid"));
		
		rsp.add("type", toString(type));
		rsp.add("folder", toString(folderName));
		rsp.add("messageid", toString(messageId));
		
		IUser user = getRequestUser(me, req, rsp, false);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		if (service != null) { 
			IMessage message = service.getMessage(messageId);
			if (user == me && message != null) {
				if (IMessage.STATUS_NEW.equals(message.getStatus())) {
					IMessage.Builder builder = service.modifyMessage(messageId);
					builder.setStatus(IMessage.STATUS_READ);
					message = builder.save();
					service.flushMessages();
				}
			}
			if (message != null) {
				NamedList<Object> info = getMessageInfo2(message, me, accesskey);
				
				rsp.add("message", info);
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Message: " + messageId + " not found");
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
	}
	
	private void handleDraft(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String draftId = trim(req.getParam("draftid"));
		String replyId = trim(req.getParam("replyid"));
		String streamId = trim(req.getParam("streamid"));
		String to = trim(req.getParam("to"));
		String subject = trim(req.getParam("subject"));
		String body = trim(req.getParam("body"));
		String contentType = trim(req.getParam("ctype"));
		String source = trim(req.getParam("source"));
		String attachments = trim(req.getParam("attachments"));
		
		if (contentType == null || contentType.length() == 0)
			contentType = "text/plain";
		
		if (!contentType.startsWith("text/")) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Body content-type must be text");
		}
		
		IUser user = getRequestUser(me, req, rsp, true);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		String messageId = null;
		boolean result = false;
		
		if (service != null) { 
			final IMessage.Builder builder;
			if (draftId != null && draftId.length() > 0) {
				builder = service.modifyMessage(draftId);
				
			} else {
				String streamKey = MessageHelper.isStreamKeyOkay(streamId) ? 
						streamId : MessageHelper.getStreamKey(replyId);
				builder = service.newMessage(IMessage.DRAFT, streamKey);
				builder.setStreamId(streamKey);
			}
			
			builder.setUpdateTime(System.currentTimeMillis())
				.setStatus(IMessage.STATUS_DRAFT)
				.setContentType(contentType)
				.setReplyId(replyId)
				.setTo(to)
				.setSubject(subject)
				.setBody(body)
				.setAttachmentFiles(MessageHelper.splitValues(attachments))
				.setSourceFile(source);
			
			IMessage message = builder.save();
			if (message != null) {
				service.flushMessages();
				messageId = message.getMessageId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
		
		rsp.add("messageid", toString(messageId));
		rsp.add("result", result);
	}
	
	private void handleSend(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String draftId = trim(req.getParam("draftid"));
		String replyId = trim(req.getParam("replyid"));
		String streamId = trim(req.getParam("streamid"));
		String to = trim(req.getParam("to"));
		String subject = trim(req.getParam("subject"));
		String body = trim(req.getParam("body"));
		String contentType = trim(req.getParam("ctype"));
		String source = trim(req.getParam("source"));
		String attachments = trim(req.getParam("attachments"));
		
		if (contentType == null || contentType.length() == 0)
			contentType = "text/plain";
		
		if (!contentType.startsWith("text/")) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Body content-type must be text");
		}
		
		
		IUser user = getRequestUser(me, req, rsp, true);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		String messageId = null;
		boolean result = false;
		
		if (service != null) { 
			boolean replyfound = false;
			if (user == me && replyId != null && replyId.length() > 0) {
				IMessage message = service.getMessage(replyId);
				if (message != null) {
					replyfound = true;
					if (IMessage.STATUS_NEW.equals(message.getStatus()) ||
						IMessage.STATUS_READ.equals(message.getStatus())) {
						IMessage.Builder builder = service.modifyMessage(replyId);
						builder.setStatus(IMessage.STATUS_REPLIED);
						builder.save();
					}
				}
			}
			
			final IMessage.Builder builder;
			if (draftId != null && draftId.length() > 0) {
				builder = service.modifyMessage(draftId);
				
			} else {
				String streamKey = MessageHelper.isStreamKeyOkay(streamId) ? 
						streamId : MessageHelper.getStreamKey(replyId);
				builder = service.newMessage(IMessage.OUTBOX, streamKey);
				builder.setStreamId(streamKey);
			}
			
			if (replyfound == false) replyId = null;
			
			builder.setUpdateTime(System.currentTimeMillis())
				.setContentType(contentType)
				.setReplyId(replyId)
				.setTo(to)
				.setSubject(subject)
				.setBody(body)
				.setAttachmentFiles(MessageHelper.splitValues(attachments))
				.setSourceFile(source);
			
			IMessage message = service.postSend(builder);
			if (message != null) {
				service.flushMessages();
				messageId = message.getMessageId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
		
		rsp.add("messageid", toString(messageId));
		rsp.add("result", result);
	}
	
	private void handleMove(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String messageId = trim(req.getParam("messageid"));
		//String folder = trim(req.getParam("folder"));
		String folderto = trim(req.getParam("folderto"));
		
		IUser user = getRequestUser(me, req, rsp, true);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		boolean result = false;
		
		if (service != null) { 
			IMessage message = service.moveMessage(messageId, folderto);
			if (message != null) {
				service.flushMessages();
				messageId = message.getMessageId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
		
		rsp.add("messageid", toString(messageId));
		rsp.add("result", result);
	}
	
	private void handleTrash(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String messageId = trim(req.getParam("messageid"));
		//String folder = trim(req.getParam("folder"));
		String folderto = IMessage.TRASH;
		
		IUser user = getRequestUser(me, req, rsp, true);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		boolean result = false;
		
		if (service != null) { 
			IMessage message = service.moveMessage(messageId, folderto);
			if (message != null) {
				service.flushMessages();
				messageId = message.getMessageId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
		
		rsp.add("messageid", toString(messageId));
		rsp.add("result", result);
	}
	
	private void handleDelete(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String messageId = trim(req.getParam("messageid"));
		//String folder = trim(req.getParam("folder"));
		
		IUser user = getRequestUser(me, req, rsp, true);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		boolean result = false;
		
		if (service != null) { 
			IMessage message = service.deleteMessage(messageId);
			if (message != null) {
				service.flushMessages();
				messageId = message.getMessageId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
		
		rsp.add("messageid", toString(messageId));
		rsp.add("result", result);
	}
	
	private void handleSetflag(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String messageId = trim(req.getParam("messageid"));
		String flag = trim(req.getParam("flag"));
		
		if (flag != null && flag.equalsIgnoreCase("null"))
			flag = "";
		
		IUser user = getRequestUser(me, req, rsp, true);
		if (user == null) user = me;
		
		IMessageService service = user.getMessageManager().getService(type);
		boolean result = false;
		
		if (service != null) { 
			IMessage.Builder builder = service.modifyMessage(messageId);
			builder.setFlag(flag != null ? flag : "");
			
			IMessage message = builder.save();
			if (message != null) {
				service.flushMessages();
				messageId = message.getMessageId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No message service: " + type + " found for user: " 
					+ user.getUserName());
		}
		
		rsp.add("messageid", toString(messageId));
		rsp.add("result", result);
	}
	
}
