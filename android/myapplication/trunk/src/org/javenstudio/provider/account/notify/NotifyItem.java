package org.javenstudio.provider.account.notify;

import android.view.View;

import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.cocoka.android.ResourceHelper;

public abstract class NotifyItem extends DataBinderItem {

	private final long mIdentity = ResourceHelper.getIdentity();
	private final NotifyProvider mProvider;
	
	public NotifyItem(NotifyProvider p) { 
		if (p == null) throw new NullPointerException();
		mProvider = p;
	}
	
	public NotifyProvider getProvider() { return mProvider; }
	
	public abstract int getViewRes();
	public abstract void bindView(View view);
	
	public void onUpdateViewOnVisible(boolean restartSlide) {}
	
	public String formatBody(String text) {
		if (text != null && text.length() > 0) {
			if (text.length() > 50) text = text.substring(0, 50) + " ...";
		}
		return text;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + mIdentity + "{}";
	}
	
}
