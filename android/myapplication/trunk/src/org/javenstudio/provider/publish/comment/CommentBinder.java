package org.javenstudio.provider.publish.comment;

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

public class CommentBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback, CommentItem.Binder {
	//private static final Logger LOG = Logger.getLogger(CommentBinder.class);
	
	private final CommentProvider mProvider;
	
	public CommentBinder(CommentProvider provider) {
		mProvider = provider;
	}

	public final CommentProvider getProvider() { return mProvider; }

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
		
		return CommentAdapter.createAdapter(activity, mProvider.getCommentDataSets(), 
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
		if (adapter == null || !(adapter instanceof CommentAdapter)) 
			return;
		
		CommentAdapter commentAdapter = (CommentAdapter)adapter;
		commentAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null || !(adapter instanceof CommentAdapter)) 
			return -1;
		
		CommentAdapter commentAdapter = (CommentAdapter)adapter;
		return commentAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getCommentItemViewRes();
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
	public final void bindItemView(IActivity activity, 
			DataBinderItem item, View view) { 
		bindItemView((CommentItem)item, view);
	}
	
	@Override
	public final void updateItemView(IActivity activity, 
			DataBinderItem item, View view) { 
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) {}
	
	protected int getCommentItemViewRes() { return R.layout.comment_item; }
	
	@Override
	public final void bindItemView(CommentItem item) { 
		if (item != null) 
			bindItemView(item, item.getBindView());
	}
	
	protected void bindItemView(CommentItem item, View view) { 
		if (item == null || view == null) return;
		
		TextView titleView = (TextView)view.findViewById(R.id.comment_item_title);
		if (titleView != null) 
			titleView.setText(item.getTitle());
		
		TextView contentView = (TextView)view.findViewById(R.id.comment_item_text);
		if (contentView != null)
			contentView.setText(item.getContent());
		
		TextView userView = (TextView)view.findViewById(R.id.comment_item_user_name);
		if (userView != null)
			userView.setText(item.getAuthor());
		
		TextView dateView = (TextView)view.findViewById(R.id.comment_item_date);
		if (dateView != null)
			dateView.setText(item.getPostDate());
		
		ImageView imageView = (ImageView)view.findViewById(R.id.comment_item_user_avatar);
		if (imageView != null) {
			imageView.setImageDrawable(item.getAuthorIcon());
			imageView.setOnClickListener(item.getAuthorClickListener());
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.comment_item_logo);
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
