package org.javenstudio.provider.people.group;

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
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.cocoka.app.RefreshGridView;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.provider.ProviderBinder;

public abstract class GroupBinder extends DataBinder 
		implements ProviderBinder, IRefreshView.RefreshListener {
	//private static final Logger LOG = Logger.getLogger(GroupBinder.class);
	
	protected abstract GroupInfoProvider getProvider();
	
	@Override
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState) {
	}
	
	@Override
	public final View bindView(IActivity activity, LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		final GroupItem item = getProvider().getGroupItem();
		
		final View headerView = inflater.inflate(R.layout.group_header, null);
		item.bindHeaderView(headerView);
		
		GroupAction selectTab = initActions(activity, inflater, item, headerView);
		
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
	
	private GroupAction initActions(final IActivity activity, LayoutInflater inflater, 
			GroupItem item, View headerView) { 
		if (activity == null || inflater == null || item == null || headerView == null) 
			return null;
		
		final GroupAction[] actions = item.getActionItems(activity);
		final ViewGroup actionRoot = (ViewGroup)headerView.findViewById(R.id.group_header_actions);
		
		GroupAction defaultAction = null;
		//TextView defaultView = null;
		
		GroupAction selectAction = null;
		//TextView selectView = null;
		
		for (int i=0; actions != null && i < actions.length; i++) { 
			final GroupAction action = actions[i];
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
			GroupItem item, final View listView, final View headerView) { 
		if (activity == null || inflater == null || item == null || listView == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		int imageHeight = (int)activity.getResources().getDimension(R.dimen.group_background_height);
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
	
	protected void onUpdateViews(GroupItem item) { 
		if (item != null) onUpdateViews(item, item.getHeaderView());
	}
	
	protected void onUpdateViews(GroupItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView userView = (TextView)view.findViewById(R.id.group_header_name);
		if (userView != null)
			userView.setText(item.getGroupName());
		
		TextView membercountView = (TextView)view.findViewById(R.id.group_header_membercount);
		if (membercountView != null)
			membercountView.setText(""+item.getMemberCount());
		
		TextView topiccountView = (TextView)view.findViewById(R.id.group_header_topiccount);
		if (topiccountView != null)
			topiccountView.setText(""+item.getTopicCount());
		
		TextView photocountView = (TextView)view.findViewById(R.id.group_header_photocount);
		if (photocountView != null) 
			photocountView.setText(""+item.getPhotoCount());
		
		TextView followcountView = (TextView)view.findViewById(R.id.group_header_followcount);
		if (followcountView != null)
			followcountView.setText("0");
		
		onUpdateImages(item, false);
	}
	
	protected void onUpdateImages(GroupItem item, boolean restartSlide) {
		final View view = item.getBindView();
		if (view == null) return;
		
		ImageView imageView = (ImageView)view.findViewById(R.id.group_header_background);
		if (imageView != null) { 
			Drawable fd = getBackgroundDrawable(item);
			if (fd != null) { 
				item.onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				item.onImageDrawableBinded(fd, restartSlide);
			}
		}
		
		ImageView avatarView = (ImageView)view.findViewById(R.id.group_header_avatar);
		if (avatarView != null) { 
			int size = AppResources.getInstance().getResources().getDimensionPixelSize(R.dimen.headerinfo_avatar_size);
			Drawable d = item.getGroupRoundDrawable(size, 0);
			avatarView.setImageDrawable(d);
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.group_header_logo);
		if (logoView != null) { 
			Drawable logo = item.getProviderIcon();
			logoView.setImageDrawable(logo);
		}
	}
	
	protected void requestDownload(IActivity activity, GroupItem item) { 
		//Image avatarImage = item.getAvatarImage();
		//if (avatarImage != null && avatarImage instanceof HttpImage) 
		//	requestDownload((HttpImage)avatarImage);
	}
	
	private Drawable getBackgroundDrawable(final GroupItem item) { 
		Drawable d = item.getCachedImageDrawable();
		if (d != null) return d;
		
		Drawable fd = item.getBackgroundDrawable(
				item.getImageViewWidth(), item.getImageViewHeight());
		
		return fd;
	}
	
}
