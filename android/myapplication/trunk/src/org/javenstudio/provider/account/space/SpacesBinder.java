package org.javenstudio.provider.account.space;

import android.support.v4.view.ViewPager;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderListBinder;

public class SpacesBinder extends ProviderListBinder {
	private static final Logger LOG = Logger.getLogger(SpacesBinder.class);

	private final SpacesProvider mProvider;
	
	public SpacesBinder(SpacesProvider provider) {
		if (provider == null) throw new NullPointerException();
		mProvider = provider;
	}
	
	@Override
	public SpacesProvider getProvider() {
		return mProvider;
	}

	@Override
	protected ViewPager.OnPageChangeListener getPageChangeListener(final IActivity activity) {
		return new ViewPager.OnPageChangeListener() {
				@Override
				public void onPageSelected(int position) {
					if (activity.getActionHelper().getSelectedItem() != position) 
						activity.getActionHelper().setSelectedItem(position);
				}
				@Override
				public void onPageScrolled(int position, float positionOffset, 
						int positionOffsetPixels) {
					//if (LOG.isDebugEnabled()) {
					//	LOG.debug("onPageScrolled: position=" + position 
					//			+ " positionOffset=" + positionOffset 
					//			+ " positionOffsetPixels=" + positionOffsetPixels);
					//}
				}
				@Override
				public void onPageScrollStateChanged(int state) {
					if (LOG.isDebugEnabled())
						LOG.debug("onPageScrollStateChanged: state=" + state);
				}
			};
	}
	
}
