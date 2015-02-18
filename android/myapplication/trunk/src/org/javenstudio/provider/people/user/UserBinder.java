package org.javenstudio.provider.people.user;

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
import org.javenstudio.provider.ProviderBinder;

public abstract class UserBinder extends DataBinder 
		implements ProviderBinder, IRefreshView.RefreshListener {

	protected abstract UserInfoProvider getProvider();
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
	}
	
	@Override
	public final View bindView(IActivity activity, LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		final UserItem item = getProvider().getUserItem();
		
		final View headerView = inflater.inflate(R.layout.user_header, null);
		item.bindHeaderView(headerView);
		
		UserAction selectTab = initActions(activity, inflater, item, headerView);
		
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
		bindItemView(activity, inflater, item, listView, headerView);
		
		if (selectTab != null) 
			selectTab.actionClick(activity, false);
		
		requestDownload(activity, item);
		
		return view;
	}

	protected View inflateView(IActivity activity, LayoutInflater inflater, 
			ViewGroup container) { 
		return inflater.inflate(R.layout.provider_list, container, false);
	}
	
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	public void onPullToRefresh(IRefreshView refreshView) {}
	public void onReleaseToRefresh(IRefreshView refreshView) {}
	public void onPullReset(IRefreshView refreshView) {}
	public void onTouchEvent(IRefreshView refreshView, MotionEvent event) {}
	
	private UserAction initActions(final IActivity activity, LayoutInflater inflater, 
			UserItem item, View headerView) { 
		if (activity == null || inflater == null || item == null || headerView == null) 
			return null;
		
		final UserAction[] actions = item.getActionItems(activity);
		final ViewGroup actionRoot = (ViewGroup)headerView.findViewById(R.id.user_header_actions);
		
		UserAction defaultAction = null;
		//TextView defaultView = null;
		
		UserAction selectAction = null;
		//TextView selectView = null;
		
		for (int i=0; actions != null && i < actions.length; i++) { 
			final UserAction action = actions[i];
			if (action == null) continue;
			
			final TextView actionView = (TextView)inflater.inflate(R.layout.accountinfo_action, actionRoot, false);
			if (actionView != null)
				actionView.setText(action.getTitle());
			
			if (action.isSelected()) {
				selectAction = action;
				//selectView = actionView;
			}
			
			if (defaultAction == null || action.isDefault()) {
				defaultAction = action;
				//defaultView = actionView;
			}
			
			if (actionRoot.getChildCount() > 0) { 
				View divider = inflater.inflate(R.layout.accountinfo_action_divider, actionRoot, false);
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
	
	private void bindItemView(final IActivity activity, LayoutInflater inflater, 
			UserItem item, final View listView, final View headerView) { 
		if (activity == null || inflater == null || item == null || listView == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		int imageHeight = (int)activity.getResources().getDimension(R.dimen.user_image_height);
		int imageWidth = screenWidth;
		
		item.setImageViewWidth(imageWidth);
		item.setImageViewHeight(imageHeight);
		
		item.setHeaderView(activity.getActivity(), headerView);
		item.setListView(activity.getActivity(), listView);
		
		AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
	    		private int mFirstVisibleItem = -1;
	    		//private ListAdapter mAdapter = adapter;
	    		
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					TouchHelper.onScrollStateChanged(activity, view, scrollState);
				}
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					if (firstVisibleItem != mFirstVisibleItem) { 
						mFirstVisibleItem = firstVisibleItem;
						//onFirstVisibleChanged(activity, mAdapter, firstVisibleItem, visibleItemCount);
					}
				}
	        };
		
		if (listView instanceof RefreshListView) {
			RefreshListView list = (RefreshListView)listView;
			list.addHeaderView(headerView);
			list.setOnScrollListener(scrollListener);
			activity.getActivityHelper().setRefreshView(list);
			
			list.setOnRefreshListener(activity.getActivityHelper()
					.createListRefreshListener(activity, this));
			list.setOnPullEventListener(activity.getActivityHelper()
					.createListPullListener(activity, this));
			
		} else if (listView instanceof ListView) { 
			ListView list = (ListView)listView;
			list.addHeaderView(headerView);
			list.setOnScrollListener(scrollListener);
			
		} else if (listView instanceof RefreshGridView) { 
			RefreshGridView list = (RefreshGridView)listView;
			//list.addHeaderView(headerView);
			list.setOnScrollListener(scrollListener);
			activity.getActivityHelper().setRefreshView(list);
			
			list.setOnRefreshListener(activity.getActivityHelper()
					.createGridRefreshListener(activity, this));
			list.setOnPullEventListener(activity.getActivityHelper()
					.createGridPullListener(activity, this));
			
		} else if (listView instanceof GridView) { 
			//GridView list = (GridView)listView;
			//list.addHeaderView(headerView);
			//list.setOnScrollListener(scrollListener);
		}
		
		onUpdateViews(item, headerView);
	}
	
	protected void onUpdateViews(UserItem item) { 
		if (item != null) onUpdateViews(item, item.getHeaderView());
	}
	
	protected void onUpdateViews(UserItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView userView = (TextView)view.findViewById(R.id.user_header_name);
		if (userView != null)
			userView.setText(item.getUserTitle());
		
		TextView photocountView = (TextView)view.findViewById(R.id.user_header_photocount);
		if (photocountView != null) 
			photocountView.setText(""+item.getStatisticCount(UserItem.COUNT_PHOTO));
		
		TextView likecountView = (TextView)view.findViewById(R.id.user_header_likecount);
		if (likecountView != null) 
			likecountView.setText(""+item.getStatisticCount(UserItem.COUNT_LIKE));
		
		TextView commentcountView = (TextView)view.findViewById(R.id.user_header_commentcount);
		if (commentcountView != null) 
			commentcountView.setText(""+item.getStatisticCount(UserItem.COUNT_COMMENT));
		
		TextView followcountView = (TextView)view.findViewById(R.id.user_header_followcount);
		if (followcountView != null) 
			followcountView.setText(""+item.getStatisticCount(UserItem.COUNT_FOLLOW));
		
		onUpdateImages(item, false);
	}
	
	protected void onUpdateImages(UserItem item, boolean restartSlide) {
		final View view = item.getBindView();
		if (view == null) return;
		
		ImageView imageView = (ImageView)view.findViewById(R.id.user_header_image);
		if (imageView != null) { 
			Drawable fd = getImageDrawable(item);
			if (fd != null) { 
				item.onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				item.onImageDrawableBinded(fd, restartSlide);
			}
		}
		
		ImageView avatarView = (ImageView)view.findViewById(R.id.user_header_avatar);
		if (avatarView != null) { 
			int size = AppResources.getInstance().getResources().getDimensionPixelSize(R.dimen.headerinfo_avatar_size);
			Drawable avatarIcon = item.getAvatarRoundDrawable(size, 0);
			if (avatarIcon != null) { 
				//Drawable d = avatarImage.getRoundThumbnailDrawable(132, 132);
				avatarView.setImageDrawable(avatarIcon);
			}
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.user_header_logo);
		if (logoView != null) { 
			Drawable logo = item.getProviderIcon();
			logoView.setImageDrawable(logo);
		}
	}
	
	protected void requestDownload(IActivity activity, UserItem item) { 
		Image avatarImage = item.getAvatarImage();
		if (avatarImage != null && avatarImage instanceof HttpImage) 
			requestDownload((HttpImage)avatarImage);
	}
	
	private Drawable getImageDrawable(final UserItem item) { 
		Drawable d = item.getCachedImageDrawable();
		if (d != null) return d;
		
		Drawable fd = item.getBackgroundDrawable(
				item.getImageViewWidth(), item.getImageViewHeight());
		
		return fd;
	}
	
}
