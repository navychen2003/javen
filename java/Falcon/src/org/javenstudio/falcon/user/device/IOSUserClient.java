package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.Member;

public class IOSUserClient extends BaseUserClient {

	public IOSUserClient(Member user, IOSDevice device, 
			String token) throws ErrorException {
		super(user, device, token);
	}
	
}
