package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.NamedHelper;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserSpaceHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserSpaceHandler(core);
	}
	
	public UserSpaceHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "info";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("info")) { 
			handleInfo(req, rsp, user);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleInfo(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		IMember user = me;
		NamedMap<Object> infos = NamedHelper.getMemberSpaceInfos(user);
		
		rsp.add("spaceinfo", infos);
	}
	
}
