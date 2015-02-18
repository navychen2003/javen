package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.Member;

public class AndroidUserClient extends BaseUserClient {

	public AndroidUserClient(Member user, AndroidDevice device, 
			String token) throws ErrorException {
		super(user, device, token);
	}
	
}
