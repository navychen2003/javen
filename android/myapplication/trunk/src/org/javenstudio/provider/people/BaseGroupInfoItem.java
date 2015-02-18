package org.javenstudio.provider.people;

import org.javenstudio.provider.people.group.GroupInfoItem;
import org.javenstudio.provider.people.group.IGroupData;

public abstract class BaseGroupInfoItem extends GroupInfoItem {

	public BaseGroupInfoItem(BaseGroupInfoProvider p, IGroupData data) { 
		super(p, data);
	}
	
}
