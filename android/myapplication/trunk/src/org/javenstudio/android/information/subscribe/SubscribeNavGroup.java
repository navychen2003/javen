package org.javenstudio.android.information.subscribe;

import org.javenstudio.cocoka.widget.model.NavigationGroup;
import org.javenstudio.cocoka.widget.model.NavigationInfo;

public class SubscribeNavGroup extends NavigationGroup {

	private boolean mLoading = false; 
	
	public SubscribeNavGroup(NavigationInfo info) { 
		this(info, false); 
	}
	
	public SubscribeNavGroup(NavigationInfo info, boolean selected) { 
		super(info, selected); 
	}
	
	public boolean isLoading() { 
		return mLoading && getChildCount() == 0; 
	}
	
	public void setLoading(boolean loading) { 
		mLoading = loading; 
	}
	
}
