package org.javenstudio.provider.people.group;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.provider.ProviderBinderBase;
import org.javenstudio.provider.ProviderCallback;

public class GroupListBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(GroupListBinder.class);
	
	private final GroupListProvider mProvider;
	
	public GroupListBinder(GroupListProvider p) { 
		mProvider = p;
	}
	
	public final GroupListProvider getProvider() { return mProvider; }
	
	protected GroupDataSets getGroupDataSets() { 
		return getProvider().getGroupDataSets();
	}
	
	protected OnGroupClickListener getOnGroupClickListener() { 
		return getProvider().getOnGroupItemClickListener();
	}
	
	@Override
	protected View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.provider_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	protected int getColumnSize(IActivity activity) { 
		int size = activity.getResources().getInteger(R.integer.list_column_size);
		return size > 1 ? size : 1;
	}
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return GroupAdapter.createAdapter(activity, getGroupDataSets(), 
				this, R.layout.provider_container, 
				getColumnSize(activity));
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
		if (adapter == null) return;
		
		GroupAdapter groupAdapter = (GroupAdapter)adapter;
		groupAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
		
		// load next albums
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		if (lastVisibleItem > 0 && groupAdapter.getCount() > 0) { 
			ProviderCallback callback = (ProviderCallback)activity.getCallback();
			callback.getController().getModel().loadNextPage(callback, 
					groupAdapter.getCount(), lastVisibleItem);
		}
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null) return -1;
		
		GroupAdapter groupAdapter = (GroupAdapter)adapter;
		return groupAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getGroupItemViewRes();
	}
	
	@Override
	public void addItemView(IActivity activity, DataBinderItem item, 
			ViewGroup container, View view, int index, int count) { 
		if (index > 0) { 
			container.addView(new View(activity.getActivity()), 
					new LinearLayout.LayoutParams((int)getColumnSpace(activity), 
							LinearLayout.LayoutParams.MATCH_PARENT));
		}
		
		if (view == null) 
			view = new View(activity.getActivity());
		
		container.addView(view, 
				new LinearLayout.LayoutParams(0, 
						LinearLayout.LayoutParams.WRAP_CONTENT, 1));
	}
	
	@Override
	public final void bindItemView(final IActivity activity, 
			DataBinderItem item, View view) { 
		final GroupItem groupItem = (GroupItem)item;
		onUpdateViews(groupItem, view);
		
		final OnGroupClickListener listener = getOnGroupClickListener();
		if (listener != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onGroupClick(activity.getActivity(), groupItem);
					}
				});
		}
	}
	
	@Override
	public final void updateItemView(final IActivity activity, 
			DataBinderItem item, View view) { 
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {}
	
	private int getGroupItemViewRes() { return R.layout.group_item; }
	
	protected void onUpdateViews(GroupItem item) { 
		if (item == null) return;
		onUpdateViews(item, item.getBindView());
	}
	
	protected void onUpdateViews(GroupItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView titleView = (TextView)view.findViewById(R.id.group_item_title);
		if (titleView != null) 
			titleView.setText(item.getTitle());
		
		TextView contentView = (TextView)view.findViewById(R.id.group_item_text);
		if (contentView != null)
			contentView.setText(item.getSummary());
		
		TextView userView = (TextView)view.findViewById(R.id.group_item_name);
		if (userView != null)
			userView.setText(item.getName());
		
		TextView dateView = (TextView)view.findViewById(R.id.group_item_date);
		if (dateView != null)
			dateView.setText(item.getCreateDate());
		
		ImageView imageView = (ImageView)view.findViewById(R.id.group_item_avatar);
		if (imageView != null) {
			imageView.setImageDrawable(item.getGroupDrawable(100, 100));
			imageView.setOnClickListener(item.getGroupClickListener());
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.group_item_logo);
		if (logoView != null) {
			Drawable d = item.getProviderIcon();
			if (d != null) { 
				logoView.setImageDrawable(d); 
				logoView.setVisibility(View.VISIBLE);
			} else
				logoView.setVisibility(View.GONE);
		}
	}
	
}
