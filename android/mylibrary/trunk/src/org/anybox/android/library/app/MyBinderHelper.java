package org.anybox.android.library.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.anybox.android.library.R;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.common.util.Logger;

public abstract class MyBinderHelper {
	private static final Logger LOG = Logger.getLogger(MyBinderHelper.class);

	public static final MyBinderHelper BLUE = new MyBlueHelper();
	public static final MyBinderHelper YELLOW = new MyYellowHelper();
	
	public static void setBackgroundDefault(IActivity activity) {
		if (activity == null) return;
		activity.setActionBarBackgroundColor(activity.getResources().getColor(R.color.actionbar_background_light));
		activity.setContentBackgroundColor(activity.getResources().getColor(R.color.content_background_light));
		activity.setActivityBackgroundColor(activity.getResources().getColor(R.color.content_background_light));
	}
	
	private static class MyBlueHelper extends MyBinderHelper {
		public boolean showBackgroundView() { return true; }
		
		public int getBackgroundImageRes() { return R.drawable.background_blue; }
		public int getBackgroundColorRes() { return R.color.blue_background_color; }
		public int getActionModeBackgroundRes() { return R.color.blue_actionmode_background_color; }
		public int getActionModeTitleColorRes() { return R.color.blue_actionmode_title_color; }
		public int getHomeAsUpIndicatorMenuRes() { return R.drawable.ic_ab_menu_holo_light; }
		public int getHomeAsUpIndicatorBackRes() { return R.drawable.ic_ab_back_holo_light; }
		
		public int getHeaderViewBackgroundRes() { return R.color.blue_background_color; }
		public int getFooterViewBackgroundRes() { return R.color.blue_footer_background_color; }
		public int getHeaderMainViewBackgroundRes() { return R.color.blue_header_main_background_color; }
		public int getHeaderPosterViewBackgroundRes() { return R.drawable.header_poster_blue_background; }
		public int getHeaderActionsViewBackgroundRes() { return R.color.blue_header_main_background_color; }
		public int getAboveActionsViewBackgroundRes() { return R.color.blue_header_main_background_color; }
		public int getCardBackgroundRes() { return R.color.blue_card_background_color; }
		
		public int getItemViewBackgroundRes(boolean selected) { return selected ? R.drawable.section_list_blue_selected : R.drawable.section_list_blue_selector; }
		public int getItemHeaderViewBackgroundRes() { return R.color.blue_item_background_color; }
		public int getItemBodyViewBackgroundRes() { return R.color.blue_item_background_color; }
		public int getItemPosterViewBackgroundRes() { return R.drawable.section_poster_blue_background; }
		
		public MyRefreshHelper getRefreshHelper() { return MyRefreshHelper.GOAT; }
	}
	
	private static class MyYellowHelper extends MyBinderHelper {
		public boolean showBackgroundView() { return true; }
		
		public int getBackgroundImageRes() { return R.drawable.background_yellow; }
		public int getBackgroundColorRes() { return R.color.yellow_background_color; }
		public int getActionModeBackgroundRes() { return R.color.yellow_actionmode_background_color; }
		public int getActionModeTitleColorRes() { return R.color.yellow_actionmode_title_color; }
		public int getHomeAsUpIndicatorMenuRes() { return R.drawable.ic_ab_menu_holo_dark; }
		public int getHomeAsUpIndicatorBackRes() { return R.drawable.ic_ab_back_holo_dark; }
		
		public int getHeaderViewBackgroundRes() { return R.color.yellow_background_color; }
		public int getFooterViewBackgroundRes() { return R.color.yellow_footer_background_color; }
		public int getHeaderMainViewBackgroundRes() { return R.color.yellow_header_main_background_color; }
		public int getHeaderPosterViewBackgroundRes() { return R.drawable.header_poster_yellow_background; }
		public int getHeaderActionsViewBackgroundRes() { return R.color.yellow_header_main_background_color; }
		public int getAboveActionsViewBackgroundRes() { return R.color.yellow_header_main_background_color; }
		public int getCardBackgroundRes() { return R.color.yellow_card_background_color; }
		
