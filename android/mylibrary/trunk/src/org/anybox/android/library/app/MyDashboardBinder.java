package org.anybox.android.library.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.app.anybox.user.AnyboxDashboardBinder;
import org.javenstudio.provider.app.anybox.user.AnyboxDashboardProvider;

public class MyDashboardBinder extends AnyboxDashboardBinder {
	//private static final Logger LOG = Logger.getLogger(MyDashboardBinder.class);

	private final MyBinderHelper mHelper = MyBinderHelper.BLUE;
		
	public MyDashboardBinder(AnyboxDashboardProvider provider) {
		super(provider);
	}
	
	@Override
	public void bindBackgroundView(IActivity activity) {
		super.bindBackgroundView(activity);
		if (mHelper != null) mHelper.onBindBackgroundView(activity);
	}
	
	@Override
	public void bindListView(IActivity activity, View view, ListAdapter adapter) { 
		super.bindListView(activity, view, adapter);
		if (mHelper != null) mHelper.onBindRefreshView(activity, view);
	}
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
		super.bindBehindAbove(activity, inflater, savedInstanceState);
		if (mHelper != null) mHelper.onBindBehindAbove(activity, inflater, savedInstanceState);
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
	protected int getItemHeaderViewBackgroundRes() {
		if (mHelper != null) return mHelper.getItemHeaderViewBackgroundRes();
		return super.getItemHeaderViewBackgroundRes();
	}
	
	@Override
	protected int getItemBodyViewBackgroundRes() {
		if (mHelper != null) return mHelper.getItemBodyViewBackgroundRes();
		return super.getItemBodyViewBackgroundRes();
	}
	
	@Override
	protected int getItemPosterViewBackgroundRes() {
		if (mHelper != null) return mHelper.getItemPosterViewBackgroundRes();
		return super.getItemPosterViewBackgroundRes();
	}
	
}
