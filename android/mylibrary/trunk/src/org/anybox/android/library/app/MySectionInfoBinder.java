package org.anybox.android.library.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionInfoBinder;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionInfoFactory;
import org.javenstudio.provider.app.anybox.library.AnyboxSectionInfoProvider;

public class MySectionInfoBinder extends AnyboxSectionInfoBinder {

	private static final MyBinderHelper mHelper = MyBinderHelper.YELLOW;
	
	public MySectionInfoBinder(AnyboxSectionInfoProvider provider) {
		super(provider);
	}
	
	@Override
	public void bindBackgroundView(IActivity activity) {
		super.bindBackgroundView(activity);
		if (mHelper != null) mHelper.onBindBackgroundView(activity);
	}
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
		super.bindBehindAbove(activity, inflater, savedInstanceState);
		if (mHelper != null) mHelper.onBindBehindAbove(activity, inflater, savedInstanceState);
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
	protected int getHeaderPosterViewBackgroundRes() {
		if (mHelper != null) return mHelper.getHeaderPosterViewBackgroundRes();
		return super.getHeaderPosterViewBackgroundRes();
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
	
	static AnyboxSectionInfoFactory FACTORY = new AnyboxSectionInfoFactory() {
			public int getMaxTags() { return 10; }
			public int getDetailsCardBackgroundRes() { return mHelper.getCardBackgroundRes(); }
			public int getDetailsCardItemBackgroundRes() { return 0; }
		};
	
}