		public int getItemViewBackgroundRes(boolean selected) { return selected ? R.drawable.section_list_yellow_selected : R.drawable.section_list_yellow_selector; }
		public int getItemHeaderViewBackgroundRes() { return R.color.yellow_item_background_color; }
		public int getItemBodyViewBackgroundRes() { return R.color.yellow_item_background_color; }
		public int getItemPosterViewBackgroundRes() { return R.drawable.section_poster_yellow_background; }
		
		public MyRefreshHelper getRefreshHelper() { return MyRefreshHelper.GOAT; }
	}
	
	public abstract boolean showBackgroundView();
	
	public abstract int getBackgroundImageRes();
	public abstract int getBackgroundColorRes();
	public abstract int getActionModeBackgroundRes();
	public abstract int getActionModeTitleColorRes();
	public abstract int getHomeAsUpIndicatorMenuRes();
	public abstract int getHomeAsUpIndicatorBackRes();
	
	public abstract int getHeaderViewBackgroundRes();
	public abstract int getFooterViewBackgroundRes();
	public abstract int getHeaderMainViewBackgroundRes();
	public abstract int getHeaderPosterViewBackgroundRes();
	public abstract int getHeaderActionsViewBackgroundRes();
	public abstract int getAboveActionsViewBackgroundRes();
	public abstract int getCardBackgroundRes();
	
	public abstract int getItemViewBackgroundRes(boolean selected);
	public abstract int getItemHeaderViewBackgroundRes();
	public abstract int getItemBodyViewBackgroundRes();
	public abstract int getItemPosterViewBackgroundRes();
	
	public abstract MyRefreshHelper getRefreshHelper();
	
	public void onBindRefreshView(IActivity activity, View view) { 
		if (activity != null) {
			IRefreshView refreshView = activity.getActivityHelper().getRefreshView();
			if (LOG.isDebugEnabled()) 
				LOG.debug("bindListView: activity=" + activity + " refreshView=" + refreshView);
			
			MyRefreshHelper helper = getRefreshHelper();
			if (refreshView != null && helper != null) 
				helper.initRefreshView(refreshView);
		}
	}
	
	public void onBindBackgroundView(IActivity activity) {
		if (activity == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("onBindBackgroundView: activity=" + activity);
		
		int bgcolorRes = getBackgroundColorRes();
		if (bgcolorRes != 0) {
			int bgcolor = activity.getResources().getColor(bgcolorRes);
			activity.setContentBackgroundColor(bgcolor);
			activity.setActivityBackgroundColor(bgcolor);
			activity.setActionBarBackgroundColor(bgcolor);
		}
	}
	
	public void onBindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
		if (activity == null || inflater == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onBindBehindAbove: activity=" + activity);
		
		final View behindView = activity.getContentBehindView();
		if (behindView != null && behindView instanceof ViewGroup) {
			ViewGroup behindGroup = (ViewGroup)behindView;
			
			if (showBackgroundView()) {
				onBindBackgroundView(activity);
				
				View view = getBackgroundView(activity, inflater);
				if (view != null) {
					behindGroup.removeAllViews();
					behindGroup.addView(view, new ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				}
				
				behindView.setVisibility(View.VISIBLE);
			}
		}
	}
	
	protected View getBackgroundView(IActivity activity, LayoutInflater inflater) {
		if (activity == null || inflater == null) return null;
		View view = inflater.inflate(R.layout.content_background, null);
		ImageView imageView = (ImageView)view.findViewById(R.id.content_background_image);
		if (imageView != null) {
			int imageRes = getBackgroundImageRes();
			if (imageRes != 0) imageView.setImageResource(imageRes);
		}
		return view;
	}
	
}
