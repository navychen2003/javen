package org.javenstudio.cocoka.widget.model;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class NavigationController extends Controller {
	private static final Logger LOG = Logger.getLogger(NavigationController.class);
	
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
	
	private final void onGroupClicked(NavigationCallback activity, NavigationGroup group) { 
		if (activity == null || group == null) 
			return; 
		
		NavigationGroupMenu menu = activity.getGroupMenu(group); 
		if (menu == null) 
			return; 
		
		if (!menu.isShowing()) { 
			initGroupMenuItems(activity, group);
			activity.showGroupMenu(menu); 
			return;
		}
		
		activity.hideGroupMenu(menu); 
	}
	
	private final void initGroupMenuItems(NavigationCallback activity, NavigationGroup group) { 
		if (activity == null || group == null) 
			return; 
		
		final NavigationGroupMenu menu = activity.getGroupMenu(group); 
		if (menu == null || menu.isMenuItemInited()) 
			return; 
		
		menu.clear();
		
		for (int i=0; i < group.getChildCount(); i++) { 
			final NavigationItem item = group.getChildAt(i); 
			if (item == null) continue; 
			
			menu.addMenuItem(item);
		}
	}
	
	public final void onGroupItemClicked(NavigationCallback activity, 
			NavigationGroupMenu menu, NavigationItem item) { 
		if (activity == null || menu == null || item == null) 
			return; 
		
		final NavigationGroup group = item.getParent(); 
		if (group != null) { 
			group.setChildSelected(item); 
			refreshContentView(activity, item); 
		}
		
		activity.hideGroupMenu(menu); 
	}
	
	public final void onItemClicked(NavigationCallback activity, NavigationItem item) { 
		if (activity == null || item == null) 
			return; 
		
		NavigationItem[] items = getNavigationModel().getNavigationItems(); 
		for (int i=0; items != null && i < items.length; i++) { 
			NavigationItem it = items[i]; 
			if (it != null) 
				it.setSelected(it == item); 
		}
		
		if (item instanceof NavigationGroup) { 
			onGroupClicked(activity, (NavigationGroup)item); 
			
		} else { 
			refreshContentView(activity, item); 
		}
	}
	
	public final void refreshContentView(NavigationCallback activity, 
			NavigationItem item) { 
		if (activity == null || item == null) 
			return;
		
		activity.initHeaderTitle(item); 
		activity.initContentView(item); 
		refreshContent(activity, false); 
	}
	
	public final void refreshContent(final NavigationCallback callback, final boolean force) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					doRefreshContent(callback, force);
				}
			});
	}
	
	private void doRefreshContent(NavigationCallback callback, boolean force) { 
		//if (!getNavigationModel().isLoaderRunning()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("refreshContent: force=" + force + " running=" 
						+ getNavigationModel().isLoaderRunning());
			}
			getNavigationModel().startLoad(callback, force); 
		//}
	}
	
	public final void abortRefresh() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					doAbortRefresh();
				}
			});
	}
	
	private void doAbortRefresh() { 
		getNavigationModel().stopLoad(); 
	}
	
	public void openNavigationItem(NavigationItem item) { 
		if (item == null || item == getCurrentItem()) 
			return;
		
		NavigationCallback activity = getNavigationModel().getModelCallback();
		if (activity != null) { 
			checkItemSelected(item);
			refreshContentView(activity, item);
			activity.resetNavigationBar();
		}
	}
	
}
