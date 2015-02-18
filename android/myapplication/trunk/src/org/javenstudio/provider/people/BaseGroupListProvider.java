package org.javenstudio.provider.people;

import org.javenstudio.provider.people.group.GroupFactory;
import org.javenstudio.provider.people.group.GroupListProvider;

public class BaseGroupListProvider extends GroupListProvider {

	public BaseGroupListProvider(String name, int iconRes, 
			GroupFactory factory) { 
		super(name, iconRes, factory);
	}
	
}
