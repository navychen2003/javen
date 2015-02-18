package org.javenstudio.provider.app.anybox.user;

import java.util.ArrayList;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.space.OverviewFreeItem;
import org.javenstudio.provider.account.space.OverviewSpaceItem;
import org.javenstudio.provider.account.space.OverviewSpaceProvider;
import org.javenstudio.provider.account.space.SpaceBinder;
import org.javenstudio.provider.account.space.SpaceFactory;
import org.javenstudio.provider.account.space.SpaceItem;
import org.javenstudio.provider.account.space.SpaceProvider;
import org.javenstudio.provider.app.anybox.AnyboxAccountSpace;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxStorage;

public class AnyboxOverviewSpaceProvider extends OverviewSpaceProvider {
	//private static final Logger LOG = Logger.getLogger(AnyboxOverviewSpaceProvider.class);
	
	private final AnyboxAccount mUser;;
	private boolean mReloaded = false;
	
	public AnyboxOverviewSpaceProvider(AnyboxSpacesProvider group, 
			int nameRes, int iconRes, int indicatorRes) { 
		super(ResourceHelper.getResources().getString(nameRes), 
				iconRes, new AnyboxOverviewSpaceFactory());
		mUser = group.getAccountUser();
		setHomeAsUpIndicator(indicatorRes);
	}
	
	public AnyboxAccount getAccountUser() { return mUser; }
	
	static class AnyboxOverviewSpaceFactory extends SpaceFactory {
		@Override
		public SpaceBinder createSpaceBinder(SpaceProvider p) { 
			return new AnyboxOverviewSpaceBinder((AnyboxOverviewSpaceProvider)p);
		}
	}
	
	@Override
	public synchronized void reloadOnThread(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		if (mReloaded == false || type == ReloadType.FORCE) {
	    	if (type == ReloadType.FORCE || getSpaceDataSets().getCount() <= 0) {
	    		if (type == ReloadType.FORCE || getAccountUser().getSpaceInfo() == null) 
	    			AnyboxAccountSpace.reloadSpaceInfo(getAccountUser(), callback, reloadId);
	    		
	    		ArrayList<SpaceItem> list = new ArrayList<SpaceItem>();
	    		list.add(new OverviewSpaceItem(this, getAccountUser().getTotalSpaces()));
	    		list.add(new OverviewFreeItem(this, getAccountUser()));
	    		
	    		AnyboxStorage.StorageNode[] nodes = getAccountUser().getStorages();
	    		if (nodes != null) {
	    			for (AnyboxStorage.StorageNode node : nodes) {
	    				if (node == null) continue;
	    				list.add(new OverviewFreeItem(this, node));
	    			}
	    		}
	    		
	    		postClearDataSets(callback);
	    		postAddSpaceItem(callback, list.toArray(new SpaceItem[list.size()]));
	    		
	    		//postNotifyChanged(callback);
	    	}
	    	mReloaded = true;
		}
	}
	
}
