package org.javenstudio.provider;

import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.activity.ProviderListActivity;

public class ProviderManager {
	private static final Logger LOG = Logger.getLogger(ProviderManager.class);

	private static ProviderController sInstance = null; 
	private static final Object sLock = new Object(); 
	
	public static ProviderController getListController() { 
		synchronized (sLock) { 
			if (sInstance == null) {
				sInstance = new ProviderController(
						ResourceHelper.getApplication(), ResourceHelper.getContext()); 
			}
			return sInstance; 
		}
	}
	
	private static Provider sDefaultProvider = null;
	private static final Map<String, Provider> sProviders = 
			new LinkedHashMap<String, Provider>();
	
	public static void clearProviders() {
		synchronized (sProviders) { 
			sProviders.clear();
			sDefaultProvider = null;
		}
	}
	
	public static void addProvider(Provider p) { 
		if (p == null) return;
		
		synchronized (sProviders) { 
			String key = p.getKey();
			if (sProviders.containsKey(key))
				throw new RuntimeException("Provider: " + key + " already exist");
			
			sProviders.put(key, p);
			
			if (sDefaultProvider == null || p.isDefault())
				sDefaultProvider = p;
			
			if (LOG.isDebugEnabled())
				LOG.debug("addProvider: key=" + key + " provider=" + p);
		}
	}
	
	public static Provider[] getProviders() { 
		synchronized (sProviders) { 
			return sProviders.values().toArray(new Provider[sProviders.size()]);
		}
	}
	
	public static Provider getDefaultProvider() { 
		return sDefaultProvider;
	}
	
	private static class NavActionItem extends ActionItem {
		private final Provider mProvider;
		
		public NavActionItem(Provider p) {
			super(p.getName(), p.getNavigationIconRes(), p.getNavigationIcon(), null);
			setTitle(p.getNavigationTitle());
			mProvider = p;
		}
		
		@Override
		public int getBackgroundRes() { 
			return 0; //AppResources.getInstance().getDrawableRes(AppResources.drawable.card_list_selector); 
		}
		
		@Override
		public int getTitleColorStateListRes() { 
			return AppResources.getInstance().getColorStateListRes(AppResources.color.accountmenu_item_title_color); 
		}
		
		@Override
		public int getTitleSizeRes() {
			int sizeRes = AppResources.getInstance().getDimenRes(AppResources.dimen.accountmenu_item_title_size);
			if (sizeRes == 0) sizeRes = R.dimen.accountmenu_item_title_size;
			return sizeRes;
		}
		
		public boolean handleClick(IActivity activity) { 
			if (mProvider != null) return mProvider.handleClick(activity, this);
			return false; 
		}
	}
	
	private static void collapseAllSubItemsOnClick(final ProviderListActivity activity) { 
		ActionItem[] items = activity.getNavigationItems();
		if (items != null && items.length > 0) { 
			for (ActionItem item : items) { 
				collapseSubItemsOnClick(item);
			}
		}
	}
	
	private static void collapseSubItemsOnClick(ActionItem item) { 
		if (item != null) { 
			ActionItem parent = item.getParentItem();
			if (parent != null && item.getSubItems() == null) 
				parent.collapseSubItems();
			else if (item.getSubItems() != null)
				item.collapseSubItems();
		}
	}
	
	private static void initNavigationSubItems(final ProviderListActivity activity, 
			NavActionItem item, ProviderGroup group) { 
		ProviderActionItem[] subItems = group.getMenuItems(activity);
		for (int j=0; subItems != null && j < subItems.length; j++) { 
			final ProviderActionItem subItem = subItems[j]; 
			if (subItem == null) continue;
			
			subItem.setOnClickListener(new ActionItem.OnClickListener() {
					@Override
					public void onActionClick() {
						collapseAllSubItemsOnClick(activity);
						if (subItem.handleNavigationSubClick(activity) == false) {
							activity.setContentProvider(subItem.getProvider());
							activity.showContent();
						}
					}
				});
		}
		item.setSubItems(subItems);
	}
	
	public static NavActionItem[] initNavigationItems(final ProviderListActivity activity) { 
		if (LOG.isDebugEnabled()) LOG.debug("initNavigationItems: activity=" + activity);
		
		final Provider[] providers = ProviderManager.getProviders();
		final NavActionItem[] menuItems;
		
		if (providers != null && providers.length > 0) { 
			menuItems = new NavActionItem[providers.length];
			
			for (int i=0; i < providers.length; i++) { 
				final Provider p = providers[i];
				final NavActionItem item;
				
				if (p instanceof ProviderGroup) {
					final ProviderGroup group = ((ProviderGroup)p);
					item = new NavActionItem(p) { 
							@Override
							public boolean onSubItemExpand(Activity activity) { 
								boolean changed = group.onGroupExpand((ProviderListActivity)activity);
								if (changed) initNavigationSubItems((ProviderListActivity)activity, this, group);
								return changed; 
							}
						};
					
					initNavigationSubItems(activity, item, group);
					
				} else { 
					item = new NavActionItem(p);
					item.setOnClickListener(new ActionItem.OnClickListener() {
							@Override
							public void onActionClick() {
								collapseAllSubItemsOnClick(activity);
								if (item.handleClick(activity) == false) {
									activity.setContentProvider(p);
									activity.showContent();
								}
							}
						});
				}
				
				menuItems[i] = item;
			}
		} else
			menuItems = new NavActionItem[0];
		
		return menuItems;
	}
	
}
