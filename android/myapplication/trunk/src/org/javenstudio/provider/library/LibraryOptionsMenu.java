package org.javenstudio.provider.library;

import android.app.Activity;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.FilterType;
import org.javenstudio.android.app.FilterTypeOperation;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SortType;
import org.javenstudio.android.app.SortTypeOperation;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuInflater;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.IProviderActivity;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.library.list.LibrariesProvider;
import org.javenstudio.provider.library.list.LibraryProvider;

public abstract class LibraryOptionsMenu implements IOptionsMenu {
	private static final Logger LOG = Logger.getLogger(LibraryOptionsMenu.class);

	private Activity mActivity = null;
	private IMenu mMenu = null;
	private IMenuItem mGridItem = null;
	private IMenuItem mSearchItem = null;
	
	public abstract AccountApp getAccountApp();
	public AccountUser getAccountUser() { return getAccountApp().getAccount(); }
	
	protected void postUpdateOptionsMenu(final Activity activity) {
		if (activity == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					onUpdateOptionsMenu(activity);
				}
			});
	}
	
	@Override
	public boolean hasOptionsMenu(Activity activity) {
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Activity activity, IMenu menu, IMenuInflater inflater) {
		if (LOG.isDebugEnabled()) LOG.debug("onCreateOptionsMenu: activity=" + activity);
		
		inflater.inflate(R.menu.library_menu, menu);
		mGridItem = menu.findItem(R.id.library_action_grid);
		mSearchItem = menu.findItem(R.id.library_action_search);
		//mSortItem = menu.findItem(R.id.library_action_sortby);
		mMenu = menu;
		mActivity = activity;
		
		LibraryProvider provider = getLibraryProvider(activity);
		if (provider != null && activity != null && activity instanceof IProviderActivity) {
			IProviderActivity menuactivity = (IProviderActivity)activity;
			menuactivity.getActivityHelper().initSearchMenuItem(mSearchItem, 
					provider.getSearchViewListener(), true);
		}
		
		return true; 
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Activity activity, IMenu menu) { 
		if (LOG.isDebugEnabled()) LOG.debug("onPrepareOptionsMenu: activity=" + activity);
		onUpdateOptionsMenu(activity);
		return true; 
	}
	
	@Override
    public boolean onOptionsItemSelected(Activity activity, IMenuItem item) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("onPrepareOptionsMenu: activity=" + activity + " item=" + item);
		
		if (item.getItemId() == R.id.library_action_grid) {
			return onGridItemSelected(activity);
		} else if (item.getItemId() == R.id.library_action_filterby) {
			return onFilterbyItemSelected(activity);
		} else if (item.getItemId() == R.id.library_action_sortby) {
			return onSortbyItemSelected(activity);
		}
		
		return false;
	}

	@Override
	public boolean removeOptionsMenu(Activity activity) {
		if (activity == null || activity != mActivity) return false;
		if (LOG.isDebugEnabled()) LOG.debug("removeOptionsMenu: activity=" + activity);
		
		IMenu menu = mMenu;
		mMenu = null;
		mActivity = null;
		
		if (menu != null) { 
			menu.removeItem(R.id.library_action_grid);
			menu.removeItem(R.id.library_action_filterby);
			menu.removeItem(R.id.library_action_sortby);
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onUpdateOptionsMenu(Activity activity) {
		if (LOG.isDebugEnabled()) LOG.debug("onUpdateOptionsMenu: activity=" + activity);
		
		IMenuItem gridItem = mGridItem;
		if (gridItem != null) {
			ViewType.Type type = getSelectViewType(activity);
			if (type == ViewType.Type.GRID) {
				gridItem.setIcon(R.drawable.ic_menu_list_holo_light);
				gridItem.setTitle(R.string.label_action_view_list);
			} else {
				gridItem.setIcon(R.drawable.ic_menu_grid_holo_light);
				gridItem.setTitle(R.string.label_action_view_grid);
			}
			
			return true;
		}
		
		return false;
	}
	
	protected boolean onGridItemSelected(Activity activity) {
		ViewType.Type fromType = getSelectViewType(activity);
		ViewType.Type toType = null;
		if (fromType == ViewType.Type.GRID) toType = ViewType.Type.LIST;
		else toType = ViewType.Type.GRID;
		
		if (toType != null && toType != fromType) 
			onChangeViewType(activity, toType);
		
		return true;
	}
	
	protected boolean onFilterbyItemSelected(final Activity activity) {
		LibraryProvider p = getLibraryProvider(activity);
		if (p != null && activity != null) {
			FilterTypeOperation.showFilterTypeDialog(activity, 
				p.getFactory().getFilterType(), 
				new FilterType.OnChangeListener() {
					@Override
					public void onChangeFilterType(FilterType.Type type) {
						LibraryProvider p = getLibraryProvider(activity);
						if (p != null && activity != null && activity instanceof IProviderActivity) 
							p.refreshContent((IProviderActivity)activity, true);
					}
				});
			return true;
		}
		return false;
	}
	
	protected boolean onSortbyItemSelected(final Activity activity) {
		LibraryProvider p = getLibraryProvider(activity);
		if (p != null && activity != null) {
			SortTypeOperation.showSortTypeDialog(activity, 
				p.getFactory().getSortType(), 
				new SortType.OnChangeListener() {
					@Override
					public void onChangeSortType(SortType.Type type) {
						LibraryProvider p = getLibraryProvider(activity);
						if (p != null && activity != null && activity instanceof IProviderActivity) 
							p.refreshContent((IProviderActivity)activity, true);
					}
				});
			return true;
		}
		return false;
	}
	
	protected ViewType.Type getSelectViewType(Activity activity) {
		LibraryProvider p = getLibraryProvider(activity);
		if (p != null) return p.getFactory().getViewType().getSelectType();
		return null;
	}
	
	protected void onChangeViewType(Activity activity, ViewType.Type to) {
		if (activity == null || to == null) return;
		
		LibraryProvider p = getLibraryProvider(activity);
		if (p != null && activity != null && activity instanceof IProviderActivity) {
			p.getFactory().getViewType().setSelectType(to);
			p.refreshContent((IProviderActivity)activity, false);
		}
	}
	
	protected LibraryProvider getLibraryProvider(Activity activity) {
		if (activity != null && activity instanceof IProviderActivity) {
			IProviderActivity menuactivity = (IProviderActivity)activity;
			Provider provider = menuactivity.getCurrentProvider();
			
			if (provider != null && provider instanceof LibrariesProvider) {
				LibrariesProvider p0 = (LibrariesProvider)provider;
				Provider selectProvider = p0.getSelectProvider();
				
				if (selectProvider != null && selectProvider instanceof LibraryProvider) {
					LibraryProvider p = (LibraryProvider)selectProvider;
					return p;
				}
			}
		}
		return null;
	}
	
}
