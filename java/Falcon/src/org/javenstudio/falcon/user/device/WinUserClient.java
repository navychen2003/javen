package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.Member;

public class WinUserClient extends BaseUserClient {

	public WinUserClient(Member user, WinDevice device, 
			String token) throws ErrorException {
		super(user, device, token);
	}
	
}
