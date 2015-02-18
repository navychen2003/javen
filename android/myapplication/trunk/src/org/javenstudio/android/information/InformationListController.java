package org.javenstudio.android.information;

import android.app.Application;
import android.content.Context;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.model.NavigationController;
import org.javenstudio.cocoka.widget.model.NavigationItem;

public final class InformationListController extends NavigationController {

	private static InformationListController sInstance = null; 
	private static final Object sLock = new Object(); 
	
	public static InformationListController getInstance() { 
		synchronized (sLock) { 
			if (sInstance == null) {
				sInstance = new InformationListController(
						ResourceHelper.getApplication(), ResourceHelper.getContext()); 
			}
			return sInstance; 
		}
	}
	
	private final Context mContext; 
	private NavigationItem mDefaultItem = null; 
	private NavigationItem mCurrentItem = null; 
	
	private InformationListController(Application app, Context context) { 
		super(new InformationListModel(app)); 
		mContext = context; 
	}
	
	@Override 
	public Context getContext() { 
		return mContext; 
	}
	
	public InformationListModel getInformationListModel() { 
		return (InformationListModel)getNavigationModel(); 
	}
	
	public NavigationItem[] getGroupItems() { 
		return getNavigationModel().getNavigationItems(); 
	}
	
	@Override 
	protected NavigationItem getDefaultSelectedItem(NavigationItem first) { 
		return getDefaultItem();
	}
	
	public synchronized NavigationItem getDefaultItem() { 
		NavigationItem item = mDefaultItem; 
		if (item == null) { 
			item = getInformationListModel().createDefaultNavigationItem(); 
			mDefaultItem = item; 
		}
		return item;
	}
	
	@Override
	public synchronized NavigationItem getSelectedItem() { 
		NavigationItem item = mCurrentItem; 
		if (item != null) 
			return item;
		
		return super.getSelectedItem();
	}
	
	@Override
	public synchronized NavigationItem getCurrentItem() { 
		return mCurrentItem;
	}
	
	public synchronized void setCurrentItem(NavigationItem item) { 
		mCurrentItem = item;
	}
	
	public void postRefresh() { 
		final InformationNavItem item = (InformationNavItem)getSelectedItem();
		postRefresh(item);
	}
	
	public void postRefresh(InformationNavItem item) { 
		if (item != null) { 
			final InformationDataSets dataSets = item.getInformationDataSets(); 
			ResourceHelper.getHandler().post(new Runnable() { 
					public void run() { 
						if (dataSets != null) 
							dataSets.notifyDataSetChanged(); 
					}
				});
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{current=" + getCurrentItem() + "}";
	}
	
}
