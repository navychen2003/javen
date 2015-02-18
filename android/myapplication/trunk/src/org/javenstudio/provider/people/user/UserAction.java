package org.javenstudio.provider.people.user;

import org.javenstudio.provider.ProviderActionTabBase;

public class UserAction extends ProviderActionTabBase {
	//private static final Logger LOG = Logger.getLogger(UserAction.class);

	public UserAction(UserItem item, String name) { 
		this(item, name, 0);
	}
	
	public UserAction(UserItem item, String name, int iconRes) { 
		super(item, name, iconRes);
	}

}
