package org.javenstudio.provider.library.details;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.TouchHelper;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.cocoka.app.RefreshGridView;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBinder;

public abstract class SectionInfoBinder extends DataBinder 
		implements ProviderBinder, IRefreshView.RefreshListener {
	private static final Logger LOG = Logger.getLogger(SectionInfoBinder.class);

	private View mHeaderView = null;
	//private View mFooterView = null;
	//private View mCenterView = null;
	
	public abstract SectionInfoProvider getProvider();
	
	public boolean showAboveViews() { return true; }
	public void bindBackgroundView(IActivity activity) {}
	
	public int getHomeAsUpIndicatorMenuRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.icon_homeasup_menu_indicator);
	}
	
	public int getHomeAsUpIndicatorBackRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.icon_homeasup_back_indicator);
	}
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
		if (activity == null || inflater == null) return;
		mHeaderView = null;
		//mFooterView = null;
		//mCenterView = null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("bindBehindAbove: activity=" + activity);
		
		final View aboveView = activity.getContentAboveView();
		if (aboveView != null && aboveView instanceof ViewGroup) {
			ViewGroup aboveGroup = (ViewGroup)aboveView;
			
			if (showAboveViews()) {
				View view = getAboveView(activity, inflater);
				if (view != null) {
					aboveGroup.removeAllViews();
					aboveGroup.addView(view, new ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				}
				
				aboveView.setVisibility(View.VISIBLE);
			} else {
				aboveView.setVisibility(View.GONE);
			}
		}
	}
	
	protected View getAboveView(IActivity activity, LayoutInflater inflater) {
		if (activity == null || inflater == null) return null;
		final View view = inflater.inflate(R.layout.sectioninfo_above, null);
		
		ViewGroup centerView = (ViewGroup)view.findViewById(R.id.sectioninfo_above_center);
		if (centerView != null) centerView.setVisibility(View.GONE);
		//mCenterView = centerView;
		
		ViewGroup headerView = (ViewGroup)view.findViewById(R.id.sectioninfo_above_header);
		if (headerView != null) {
			View v = getAboveHeaderView(activity, inflater);
			if (v != null) {
				headerView.removeAllViews();
				headerView.addView(v, new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				headerView.setVisibility(View.VISIBLE);
			} else {
				headerView.setVisibility(View.GONE);
			}
			showHeaderView(false);
		}
		mHeaderView = headerView;
		
		ViewGroup footerView = (ViewGroup)view.findViewById(R.id.sectioninfo_above_footer);
		if (footerView != null) footerView.setVisibility(View.GONE);
		//mFooterView = footerView;
		
		return view;
	}
	
	protected View getAboveHeaderView(IActivity activity, LayoutInflater inflater) {
		if (activity == null || inflater == null) return null;
		final View view = inflater.inflate(R.layout.sectioninfo_above_actiontabs, null);
		
		final SectionInfoItem item = getProvider().getSectionItem();
		final SectionActionTab[] actions = item.getActionItems(activity);
		
		SectionActionTab selectTab = initActionTabs(activity, inflater, item, actions, view, 
				R.id.sectioninfo_above_actions, R.layout.sectioninfo_action, 
				R.layout.sectioninfo_action_divider, 
				getAboveActionsViewBackgroundRes());
		
		if (selectTab != null) 
			selectTab.actionClick(activity, false);
		
		return view;
	}
	
	protected void showHeaderView(boolean show) {
		View view = mHeaderView;
		showHeaderView(view, show);
	}
	
	protected void showHeaderView(View view, boolean show) {
		if (view != null) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("showHeaderView: view=" + view + " show=" + show
						+ " visibility=" + view.getVisibility());
			}
			view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
		}
	}
	
	@Override
	public final View bindView(IActivity activity, LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		final View headerView = inflater.inflate(R.layout.sectioninfo_header, null);
		final View mainView = headerView.findViewById(R.id.sectioninfo_header_main);
		if (mainView != null) {
			int backgroundRes = getHeaderMainViewBackgroundRes();
			if (backgroundRes != 0) mainView.setBackgroundResource(backgroundRes);
		}
		
		final SectionInfoItem item = getProvider().getSectionItem();
		final SectionActionTab[] actions = item.getActionItems(activity);
		
		item.bindHeaderView(headerView);
		
		final View actionsView = inflater.inflate(R.layout.sectioninfo_actiontabs, null);
		SectionActionTab selectTab = initActionTabs(activity, inflater, item, actions, actionsView);
		
		View view = null, listView = null;
		if (selectTab != null) { 
			view = selectTab.inflateView(activity, inflater, container);
			listView = selectTab.findListView(activity, view);
		}
		
		if (view == null || listView == null) { 
			view = inflateView(activity, inflater, container);
			listView = findListView(activity, view);
		}
		
		item.setBindView(activity.getActivity(), view);
		bindItemView(activity, inflater, item, listView, headerView, actionsView);
		onBindedItemView(activity, inflater, view);
		
		if (selectTab != null) 
			selectTab.actionClick(activity, false);
		
		requestDownload(activity, item);
		
		return view;
	}

	protected View inflateView(IActivity activity, LayoutInflater inflater, 
			ViewGroup container) { 
		return inflater.inflate(R.layout.sectioninfo, container, false);
	}
	
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.sectioninfo_listview);
	}
	
	public void onPullToRefresh(IRefreshView refreshView) {}
	public void onReleaseToRefresh(IRefreshView refreshView) {}
	public void onPullReset(IRefreshView refreshView) {}
	public void onTouchEvent(IRefreshView refreshView, MotionEvent event) {}
	
	protected int getHeaderMainViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.sectioninfo_header_main_background);
	}
	
	protected int getHeaderPosterViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.sectioninfo_header_poster_background);
	}
	
	protected int getHeaderActionsViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.sectioninfo_header_actions_background);
	}
	
	protected int getAboveActionsViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.sectioninfo_above_actions_background);
	}
	
	protected SectionActionTab initActionTabs(final IActivity activity, 
			LayoutInflater inflater, SectionInfoItem item, SectionActionTab[] actions, View actionsView) { 
		return initActionTabs(activity, inflater, item, actions, actionsView, 
				R.id.sectioninfo_header_actions, R.layout.sectioninfo_action, 
				R.layout.sectioninfo_action_divider, 
				getHeaderActionsViewBackgroundRes());
	}
	
	protected final SectionActionTab initActionTabs(final IActivity activity, 
			LayoutInflater inflater, SectionInfoItem item, SectionActionTab[] actions, View actionsView, 
			int actionRootId, int actionViewRes, int dividerViewRes, int backgroundRes) { 
		if (activity == null || inflater == null || item == null || actionsView == null) 
			return null;
		
		//final SectionActionTab[] actions = item.getActionItems(activity);
		final ViewGroup actionRoot = (ViewGroup)actionsView.findViewById(actionRootId);
		if (backgroundRes != 0) actionRoot.setBackgroundResource(backgroundRes);
		
		SectionActionTab defaultAction = null;
		//TextView defaultView = null;
		
		SectionActionTab selectAction = null;
		//TextView selectView = null;
		
		for (int i=0; actions != null && i < actions.length; i++) { 
			final SectionActionTab action = actions[i];
			if (action == null) continue;
			
			final TextView actionView = (TextView)inflater.inflate(actionViewRes, actionRoot, false);
			if (actionView != null) actionView.setText(action.getTitle());
			
			if (action.isSelected()) {
				selectAction = action;
				//selectView = actionView;
			}
			
			if (defaultAction == null || action.isDefault()) {
				defaultAction = action;
				//defaultView = actionView;
			}
			
			if (actionRoot.getChildCount() > 0) { 
				View divider = inflater.inflate(dividerViewRes, actionRoot, false);
				actionRoot.addView(divider);
			}
			
			if (actionView != null) { 
				actionView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							action.actionClick(activity);
						}
					});
			}
			
			action.setActionTabs(actions);
			action.addActionView(actionView);
			action.setSelected(false);
			
			actionRoot.addView(actionView);
		}
		
		if (selectAction == null) {
			selectAction = defaultAction;
			//selectView = defaultView;
		}
		
		return selectAction;
	}
	
	private final void bindItemView(final IActivity activity, LayoutInflater inflater, 
			SectionInfoItem item, final View listView, final View headerView, final View actionsView) { 
		if (activity == null || inflater == null || item == null || listView == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		int imageHeight = (int)activity.getResources().getDimension(R.dimen.user_image_height);
		int imageWidth = screenWidth;
		
		item.setImageViewWidth(imageWidth);
		item.setImageViewHeight(imageHeight);
		
		item.setHeaderView(activity.getActivity(), headerView);
		item.setActionView(activity.getActivity(), actionsView);
		item.setListView(activity.getActivity(), listView);
		
		AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
	    		private int mFirstVisibleItem = -1;
	    		
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					TouchHelper.onScrollStateChanged(activity, view, scrollState);
				}
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					if (firstVisibleItem != mFirstVisibleItem) { 
						mFirstVisibleItem = firstVisibleItem;
						onFirstVisibleChanged(activity, view, firstVisibleItem, 
								visibleItemCount, totalItemCount);
					}
				}
	        };
		
		if (listView instanceof RefreshListView) {
			RefreshListView list = (RefreshListView)listView;
			list.addHeaderView(headerView);
			list.addHeaderView(actionsView);
			list.setOnScrollListener(scrollListener);
			list.disableOverscrollGlowEdge();
			activity.getActivityHelper().setRefreshView(list);
			
			list.setOnRefreshListener(activity.getActivityHelper()
					.createListRefreshListener(activity, this));
			list.setOnPullEventListener(activity.getActivityHelper()
					.createListPullListener(activity, this));
			
		} else if (listView instanceof ListView) { 
			ListView list = (ListView)listView;
			list.addHeaderView(headerView);
			list.addHeaderView(actionsView);
			list.setOnScrollListener(scrollListener);
			RefreshListView.disableOverscrollGlowEdge(list);
			
		} else if (listView instanceof RefreshGridView) { 
			RefreshGridView list = (RefreshGridView)listView;
			//list.addHeaderView(headerView);
			//list.addHeaderView(actionsView);
			list.setOnScrollListener(scrollListener);
			list.disableOverscrollGlowEdge();
			activity.getActivityHelper().setRefreshView(list);
			
			list.setOnRefreshListener(activity.getActivityHelper()
					.createGridRefreshListener(activity, this));
			list.setOnPullEventListener(activity.getActivityHelper()
					.createGridPullListener(activity, this));
			
		} else if (listView instanceof GridView) { 
			//GridView list = (GridView)listView;
			//list.addHeaderView(headerView);
			//list.addHeaderView(actionsView);
			//list.setOnScrollListener(scrollListener);
			//RefreshListView.disableOverscrollGlowEdge(list);
		}
		
		onBindView(activity, item, headerView);
	}
	
	protected void onBindedItemView(IActivity activity, LayoutInflater inflater, View view) {
	}
	
	protected void onFirstVisibleChanged(IActivity activity, AbsListView view, 
			int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("onFirstVisibleChanged: view=" + view 
					+ " firstVisibleItem=" + firstVisibleItem
					+ " visibleItemCount=" + visibleItemCount 
					+ " totalItemCount=" + totalItemCount);
		}
		showHeaderView(firstVisibleItem > 1);
	}
	
	protected void onBindView(final IActivity activity, SectionInfoItem item, View view) { 
		if (activity == null || item == null || view == null) return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		final int minHeight = (int)activity.getResources().getDimension(R.dimen.dashboardsection_image_minheight);
		
		int imageWidth = screenWidth;
		int columns = 1; //getColumnSize(activity);
		if (columns > 1) imageWidth = imageWidth / columns;
		int imageHeight = imageWidth / 2;
		
		int imgWidth = item.getImageWidth();
		int imgHeight = item.getImageHeight();
		
		if (imgWidth > 0 && imgHeight > 0) {
			float rate = (float)imgWidth / (float)imgHeight;
			if (imgWidth >= imageWidth - 10 && rate < 1.1f) {
				//if (rate < 0.8f)
				//	imageHeight = imageWidth;
				//else
					imageHeight = (int)((float)imageWidth * 0.8f);
			} else {
				//imageHeight = imageWidth / 2;
				imageHeight = (int)((float)imageWidth * 0.8f);
			}
		} else {
			imageHeight = (int)((float)imageWidth * 0.8f);
		}
		
		if (imageHeight < minHeight)
			imageHeight = minHeight;
		
		item.setImageViewWidth(imageWidth);
		item.setImageViewHeight(imageHeight);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onBindView: imageWidth=" + imgWidth + " imageHeight=" + imgHeight 
					+ " viewWidth=" + imageWidth + " viewHeight=" + imageHeight);
		}
		
		final View topView = view.findViewById(R.id.sectioninfo_header_top);
		if (topView != null) {
			int backgroundRes = getHeaderPosterViewBackgroundRes();
			if (backgroundRes != 0) topView.setBackgroundResource(backgroundRes);
		}
		
		final ImageView bigiconView = (ImageView)view.findViewById(R.id.sectioninfo_header_bigicon);
		if (bigiconView != null) { 
			Drawable icon = AppResources.getInstance().getSectionBigIcon(item.getSectionData());
			if (icon != null) bigiconView.setImageDrawable(icon);
		}
		
		TextView nameView = (TextView)view.findViewById(R.id.sectioninfo_header_name_title);
		if (nameView != null) {
			int colorRes = AppResources.getInstance().getColorRes(AppResources.color.sectioninfo_header_name_color);
			if (colorRes != 0) nameView.setTextColor(AppResources.getInstance().getResources().getColor(colorRes));
			nameView.setText(item.getSectionTitle());
		}
		
		ImageView iconView = (ImageView)view.findViewById(R.id.sectioninfo_header_icon_image);
		if (iconView != null) {
			Drawable icon = item.getSectionIcon();
			if (icon != null) iconView.setImageDrawable(icon);
		}
		
		final TextView imageText = (TextView)view.findViewById(R.id.sectioninfo_header_image_text);
		if (imageText != null) {
			int colorRes = AppResources.getInstance().getColorRes(AppResources.color.sectioninfo_header_image_text_color);
			if (colorRes != 0) imageText.setTextColor(activity.getResources().getColorStateList(colorRes));
			int textRes = AppResources.getInstance().getStringRes(AppResources.string.sectioninfo_download_image_label);
			if (textRes == 0) textRes = R.string.download_image_label;
			imageText.setText(textRes);
		}
		
		onUpdateView(item, view, false);
	}
	
	protected void onUpdateView(final SectionInfoItem item, View view, boolean restartSlide) {
		if (item == null || view == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("onUpdateView: item=" + item + " view=" + view 
					+ " restartSlide=" + restartSlide);
		}
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.sectioninfo_header_image);
		if (imageView != null) { 
			Drawable fd = getImageDrawable(item);
			if (fd != null) { 
				item.onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				item.onImageDrawableBinded(fd, restartSlide);
			}
			imageView.setVisibility(View.VISIBLE);
		}
		
		final ImageView overlayView = (ImageView)view.findViewById(R.id.sectioninfo_header_overlay);
		if (overlayView != null) { 
			int overlayRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.sectioninfo_overlay);
			if (overlayRes != 0) overlayView.setImageResource(overlayRes);
			
			overlayView.setVisibility(item.supportChangePoster() ? View.VISIBLE : View.GONE);
			overlayView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						//startAnimation(overlayView);
					}
				});
		}
		
		final TextView imageText = (TextView)view.findViewById(R.id.sectioninfo_header_image_text);
		if (imageText != null) {
			if (item.isFetching() || item.isImageDownloaded() || imageView == null) {
				imageText.setVisibility(View.GONE);
				if (imageView != null) imageView.setOnClickListener(null);
			} else {
				imageText.setVisibility(View.VISIBLE);
				imageView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							requestDownload(null, item, true);
						}
					});
			}
		}
		
		final View progressView = view.findViewById(R.id.sectioninfo_header_progressbar);
		if (progressView != null) {
			progressView.setVisibility(item.isFetching() ? View.VISIBLE : View.GONE);
		}
	}
	
	protected void requestDownload(IActivity activity, SectionInfoItem item) {
		requestDownload(activity, item, false);
	}
	
	protected void requestDownload(IActivity activity, SectionInfoItem item, boolean nocheck) { 
		Image posterImage = item.getPosterImage();
		if (posterImage != null && posterImage instanceof HttpImage) 
			requestDownload(activity, (HttpImage)posterImage, nocheck);
	}
	
	private Drawable getImageDrawable(final SectionInfoItem item) { 
		Drawable d = item.getCachedImageDrawable();
		if (d != null) return d;
		
		Drawable fd = item.getImageDrawable(
				item.getImageViewWidth(), item.getImageViewHeight());
		
		return fd;
	}
	
}
