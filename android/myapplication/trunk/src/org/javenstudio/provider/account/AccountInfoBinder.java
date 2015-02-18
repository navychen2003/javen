package org.javenstudio.provider.account;

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

import org.javenstudio.android.account.SystemUser;
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

public abstract class AccountInfoBinder extends DataBinder 
		implements ProviderBinder, IRefreshView.RefreshListener {
	private static final Logger LOG = Logger.getLogger(AccountInfoBinder.class);

	private View mHeaderView = null;
	//private View mFooterView = null;
	//private View mCenterView = null;
	
	public abstract AccountInfoProvider getProvider();
	
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
		final View view = inflater.inflate(R.layout.accountinfo_above, null);
		
		ViewGroup centerView = (ViewGroup)view.findViewById(R.id.accountinfo_above_center);
		if (centerView != null) centerView.setVisibility(View.GONE);
		//mCenterView = centerView;
		
		ViewGroup headerView = (ViewGroup)view.findViewById(R.id.accountinfo_above_header);
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
		}
		mHeaderView = headerView;
		
		ViewGroup footerView = (ViewGroup)view.findViewById(R.id.accountinfo_above_footer);
		if (footerView != null) footerView.setVisibility(View.GONE);
		//mFooterView = footerView;
		
		return view;
	}
	
	protected View getAboveHeaderView(IActivity activity, LayoutInflater inflater) {
		if (activity == null || inflater == null) return null;
		final View view = inflater.inflate(R.layout.accountinfo_above_actiontabs, null);
		
		final AccountInfoItem item = getProvider().getAccountItem();
		final AccountActionTab[] actions = item.getActionItems(activity);
		
		AccountActionTab selectTab = initActionTabs(activity, inflater, item, actions, view, 
				R.id.accountinfo_above_actions, R.layout.accountinfo_action, 
				R.layout.accountinfo_action_divider, 
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
		
		final View headerView = inflater.inflate(R.layout.accountinfo_header, null);
		final View mainView = headerView.findViewById(R.id.accountinfo_header_main);
		if (mainView != null) {
			int backgroundRes = getHeaderMainViewBackgroundRes();
			if (backgroundRes != 0) mainView.setBackgroundResource(backgroundRes);
		}
		
		final AccountInfoItem item = getProvider().getAccountItem();
		final AccountActionTab[] actions = item.getActionItems(activity);
		
		item.bindHeaderView(headerView);
		
		final View actionsView = inflater.inflate(R.layout.accountinfo_actiontabs, null);
		AccountActionTab selectTab = initActionTabs(activity, inflater, item, actions, actionsView);
		
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
		return inflater.inflate(R.layout.accountinfo, container, false);
	}
	
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.accountinfo_listview);
	}
	
	public void onPullToRefresh(IRefreshView refreshView) {}
	public void onReleaseToRefresh(IRefreshView refreshView) {}
	public void onPullReset(IRefreshView refreshView) {}
	public void onTouchEvent(IRefreshView refreshView, MotionEvent event) {}
	
	protected int getHeaderMainViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.accountinfo_header_background);
	}
	
	protected int getHeaderActionsViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.accountinfo_header_actions_background);
	}
	
	protected int getAboveActionsViewBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.accountinfo_above_actions_background);
	}
	
	protected AccountActionTab initActionTabs(final IActivity activity, 
			LayoutInflater inflater, AccountInfoItem item, AccountActionTab[] actions, View actionsView) {
		return initActionTabs(activity, inflater, item, actions, actionsView, 
				R.id.accountinfo_header_actions, R.layout.accountinfo_action, 
				R.layout.accountinfo_action_divider, 
				getHeaderActionsViewBackgroundRes());
	}
	
	protected final AccountActionTab initActionTabs(final IActivity activity, 
			LayoutInflater inflater, AccountInfoItem item, AccountActionTab[] actions, View actionsView, 
			int actionRootId, int actionViewRes, int dividerViewRes, int backgroundRes) { 
		if (activity == null || inflater == null || item == null || actionsView == null) 
			return null;
		
		//final AccountActionTab[] actions = item.getActionItems(activity);
		final ViewGroup actionRoot = (ViewGroup)actionsView.findViewById(actionRootId);
		if (backgroundRes != 0) actionRoot.setBackgroundResource(backgroundRes);
		
		AccountActionTab defaultAction = null;
		//TextView defaultView = null;
		
		AccountActionTab selectAction = null;
		//TextView selectView = null;
		
		for (int i=0; actions != null && i < actions.length; i++) { 
			final AccountActionTab action = actions[i];
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
			AccountInfoItem item, final View listView, final View headerView, final View actionsView) { 
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
		
		onBindView(item, headerView);
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
	
	protected void onBindView(AccountInfoItem item) { 
		if (item != null) onBindView(item, item.getHeaderView());
	}
	
	protected void onBindView(AccountInfoItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView userView = (TextView)view.findViewById(R.id.accountinfo_header_name);
		if (userView != null) {
			int colorRes = AppResources.getInstance().getColorRes(AppResources.color.accountinfo_header_name_color);
			if (colorRes != 0) userView.setTextColor(AppResources.getInstance().getResources().getColor(colorRes));
			userView.setText(item.getUserTitle());
		}
		
		TextView photocountView = (TextView)view.findViewById(R.id.accountinfo_header_photocount);
		if (photocountView != null) {
			photocountView.setText(""+item.getStatisticCount(SystemUser.COUNT_PHOTO));
		}
		
		TextView likecountView = (TextView)view.findViewById(R.id.accountinfo_header_likecount);
		if (likecountView != null) {
			likecountView.setText(""+item.getStatisticCount(SystemUser.COUNT_LIKE));
		}
		
		TextView commentcountView = (TextView)view.findViewById(R.id.accountinfo_header_commentcount);
		if (commentcountView != null) {
			commentcountView.setText(""+item.getStatisticCount(SystemUser.COUNT_COMMENT));
		}
		
		TextView followcountView = (TextView)view.findViewById(R.id.accountinfo_header_followcount);
		if (followcountView != null) {
			followcountView.setText(""+item.getStatisticCount(SystemUser.COUNT_FOLLOW));
		}
		
		onUpdateView(item, false);
	}
	
	protected void onUpdateView(AccountInfoItem item, boolean restartSlide) {
		final View view = item.getBindView();
		if (view == null) return;
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.accountinfo_header_image);
		if (imageView != null) { 
			Drawable fd = getImageDrawable(item);
			if (fd != null) { 
				item.onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				item.onImageDrawableBinded(fd, restartSlide);
			}
			imageView.setVisibility(View.VISIBLE);
		}
		
		final ImageView avatarView = (ImageView)view.findViewById(R.id.accountinfo_header_avatar);
		if (avatarView != null) { 
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.accountinfo_avatar_round_selector);
			if (backgroundRes != 0) avatarView.setBackgroundResource(backgroundRes);
			
			int size = AppResources.getInstance().getResources().getDimensionPixelSize(R.dimen.headerinfo_avatar_size);
			Drawable avatarIcon = item.getAvatarRoundDrawable(size, 0);
			if (avatarIcon != null) 
				avatarView.setImageDrawable(avatarIcon);
			
			avatarView.setVisibility(View.VISIBLE);
			avatarView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						//startAnimation(avatarView);
					}
				});
		}
		
		final ImageView overlayView = (ImageView)view.findViewById(R.id.accountinfo_header_overlay);
		if (overlayView != null) { 
			int overlayRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.accountinfo_overlay);
			if (overlayRes != 0) overlayView.setImageResource(overlayRes);
			
			overlayView.setVisibility(View.VISIBLE);
			overlayView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						//startAnimation(overlayView);
					}
				});
		}
	}
	
	protected void requestDownload(IActivity activity, AccountInfoItem item) { 
		Image avatarImage = item.getAvatarImage();
		if (avatarImage != null && avatarImage instanceof HttpImage) 
			requestDownload((HttpImage)avatarImage);
	}
	
	private Drawable getImageDrawable(final AccountInfoItem item) { 
		Drawable d = item.getCachedImageDrawable();
		if (d != null) return d;
		
		Drawable fd = item.getImageDrawable(
				item.getImageViewWidth(), item.getImageViewHeight());
		
		return fd;
	}
	
}
