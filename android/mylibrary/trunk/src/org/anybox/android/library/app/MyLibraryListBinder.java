package org.anybox.android.library.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryListBinder;
import org.javenstudio.provider.app.anybox.library.AnyboxLibraryProvider;
import org.javenstudio.provider.library.list.LibraryFactory;
import org.javenstudio.provider.library.section.SectionListItem;

public class MyLibraryListBinder extends AnyboxLibraryListBinder {
	//private static final Logger LOG = Logger.getLogger(MyLibraryListBinder.class);

	private final MyBinderHelper mHelper = MyBinderHelper.YELLOW;
	
	public MyLibraryListBinder(AnyboxLibraryProvider provider, 
			LibraryFactory factory) {
		super(provider, factory);
	}
	
	@Override
	protected int getSectionListItemViewRes(SectionListItem item) {
		if (item != null && item instanceof MyLibraryFactory.FolderEmptyItem) {
			return MyLibraryFactory.FolderEmptyItem.getViewRes();
		}
		return super.getSectionListItemViewRes(item);
	}
	
	@Override
	protected void onBindItemView(IActivity activity, SectionListItem item, View view) { 
		if (activity != null && item != null && item instanceof MyLibraryFactory.FolderEmptyItem) {
			MyLibraryFactory.FolderEmptyItem.bindViews(activity, (MyLibraryFactory.FolderEmptyItem)item, view);
			return;
		}
		super.onBindItemView(activity, item, view);
	}
	
	@Override
	public void updateItemView(SectionListItem item, View view, boolean restartSlide) {
		if (item != null && item instanceof MyLibraryFactory.FolderEmptyItem) {
			MyLibraryFactory.FolderEmptyItem.updateImageView((MyLibraryFactory.FolderEmptyItem)item, view, restartSlide);
			return;
		}
		super.updateItemView(item, view, restartSlide);
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
	public int getHeaderViewBackgroundRes() {
		if (mHelper != null) return mHelper.getHeaderViewBackgroundRes();
		return super.getHeaderViewBackgroundRes();
	}
	
	@Override
	public int getFooterViewBackgroundRes() {
		if (mHelper != null) return mHelper.getFooterViewBackgroundRes();
		return super.getFooterViewBackgroundRes();
	}
	
	@Override
	public int getItemViewBackgroundRes(boolean selected) {
		if (mHelper != null) return mHelper.getItemViewBackgroundRes(selected);
		return super.getItemViewBackgroundRes(selected);
	}
	
	@Override
	public int getItemPosterViewBackgroundRes() {
		if (mHelper != null) return mHelper.getItemPosterViewBackgroundRes();
		return super.getItemPosterViewBackgroundRes();
	}
	
	@Override
	public void onActionModeViewBinded(SelectMode mode, View view) {
		super.onActionModeViewBinded(mode, view);
		if (mHelper != null && mode != null) {
			int backgroundRes = mHelper.getActionModeBackgroundRes();
			IActivity activity = mode.getActionHelper().getIActivity();
			if (backgroundRes != 0 && activity != null) {
				activity.setActionModeBackgroundResource(backgroundRes);
			}
			
			int titlecolorRes = mHelper.getActionModeTitleColorRes();
			if (titlecolorRes != 0) {
				TextView titleView = mode.getTitleView();
				TextView subtitleView = mode.getSubTitleView();
				if (titleView != null) 
					titleView.setTextColor(ResourceHelper.getResources().getColor(titlecolorRes));
				if (subtitleView != null) 
					subtitleView.setTextColor(ResourceHelper.getResources().getColor(titlecolorRes));
			}
		}
	}
	
}
