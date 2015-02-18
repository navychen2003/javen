package org.javenstudio.cocoka.widget.activity;

import android.view.View;
import android.widget.PopupWindow;

import org.javenstudio.cocoka.widget.PopupMenu;
import org.javenstudio.cocoka.widget.TextButton;
import org.javenstudio.cocoka.widget.ToolBar;
import org.javenstudio.cocoka.widget.model.ActivityListener;
import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.cocoka.widget.model.NavigationController;
import org.javenstudio.cocoka.widget.model.NavigationGroup;
import org.javenstudio.cocoka.widget.model.NavigationItem;

public abstract class NavigationActivity extends BaseActivity implements Model.Callback, NavigationItem.Callbacks {

	private PopupMenu mGroupMenu = null; 
	private ToolBar mNavigationBar = null; 
	
	@Override 
	public void invokeByModel(int action, Object params) { 
		// do nothing
	}
	
	@Override 
	public void onInvoke(NavigationItem item, int action, Object params) { 
		// do nothing
	}
	
	@Override 
	public void initializeActivityListener(ActivityListener listener) { 
		super.initializeActivityListener(listener); 
		
		listener.registerCallback(this); 
	}
	
	protected final void initNavigationBar(final NavigationController controller, final ToolBar toolbar) { 
		if (controller == null || toolbar == null) 
			return; 
		
		mNavigationBar = toolbar; 
		
		final NavigationItem[] items = controller.getNavigationModel().getNavigationItems(); 
		final NavigationActivity activity = this; 
		
		TextButton selected = null; 
		boolean hasGroup = false; 
		
		for (int i=0; items != null && i < items.length; i++) { 
			final NavigationItem item = items[i]; 
			if (item == null) continue; 
			if (item instanceof NavigationGroup) hasGroup = true; 
			
			final TextButton button = addNavigationBarItem(toolbar, item.getDisplayName()); 
			if (button == null) 
				continue;
			
			button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!button.isChecked() || item instanceof NavigationGroup) { 
							toolbar.setChildChecked(button); 
							controller.onItemClicked(activity, v, item); 
						}
					}
				}); 
			
			item.setCallbacks(activity); 
			item.setBindView(button); 
			
			if (item.isSelected()) 
				selected = button; 
		}
		
		if (selected != null) 
			toolbar.setChildChecked(selected); 
		
		if (hasGroup) { 
			final PopupMenu menu = createGroupMenu(); 
			if (menu != null) { 
				menu.setOnDismissListener(new PopupWindow.OnDismissListener() {
						@Override
						public void onDismiss() {
							onPopupMenuDismiss(menu); 
						}
					});
			}
			mGroupMenu = menu; 
		}
	}
	
	protected abstract TextButton addNavigationBarItem(ToolBar toolbar, String name);
	
	protected void onPopupMenuDismiss(PopupMenu menu) { 
		resetNavigationBar();
	}
	
	public void resetNavigationBar() { 
		final ToolBar toolbar = getNavigationBar(); 
		if (toolbar == null) 
			return; 
		
		final NavigationController controller = getController(); 
		if (controller != null) { 
			NavigationItem item = controller.getCurrentItem(); 
			if (item != null && item.getParent() != null) 
				item = item.getParent(); 
			
			if (item != null) { 
				View view = item.getBindView(); 
				if (view != null && view instanceof ToolBar.CheckedButton) { 
					toolbar.setChildChecked((ToolBar.CheckedButton)view); 
					return; 
				}
			}
		}
		
		toolbar.clearChildChecked(); 
	}
	
	public void showGroupMenu(NavigationGroup group, View clickView) { 
		if (clickView == null) return; 
		
		PopupMenu menu = mGroupMenu; 
		if (menu != null && !menu.isShowing()) {
			menu.showAtLeft(getContentView(), clickView); 
		}
	}
	
	public void hideGroupMenu() { 
		PopupMenu menu = mGroupMenu; 
		if (menu != null) { 
			menu.dismiss();
		}
	}
	
	public boolean isGroupMenuShowing() { 
		PopupMenu menu = mGroupMenu; 
		if (menu != null) { 
			return menu.isShowing(); 
		}
		return false; 
	}
	
	public PopupMenu getGroupMenu() { 
		return mGroupMenu; 
	}
	
	public ToolBar getNavigationBar() { 
		return mNavigationBar; 
	}
	
	protected PopupMenu createGroupMenu() { 
		return null; 
	}
	
	public boolean clearGroupMenuItems(NavigationController controller, PopupMenu menu) { 
		if (menu == null) return false; 
		
		//final View view = menu.getContentView(); 
		//
		//if (view != null && view instanceof ToolBar) { 
		//	ToolBar toolbar = (ToolBar)view; 
		//	toolbar.removeAllViews(); 
		//}
		
		return false;
	}
	
	public View addGroupMenuItem(NavigationController controller, NavigationItem item, PopupMenu menu) { 
		return null; 
	}
	
	public abstract NavigationController getController(); 
	public abstract void initHeaderTitle(NavigationController controller, NavigationItem item); 
	public abstract void initContentView(NavigationController controller, NavigationItem item); 
	
}
