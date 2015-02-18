package org.javenstudio.provider.publish.discuss;

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

public class TopicBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(TopicBinder.class);
	
	private final TopicProvider mProvider;
	
	public TopicBinder(TopicProvider provider) {
		mProvider = provider;
	}

	public final TopicProvider getProvider() { return mProvider; }

	protected TopicDataSets getTopicDataSets() { 
		return getProvider().getTopicDataSets();
	}
	
	protected OnTopicClickListener getOnTopicClickListener() { 
		return getProvider().getOnTopicItemClickListener();
	}
	
	@Override
	protected View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.provider_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return TopicAdapter.createAdapter(activity, mProvider.getTopicDataSets(), 
				this, R.layout.provider_container, 
				getColumnSize(activity));
	}
	
	protected int getColumnSize(IActivity activity) { 
		int size = activity.getResources().getInteger(R.integer.list_column_size);
		return size > 1 ? size : 1;
	}
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
		if (adapter == null || !(adapter instanceof TopicAdapter)) 
			return;
		
		TopicAdapter topicAdapter = (TopicAdapter)adapter;
		topicAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof TopicAdapter)) 
			return -1;
		
		TopicAdapter topicAdapter = (TopicAdapter)adapter;
		return topicAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getTopicItemViewRes();
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
						getItemViewHeight(activity, container, view), 1));
	}
	
	protected int getItemViewHeight(IActivity activity, 
			ViewGroup container, View view) { 
		if (getColumnSize(activity) > 1) 
			return (int)activity.getResources().getDimension(R.dimen.discuss_item_height);
		
		return getDefaultItemViewHeight(activity, container, view);
	}
	
	protected int getDefaultItemViewHeight(IActivity activity, 
			ViewGroup container, View view) { 
		return LinearLayout.LayoutParams.WRAP_CONTENT;
	}
	
	@Override
	public final void bindItemView(final IActivity activity, 
			DataBinderItem item, View view) {
		final TopicItem contactItem = (TopicItem)item;
		onUpdateViews(contactItem, view);
		
		final OnTopicClickListener listener = getOnTopicClickListener();
		if (listener != null) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onTopicClick(activity.getActivity(), contactItem);
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
	
	protected int getTopicItemViewRes() { return R.layout.topic_item; }
	
	protected void onUpdateViews(TopicItem item) { 
		if (item == null) return;
		onUpdateViews(item, item.getBindView());
	}
	
	protected void onUpdateViews(TopicItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView titleView = (TextView)view.findViewById(R.id.topic_item_title);
		if (titleView != null) 
			titleView.setText(item.getTitle());
		
		TextView contentView = (TextView)view.findViewById(R.id.topic_item_text);
		if (contentView != null)
			contentView.setText(item.getSummary());
		
		TextView userView = (TextView)view.findViewById(R.id.topic_item_user_name);
		if (userView != null)
			userView.setText(item.getUserName());
		
		TextView dateView = (TextView)view.findViewById(R.id.topic_item_date);
		if (dateView != null)
			dateView.setText(item.getUpdateDate());
		
		TextView replyView = (TextView)view.findViewById(R.id.topic_item_comment);
		if (replyView != null) {
			replyView.setText(""+item.getTopicData().getReplyCount());
			replyView.setVisibility(View.VISIBLE);
		}
		
		ImageView imageView = (ImageView)view.findViewById(R.id.topic_item_user_avatar);
		if (imageView != null) {
			imageView.setImageDrawable(item.getUserIcon(100, 100));
			imageView.setOnClickListener(item.getUserClickListener());
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.topic_item_logo);
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
