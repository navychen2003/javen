package org.javenstudio.cocoka.widget.model;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;

public class NavigationGroup extends NavigationItem {

	private final List<NavigationItem> mItems; 
	
	public NavigationGroup(NavigationInfo info) { 
		this(info, false); 
	}
	
	public NavigationGroup(NavigationInfo info, boolean selected) { 
		super(info, selected); 
		
		mItems = new ArrayList<NavigationItem>(); 
	}
	
	@Override 
	public void clearDataSets() { 
		// do nothing
	}
	
	@Override 
	public AbstractDataSets<?> getDataSets() { 
		return null; 
	}
	
	public final void clearChildItems() { 
		synchronized (mItems) { 
			mItems.clear(); 
		}
	}
	
	public final void addChild(NavigationItem item) { 
		if (item == null) return; 
		
		synchronized (mItems) { 
			for (NavigationItem it : mItems) { 
				if (item == it) 
					return; 
			}
			mItems.add(item); 
			item.setParent(this); 
		}
	}
	
	public final int getChildCount() { 
		synchronized (mItems) { 
			return mItems.size(); 
		}
	}
	
	public final NavigationItem getChildAt(int position) { 
		synchronized (mItems) { 
			return position >= 0 && position < mItems.size() ? mItems.get(position) : null; 
		}
	}
	
	public NavigationItem getSelected() { 
		synchronized (mItems) { 
			for (NavigationItem it : mItems) { 
				if (it.isSelected()) return it; 
			}
			return getChildAt(0); 
		}
	}
	
	public final void setChildSelected(NavigationItem item) { 
		if (item == null) return; 
		
		synchronized (mItems) { 
			boolean found = false; 
			for (NavigationItem it : mItems) { 
				if (it == item) { found = true; break; } 
			}
			if (found) { 
				for (NavigationItem it : mItems) { 
					it.setSelected(it == item); 
				}
			}
		}
	}
	
	@Override 
	public boolean shouldReload() { 
		NavigationItem item = getSelected(); 
		if (item != null) 
			return item.shouldReload(); 
		else 
			return false; 
	}
	
}
