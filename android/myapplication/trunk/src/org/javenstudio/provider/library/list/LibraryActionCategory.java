package org.javenstudio.provider.library.list;

import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderActionItem;

public class LibraryActionCategory extends ProviderActionItem {

	private final CharSequence mCategory;
	
	public LibraryActionCategory(Provider item, CharSequence title) {
		super(item.getName(), item.getIconRes(), item.getIcon(), null, item);
		mCategory = title;
	}
	
	@Override
	public boolean isEnabled() { return false; }
	
	@Override
	public CharSequence getTitle() {
		return mCategory;
	}
	
	@Override
	public CharSequence getSubTitle() {
		return null;
	}
	
	@Override
	public CharSequence getDropdownTitle() {
		return mCategory;
	}
	
	@Override
	public CharSequence getDropdownText() { 
		return mCategory;
	}
	
}
