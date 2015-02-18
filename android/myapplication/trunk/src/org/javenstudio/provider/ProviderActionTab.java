package org.javenstudio.provider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.common.util.Logger;

public abstract class ProviderActionTab extends ActionItem {
	private static final Logger LOG = Logger.getLogger(ProviderActionTab.class);

	public static interface TabItem {
		public View getListView();
	}
	
	private final TabItem mItem;
	
	private boolean mDefault = false;
	private boolean mSelected = false;
	
	private ProviderActionTab[] mActions = null;
	//private View mView = null;
	
	public ProviderActionTab(TabItem item, String name) { 
		this(item, name, 0);
	}
	
	public ProviderActionTab(TabItem item, String name, int iconRes) { 
		super(name, iconRes);
		mItem = item;
	}

	public TabItem getTabItem() { return mItem; }
	
	public boolean isSelected() { return mSelected; }
	public void setSelected(boolean selected) { mSelected = selected; }
	
	public boolean isDefault() { return mDefault; }
	public void setDefault(boolean def) { mDefault = def; }
	
	//public void setBindedView(View view) { mView = view; }
	public View[] getBindedViews() { return null; }
	
	public void setActionTabs(ProviderActionTab[] actions) { mActions = actions; }
	public ProviderActionTab[] getActionTabs() { return mActions; }
	
	public final void actionClick(IActivity activity) { 
		actionClick(activity, true);
	}
	
	public final void actionClick(IActivity activity, boolean refresh) { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("actionClick: activity=" + activity 
					+ " action=" + this + " refresh=" + refresh);
		}
		
		ProviderActionTab[] actions = getActionTabs();
		
		for (int i=0; actions != null && i < actions.length; i++) { 
			ProviderActionTab action = actions[i];
			if (action == null) continue;
			
			View[] actionViews = action.getBindedViews();
			if (actionViews == null || actionViews.length == 0) { 
				action.setSelected(false);
				continue; 
			}
			
			if (action == this) { 
				bindActionViews(actionViews, true);
				action.setSelected(true);
				
			} else { 
				bindActionViews(actionViews, false);
				action.setSelected(false);
			}
		}
		
		boolean result = onActionClick(activity);
		
		if (refresh) { 
			if (!result) activity.setContentFragment();
			activity.refreshContent(false);
		}
	}
	
	public final boolean onActionClick(IActivity activity) { 
		IMenuOperation mo = getMenuOperation(activity);
		if (mo != null) { 
			mo.onUpdateContent(activity.getActivity(), 
					activity.getActivityMenu());
		}
		
		boolean result = bindListView(activity);
		if (LOG.isDebugEnabled())
			LOG.debug("onActionClick: action=" + this + " result=" + result);
		
		return result;
	}
	
	public IMenuOperation getMenuOperation(IActivity activity) { 
		return activity.getMenuOperation();
	}
	
	public void bindActionViews(View[] actionViews, boolean selected) {
	}
	
	public View inflateView(IActivity activity, LayoutInflater inflater, 
			ViewGroup container) {
		return null;
	}
	
	public View findListView(IActivity activity, View rootView) { 
		return null;
	}
	
	public boolean bindListView(IActivity activity) {
		return bindListViewDefault(getTabItem().getListView(), getAdapter(activity));
	}
	
	public final boolean bindListViewDefault(View view, ListAdapter adapter) { 
		if (view == null) return false;
		if (LOG.isDebugEnabled())
			LOG.debug("bindListViewDefault: listView=" + view + " adapter=" + adapter);
		
		if (view instanceof RefreshListView) {
			RefreshListView listView = (RefreshListView)view;
			listView.setAdapter(adapter);
			return true;
			
		} else if (view instanceof ListView) { 
			ListView listView = (ListView)view;
			listView.setAdapter(adapter);
			return true;
		}
		
		return false;
	}
	
	public ListAdapter getAdapter(IActivity activity) { 
		return null;
	}
	
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {
	}
	
	public CharSequence getLastUpdatedLabel(IActivity activity) {
		return null;
	}
	
}
