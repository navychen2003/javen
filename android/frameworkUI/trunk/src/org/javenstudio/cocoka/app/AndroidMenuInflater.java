package org.javenstudio.cocoka.app;

import android.view.MenuInflater;

final class AndroidMenuInflater implements IMenuInflater {

	protected final MenuInflater mInflater;
	
	public AndroidMenuInflater(MenuInflater inflater) { 
		mInflater = inflater;
	}

	@Override
	public void inflate(int menuRes, IMenu menu) {
		mInflater.inflate(menuRes, ((AndroidMenu)menu).mMenu);
	}
	
}
