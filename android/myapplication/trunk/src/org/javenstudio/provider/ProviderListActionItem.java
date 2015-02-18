package org.javenstudio.provider;

import android.view.View;

import org.javenstudio.common.util.Logger;

public class ProviderListActionItem extends ProviderActionItem {
	private static final Logger LOG = Logger.getLogger(ProviderListActionItem.class);
	
	private final ProviderList mProviderList;
	
	public ProviderListActionItem(ProviderList list, Provider item) {
		super(item.getName(), item.getIconRes(), item.getIcon(), null, item);
		mProviderList = list;
	}
	
	public ProviderList getProviderList() { return mProviderList; }
	
	@Override
	public CharSequence getTitle() {
		return getProvider().getTitle();
	}
	
	@Override
	public CharSequence getSubTitle() {
		return getProvider().getSubTitle();
	}
	
	@Override
	public CharSequence getDropdownText() { 
		CharSequence title = getProvider().getDropdownTitle();
		if (title == null || title.length() == 0) title = getDropdownTitle();
		if (title != null && title.length() > 0) return title;
		return getTitle();
	}
	
	@Override
	public void onTitleBinded(View view, View subview, boolean dropdown) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onTitleBinded: item=" + this + " view=" + view 
					+ " dropdown=" + dropdown);
		}
		getProviderList().onActionTitleBinded(this, view, subview, dropdown);
	}
	
}
