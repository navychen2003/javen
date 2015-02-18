package org.javenstudio.cocoka.widget.model;

import android.view.View;

import org.javenstudio.cocoka.widget.PopupMenu;
import org.javenstudio.cocoka.widget.activity.NavigationActivity;

public abstract class NavigationController extends Controller {

	private final NavigationModel mModel; 
	
	public NavigationController(NavigationModel model) {
		mModel = model; 
	} 
	
	public NavigationModel getNavigationModel() { 
		return mModel; 
	}
	
	@Override 
	public Model getModel() { 
		return getNavigationModel(); 
	}
	
	public NavigationItem getSelectedItem() { 
		NavigationItem[] items = getNavigationModel().getNavigationItems(); 
		NavigationItem selected = null; 
		
		if (selected == null) { 
			NavigationItem first = null; 
			
			for (int i=0; items != null && i < items.length; i++) { 
				NavigationItem item = items[i]; 
				if (item == null) continue; 
				if (item.isSelected()) { 
					selected = item; 
					break; 
				}
				if (first == null) 
					first = item; 
			}
			
			if (selected == null) 
				selected = getDefaultSelectedItem(first); 
		}
		
		if (selected != null && selected instanceof NavigationGroup) { 
			NavigationGroup group = (NavigationGroup)selected; 
			selected = group.getSelected(); 
		}
		
		checkItemSelected(selected);
		
		return selected; 
	}
	
	private void checkItemSelected(NavigationItem item) { 
		if (item != null) { 
			NavigationItem parent = item.getParent(); 
			if (parent != null && !parent.isSelected()) 
				parent.setSelected(true); 
			if (!item.isSelected()) 
				item.setSelected(true); 
		}
	}
	
	public NavigationItem getCurrentItem() { 
		return null;
	}
	
	protected NavigationItem getDefaultSelectedItem(NavigationItem first) { 
		return null; 
	}
	
	private final void onGroupClicked(NavigationActivity activity, View clickView, NavigationGroup group) { 
		if (activity == null || clickView == null || group == null) 
			return; 
		
		PopupMenu menu = activity.getGroupMenu(); 
		if (menu == null) 
			return; 
		
		if (!menu.isShowing()) { 
			if (initGroupMenuItems(activity, group)) { 
				activity.showGroupMenu(group, clickView); 
				return; 
			}
		}
		
		activity.hideGroupMenu(); 
	}
	
	public final boolean initGroupMenuItems(final NavigationActivity activity, final NavigationGroup group) { 
		if (activity == null || group == null) 
			return false; 
		
		final PopupMenu menu = activity.getGroupMenu(); 
		if (menu == null) 
			return false; 
		
		boolean result = activity.clearGroupMenuItems(this, menu);
		
		for (int i=0; i < group.getChildCount(); i++) { 
			final NavigationItem item = group.getChildAt(i); 
			if (item == null) continue; 
			
			final View button = activity.addGroupMenuItem(this, item, menu); 
			if (button != null) { 
				result = true;
				button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							onGroupItemClicked(activity, item); 
						}
					});
			}
		}
		
		return result; 
	}
	
	protected void onGroupItemClicked(NavigationActivity activity, NavigationItem item) { 
		if (activity == null || item == null) 
			return; 
		
		final NavigationGroup group = item.getParent(); 
		if (group != null) { 
			group.setChildSelected(item); 
			refreshContentView(activity, item); 
		}
		
		activity.hideGroupMenu(); 
	}
	
	public final void onItemClicked(NavigationActivity activity, View clickView, NavigationItem item) { 
		if (activity == null || clickView == null || item == null) 
			return; 
		
		NavigationItem[] items = getNavigationModel().getNavigationItems(); 
		for (int i=0; items != null && i < items.length; i++) { 
			NavigationItem it = items[i]; 
			if (it != null) 
				it.setSelected(it == item); 
		}
		
		if(item instanceof NavigationGroup) { 
			onGroupClicked(activity, clickView, (NavigationGroup)item); 
			
		} else { 
			refreshContentView(activity, item); 
		}
	}
	
	public final void refreshContentView(NavigationActivity activity, NavigationItem item) { 
		if (activity == null || item == null) 
			return;
		
		activity.initHeaderTitle(this, item); 
		activity.initContentView(this, item); 
		refreshContent(activity, false); 
	}
	
	public final void refreshContent(NavigationActivity activity, boolean force) { 
		getNavigationModel().reload(activity, force); 
	}
	
	public final void abortRefresh() { 
		getNavigationModel().stop(); 
	}
	
	public void openNavigationItem(NavigationItem item) { 
		if (item == null || item == getCurrentItem()) 
			return;
		
		NavigationActivity activity = getNavigationModel().getNavigationActivity();
		if (activity != null) { 
			checkItemSelected(item);
			refreshContentView(activity, item);
			activity.resetNavigationBar();
		}
	}
	
}
