package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.Member;

public class HostUserClient extends BaseUserClient {

	public HostUserClient(Member user, HostDevice device, 
			String token) throws ErrorException {
		super(user, device, token);
	}
	
}
