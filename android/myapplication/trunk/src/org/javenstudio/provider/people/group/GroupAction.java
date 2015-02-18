package org.javenstudio.provider.people.group;

import org.javenstudio.provider.ProviderActionTabBase;

public class GroupAction extends ProviderActionTabBase {
	//private static final Logger LOG = Logger.getLogger(GroupAction.class);

	public GroupAction(GroupItem item, String name) { 
		this(item, name, 0);
	}
	
	public GroupAction(GroupItem item, String name, int iconRes) { 
		super(item, name, iconRes);
	}

}
