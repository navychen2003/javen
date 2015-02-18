package org.javenstudio.provider.publish.comment;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.android.data.comment.MediaComments;
import org.javenstudio.cocoka.view.PhotoActionBase;
import org.javenstudio.cocoka.widget.AdvancedAdapter;
import org.javenstudio.common.util.Logger;

public class PhotoActionComments extends PhotoActionBase 
		implements CommentCursor.Model, CommentItem.Binder {
	private static final Logger LOG = Logger.getLogger(PhotoActionComments.class);

	private final IMediaComments mComments;
	private final CommentDataSets mDataSets;
	
	public PhotoActionComments(Context context, 
			IMediaComments comments, String name) { 
		this(context, comments, name, 0);
	}
	
	public PhotoActionComments(Context context, 
			IMediaComments comments, String name, int iconRes) { 
		super(context, name, iconRes);
		mComments = comments != null ? comments : new MediaComments();
		mDataSets = new CommentDataSets(
				new CommentCursorFactory(this));
	}
	
	public final IMediaComments getComments() { return mComments; }
	public final CommentDataSets getCommentDataSets() { return mDataSets; }
	
	@Override
	public void onDataSetChanged() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onDataSetChanged");
		
		//getCommentDataSets().notifyContentChanged(false);
		//getCommentDataSets().notifyDataSetChanged();
	}
	
	@Override
	public void onLoaded() { 
		super.onLoaded();
		
		getCommentDataSets().requery();
		postBindListView();
	}
	
	@Override
	protected synchronized void bindListView(Context context, 
			ListView listView, boolean reclick) { 
		super.bindListView(context, listView, reclick);
		mComments.onViewBinded(this, reclick);
	}
	
	@Override
	protected ListAdapter createAdapter(final Context context, boolean reclick) { 
		if (context == null) return null;
		
		CommentAdapter.AdapterImpl adapter = new CommentAdapter.AdapterImpl(
				context, getCommentDataSets(), 
				R.layout.comment_item_photo
			);
		
		adapter.setViewBinder(new AdvancedAdapter.ViewBinder() { 
				@Override
				public void onViewBinded(AdvancedAdapter.DataSet dataSet, View view, int position) { 
					CommentDataSet ds = (CommentDataSet)dataSet;
					CommentItem item = ds != null ? ds.getCommentItem() : null;
					if (item != null) { 
						item.setBinder(PhotoActionComments.this);
						bindItemView(item, view);
					}
					
					if (position % 2 == 0) 
						view.setBackgroundColor(context.getResources().getColor(R.color.photo_comments_background));
					else
						view.setBackgroundResource(0);
		    	}
			});
		
		return adapter;
	}
	
	@Override
	public final void bindItemView(CommentItem item) { 
		if (item != null) 
			bindItemView(item, item.getDataSetBindedView());
	}
	
	protected void bindItemView(CommentItem item, View view) { 
		if (item == null || view == null) return;
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("bindItemView: item=" + item + " view=" + view);
		
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
			dateView.setText(item.getPosted());
		
		ImageView imageView = (ImageView)view.findViewById(R.id.comment_item_user_avatar);
		if (imageView != null) {
			imageView.setImageDrawable(item.getAuthorIcon());
			imageView.setOnClickListener(item.getAuthorClickListener());
		}
	}
	
}
