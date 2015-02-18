package org.javenstudio.lightning.core.user;

import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.IPublicationService;
import org.javenstudio.falcon.publication.IPublicationSet;
import org.javenstudio.falcon.publication.PublicationHelper;
import org.javenstudio.falcon.publication.PublicationQuery;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserPublishHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserPublishHandler(core);
	}
	
	public UserPublishHandler(UserCore core) { 
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
		} else if (action.equalsIgnoreCase("draft")) { 
			handleDraft(user, req, rsp);
		} else if (action.equalsIgnoreCase("save")) { 
			handleSave(user, req, rsp);
		} else if (action.equalsIgnoreCase("move")) { 
			handleMove(user, req, rsp);
		} else if (action.equalsIgnoreCase("trash")) { 
			handleTrash(user, req, rsp);
		} else if (action.equalsIgnoreCase("delete")) { 
			handleDelete(user, req, rsp);
		} else if (action.equalsIgnoreCase("setflag")) { 
			handleSetflag(user, req, rsp);
		} else if (action.equalsIgnoreCase("getchannels")) { 
			handleGetchannels(user, req, rsp);
		} else if (action.equalsIgnoreCase("list")) { 
			handleList(user, req, rsp);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private IPublicationService getService(IMember me, Request req, Response rsp, 
			String type, boolean modify) throws ErrorException {
		String username = trim(req.getParam("username"));
		if (username != null && username.equals(IUser.SYSTEM)) {
			if (!me.isManager()) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"No permission: not administrator");
			}
			
			return getManager().getPublicationManager().getService(type);
		}
		
		IUser user = UserMessageHandler.getRequestUser(me, req, rsp, modify);
		if (user == null) user = me;
		
		IPublicationService service = user.getPublicationManager().getService(type);
		return service;
	}
	
	private void handleGet(IMember me, Request req, Response rsp)
			throws ErrorException {
		String accesskey = trim(req.getParam("accesskey"));
		String type = trim(req.getParam("type"));
		String channelName = trim(req.getParam("channel"));
		String publishId = trim(req.getParam("publishid"));
		
		rsp.add("type", toString(type));
		rsp.add("channel", toString(channelName));
		rsp.add("publishid", toString(publishId));
		
		IPublicationService service = getService(me, req, rsp, type, false);
		if (service != null) { 
			rsp.add("username", toString(service.getManager().getStore().getUserName()));
			
			IPublication publication = service.getPublication(publishId);
			if (publication != null) {
				NamedList<Object> info = getPublicationInfo2(publication, me, accesskey);
				
				rsp.add("publication", info);
			} else {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Publication: " + publishId + " not found");
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No publication service: " + type + " found");
		}
	}
	
	private void handleDelete(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String publishId = trim(req.getParam("publishid"));
		//String channel = trim(req.getParam("channel"));
		
		IPublicationService service = getService(me, req, rsp, type, true);
		boolean result = false;
		
		if (service != null) { 
			rsp.add("username", toString(service.getManager().getStore().getUserName()));
			
			IPublication publication = service.deletePublication(publishId);
			if (publication != null) {
				service.flushPublications();
				publishId = publication.getId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No publication service: " + type + " found");
		}
		
		rsp.add("publicationid", toString(publishId));
		rsp.add("result", result);
	}
	
	private void handleSetflag(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String publishId = trim(req.getParam("publishid"));
		String flag = trim(req.getParam("flag"));
		
		if (flag != null && flag.equalsIgnoreCase("null"))
			flag = "";
		
		IPublicationService service = getService(me, req, rsp, type, true);
		boolean result = false;
		
		if (service != null) { 
			rsp.add("username", toString(service.getManager().getStore().getUserName()));
			
			IPublication.Builder builder = service.modifyPublication(publishId);
			builder.setAttr(IPublication.ATTR_FLAG, flag != null ? flag : "");
			
			IPublication publication = builder.save();
			if (publication != null) {
				service.flushPublications();
				publishId = publication.getId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No publication service: " + type + " found");
		}
		
		rsp.add("publishid", toString(publishId));
		rsp.add("result", result);
	}
	
	private void handleMove(IMember me, Request req, Response rsp)
			throws ErrorException {
		String channelto = trim(req.getParam("channelto"));
		doMove(me, req, rsp, channelto);
	}
	
	private void handleTrash(IMember me, Request req, Response rsp)
			throws ErrorException {
		doMove(me, req, rsp, IPublication.TRASH);
	}
	
	private void doMove(IMember me, Request req, Response rsp, 
			String channelto) throws ErrorException {
		String type = trim(req.getParam("type"));
		String publishId = trim(req.getParam("publishid"));
		//String channel = trim(req.getParam("channel"));
		//String channelto = trim(req.getParam("channelto"));
		
		IPublicationService service = getService(me, req, rsp, type, true);
		boolean result = false;
		
		if (service != null) { 
			rsp.add("username", toString(service.getManager().getStore().getUserName()));
			
			IPublication publication = service.movePublication(publishId, channelto);
			if (publication != null) {
				service.flushPublications();
				publishId = publication.getId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No publication service: " + type + " found");
		}
		
		rsp.add("publishid", toString(publishId));
		rsp.add("result", result);
	}
	
	private void handleDraft(IMember me, Request req, Response rsp)
			throws ErrorException {
		String publishId = trim(req.getParam("publishid"));
		String channel = trim(req.getParam("channel"));
		doSave(me, req, rsp, publishId, IPublication.DRAFT, channel, 
				IPublication.STATUS_DRAFT);
	}
	
	private void handleSave(IMember me, Request req, Response rsp)
			throws ErrorException {
		String publishId = trim(req.getParam("publishid"));
		String channel = trim(req.getParam("channel"));
		String channelFrom = trim(req.getParam("channelfrom"));
		doSave(me, req, rsp, publishId, channel, channelFrom, null);
	}
	
	private void doSave(IMember me, Request req, Response rsp, 
			String publishId, String channel, String channelFrom, 
			String status) throws ErrorException {
		String type = trim(req.getParam("type"));
		String replyId = trim(req.getParam("replyid"));
		String streamId = trim(req.getParam("streamid"));
		String tags = trim(req.getParam("tags"));
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
		
		IPublicationService service = getService(me, req, rsp, type, true);
		boolean result = false;
		
		if (service != null) { 
			rsp.add("username", toString(service.getManager().getStore().getUserName()));
			
			final IPublication.Builder builder;
			if (publishId != null && publishId.length() > 0) {
				builder = service.modifyPublication(publishId);
				
				if (channel != null && channel.length() > 0)
					builder.setAttr(IPublication.ATTR_CHANNEL, channel);
				
			} else {
				String streamKey = PublicationHelper.isStreamKeyOkay(streamId) ? 
						streamId : PublicationHelper.getStreamKey(replyId);
				
				if (channel == null || channel.length() == 0)
					channel = IPublication.DRAFT;
				
				builder = service.newPublication(me, channel, streamKey);
				
				if (streamKey != null && streamKey.length() > 0)
					builder.setAttr(IPublication.ATTR_STREAMID, streamKey);
			}
			
			builder.setAttr(IPublication.ATTR_UPDATETIME, System.currentTimeMillis())
				.setAttr(IPublication.ATTR_REPLYID, replyId)
				.setHeader(IPublication.HEADER_CONTENTTYPE, contentType)
				.setHeader(IPublication.HEADER_TAGS, tags)
				.setHeader(IPublication.HEADER_SUBJECT, subject)
				.setContent(IPublication.CONTENT_BODY, body)
				.setContent(IPublication.CONTENT_ATTACHMENTS, attachments)
				.setContent(IPublication.CONTENT_SOURCE, source);
			
			if (status != null && status.length() > 0)
				builder.setAttr(IPublication.ATTR_STATUS, status);
			
			if (channelFrom != null && channelFrom.length() > 0)
				builder.setAttr(IPublication.ATTR_CHANNELFROM, channelFrom);
			
			IPublication publication = builder.save();
			if (publication != null) {
				service.flushPublications();
				publishId = publication.getId();
				result = true;
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No publication service: " + type + " found");
		}
		
		rsp.add("publishid", toString(publishId));
		rsp.add("result", result);
	}
	
	private void handleGetchannels(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String[] channelNames = null;
		
		IPublicationService service = getService(me, req, rsp, type, false);
		if (service != null) { 
			rsp.add("username", toString(service.getManager().getStore().getUserName()));
			channelNames = service.getChannelNames();
		}
		
		rsp.add("type", toString(type));
		rsp.add("channels", channelNames);
	}
	
	private void handleList(IMember me, Request req, Response rsp)
			throws ErrorException {
		String type = trim(req.getParam("type"));
		String channel = trim(req.getParam("channel"));
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
		String[] channelNames = null;
		IPublication[] publications = null;
		
		IPublicationService service = getService(me, req, rsp, type, false);
		if (service != null) { 
			channelNames = service.getChannelNames();
			
			if (streamid != null && streamid.length() > 0) {
				PublicationQuery query = new PublicationQuery(start, count);
				query.setStreamId(streamid);
				if (timestart > 0 && timeend > timestart)
					query.setTimeRange(timestart, timeend);
				if (groupbyStream) 
					query.setGroupBy(PublicationQuery.GroupBy.STREAM, grouprows);
				
				IPublicationSet result = service.getPublications(query);
				if (result != null) {
					totalCount = (int)result.getTotalCount();
					publications = result.getPublications();
				}
				
				channel = IPublication.DEFAULT;
				
			} else {
				if (channel == null || channel.length() == 0) { 
					if (channelNames != null && channelNames.length > 0)
						channel = channelNames[0];
					else
						channel = IPublication.DEFAULT;
				}
				
				PublicationQuery query = new PublicationQuery(start, count);
				query.addChannelName(channel);
				if (timestart > 0 && timeend > timestart)
					query.setTimeRange(timestart, timeend);
				if (groupbyStream) 
					query.setGroupBy(PublicationQuery.GroupBy.STREAM, grouprows);
				
				IPublicationSet result = service.getPublications(query);
				if (result != null) {
					totalCount = (int)result.getTotalCount();
					publications = result.getPublications();
				}
			}
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"No publication service: " + type + " found");
		}
		
		int totalPage = totalCount / pagesize;
		if (totalCount % pagesize > 0)
			totalPage += 1;
		
		ArrayList<NamedList<Object>> list = new ArrayList<NamedList<Object>>();
		
		for (int i=0; publications != null && i < publications.length; i++) { 
			IPublication item = publications[i];
			NamedList<Object> info = item != null ? getPublicationInfo(item) : null;
			if (info != null && item != null) {
				list.add(info);
				if (list.size() >= count)
					break;
			}
		}
		
		Object[] items = list.toArray(new Object[list.size()]);
		NamedList<Object> userInfo = null; 
		String username = null;
		
		if (service != null) {
			username = service.getManager().getStore().getUserName();
			userInfo = UserInfoHandler.getUserInfo(UserHelper.getLocalUserByName(username));
		}
		
		rsp.add("user", userInfo);
		rsp.add("username", toString(username));
		rsp.add("type", toString(type));
		rsp.add("channel", toString(channel));
		rsp.add("channels", channelNames);
		rsp.add("groupby", toString(groupby));
		rsp.add("page", page);
		rsp.add("pagesize", pagesize);
		rsp.add("totalpage", totalPage);
		rsp.add("totalcount", totalCount);
		rsp.add("publications", items);
		
		if (streamid != null && streamid.length() > 0) 
			rsp.add("streamid", toString(streamid));
	}
	
}
