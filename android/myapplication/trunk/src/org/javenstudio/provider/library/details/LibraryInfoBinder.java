package org.javenstudio.provider.library.details;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.provider.ProviderBinder;

public class LibraryInfoBinder extends DataBinder 
		implements ProviderBinder, IRefreshView.RefreshListener {

	@Override
	public View bindView(IActivity activity, LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		return null;
	}

	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater,
			Bundle savedInstanceState) {
	}

	@Override
	public void onPullToRefresh(IRefreshView refreshView) {
	}

	@Override
	public void onReleaseToRefresh(IRefreshView refreshView) {
	}

	@Override
	public void onPullReset(IRefreshView refreshView) {
	}

	@Override
	public void onTouchEvent(IRefreshView refreshView, MotionEvent event) {
	}
	
}
