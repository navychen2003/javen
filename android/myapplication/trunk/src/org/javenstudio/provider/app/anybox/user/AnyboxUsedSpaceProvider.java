package org.javenstudio.provider.app.anybox.user;

import java.util.ArrayList;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.space.OverviewUsedItem;
import org.javenstudio.provider.account.space.OverviewUsedProvider;
import org.javenstudio.provider.account.space.OverviewUserItem;
import org.javenstudio.provider.account.space.SpaceBinder;
import org.javenstudio.provider.account.space.SpaceFactory;
import org.javenstudio.provider.account.space.SpaceItem;
import org.javenstudio.provider.account.space.SpaceProvider;
import org.javenstudio.provider.app.anybox.AnyboxAccountSpace;
import org.javenstudio.provider.app.anybox.AnyboxAccount;
import org.javenstudio.provider.app.anybox.AnyboxStorage;

public class AnyboxUsedSpaceProvider extends OverviewUsedProvider {
	//private static final Logger LOG = Logger.getLogger(AnyboxUsedSpaceProvider.class);

	private final AnyboxAccount mUser;;
	private boolean mReloaded = false;
	
	public AnyboxUsedSpaceProvider(AnyboxSpacesProvider group, 
			int nameRes, int iconRes, int indicatorRes) { 
		super(ResourceHelper.getResources().getString(nameRes), 
				iconRes, new AnyboxUsedSpaceFactory());
		mUser = group.getAccountUser();
		setHomeAsUpIndicator(indicatorRes);
	}
	
	public AnyboxAccount getAccountUser() { return mUser; }
	
	static class AnyboxUsedSpaceFactory extends SpaceFactory {
		@Override
		public SpaceBinder createSpaceBinder(SpaceProvider p) { 
			return new AnyboxUsedSpaceBinder((AnyboxUsedSpaceProvider)p);
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
	    		list.add(new OverviewUsedItem(this, getAccountUser().getTotalSpaces()));
	    		
	    		AnyboxAccountSpace spaces = getAccountUser().getSpaceInfo();
	    		AnyboxAccountSpace.UserInfo me = spaces != null ? spaces.getMe() : null;
	    		AnyboxAccountSpace.UserInfo[] users = spaces != null ? spaces.getUsers() : null;
	    		if (me != null) {
	    			list.add(new OverviewUserItem(this, me));
	    		}
	    		if (users != null) {
	    			for (AnyboxAccountSpace.UserInfo user : users) {
	    				if (user == null) continue;
	    				list.add(new OverviewUserItem(this, user));
	    			}
	    		}
	    		
	    		AnyboxStorage.StorageNode[] nodes = getAccountUser().getStorages();
	    		if (nodes != null) {
	    			for (AnyboxStorage.StorageNode node : nodes) {
	    				if (node == null) continue;
	    				list.add(new OverviewUserItem(this, node));
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
