package org.anybox.android.library.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.provider.activity.AccountMenuFragment;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountMenuBinder;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountProvider;

public class MyAccountMenuBinder extends AnyboxAccountMenuBinder {

	private final MyMenuHelper mHelper = MyMenuHelper.BEACH;
	
	public MyAccountMenuBinder(AnyboxAccountProvider p) { 
		super(p);
	}
	
	@Override
	public int getHeaderBottomBackgroundRes() {
		if (mHelper != null) return mHelper.getMenuHeaderBottomBackgroundRes();
		return super.getHeaderBottomBackgroundRes();
	}
	
	@Override
	public View createBehindView(AccountMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (mHelper != null) {
			return mHelper.createMenuBehindView(fragment, 
					inflater, container, savedInstanceState);
		}
		
		return super.createBehindView(fragment, 
				inflater, container, savedInstanceState);
	}
	
	@Override
	public void onViewCreated(AccountMenuFragment fragment, 
			View view, Bundle savedInstanceState) {
		super.onViewCreated(fragment, view, savedInstanceState);
		
		if (mHelper != null) { 
			mHelper.onMenuViewCreated(fragment, view, savedInstanceState);
		}
	}
	
}
