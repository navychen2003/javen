package org.javenstudio.provider;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderListener;

public class ProviderGroup extends Provider {

	private final List<Provider> mProviders = new ArrayList<Provider>();
	
	public ProviderGroup(String name, int iconRes) { 
		super(name, iconRes);
	}

	public void addProvider(Provider provider) { 
		if (provider == null) return;
		
		synchronized (mProviders) {
			for (Provider p : mProviders) { 
				if (p == provider) return;
			}
			
			mProviders.add(provider);
		}
	}
	
	public Provider[] getProviders() { 
		synchronized (mProviders) {
			return mProviders.toArray(new Provider[mProviders.size()]);
		}
	}
	
	public void removeProviders() { 
		synchronized (mProviders) {
			mProviders.clear();
		}
	}
	
	@Override
	public void setBindListener(DataBinderListener l) { 
		super.setBindListener(l); 
		
		synchronized (mProviders) {
			for (Provider p : mProviders) { 
				p.setBindListener(l);
			}
		}
	}
	
	public ProviderActionItem[] getMenuItems(final IActivity activity) { 
		synchronized (mProviders) {
			if (mProviders.size() <= 0) return null;
			
			final ProviderActionItem[] items = new ProviderActionItem[mProviders.size()];
			
			for (int i=0; i < items.length; i++) { 
				final Provider item = mProviders.get(i);
				final int iconRes = item.getIconRes();
				final Drawable icon = item.getIcon();
				
				items[i] = new ProviderActionItem(item.getName(), 
					iconRes, icon, null, item);
				
			}
			
			return items; 
		}
	}

	@Override
	public ProviderBinder getBinder() {
		return null;
	}
	
	public boolean onGroupExpand(Activity activity) { 
		return false; 
	}
	
}
