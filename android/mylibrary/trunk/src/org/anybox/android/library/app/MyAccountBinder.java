package org.anybox.android.library.app;

import android.view.LayoutInflater;
import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountBinder;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountProvider;

public class MyAccountBinder extends AnyboxAccountBinder {
	//private static final Logger LOG = Logger.getLogger(MyAccountBinder.class);

	private final MyBinderHelper mHelper = MyBinderHelper.BLUE;
	
	public MyAccountBinder(AnyboxAccountProvider p) { 
		super(p);
	}
	
	@Override
	protected void onBindedItemView(IActivity activity, LayoutInflater inflater, View view) {
		super.onBindedItemView(activity, inflater, view);
		if (mHelper != null) mHelper.onBindRefreshView(activity, view);
	}
	
	@Override
	public int getHomeAsUpIndicatorMenuRes() {
		if (mHelper != null) return mHelper.getHomeAsUpIndicatorMenuRes();
		return super.getHomeAsUpIndicatorMenuRes();
	}
	
	@Override
	public int getHomeAsUpIndicatorBackRes() {
		if (mHelper != null) return mHelper.getHomeAsUpIndicatorBackRes();
		return super.getHomeAsUpIndicatorBackRes();
	}
	
	@Override
	protected int getHeaderMainViewBackgroundRes() {
		if (mHelper != null) return mHelper.getHeaderMainViewBackgroundRes();
		return super.getHeaderMainViewBackgroundRes();
	}
	
	@Override
	protected int getHeaderActionsViewBackgroundRes() {
		if (mHelper != null) return mHelper.getHeaderActionsViewBackgroundRes();
		return super.getHeaderActionsViewBackgroundRes();
	}
	
	@Override
	protected int getAboveActionsViewBackgroundRes() {
		if (mHelper != null) return mHelper.getAboveActionsViewBackgroundRes();
		return super.getAboveActionsViewBackgroundRes();
	}
	
}
