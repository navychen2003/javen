package org.javenstudio.provider.publish.comment;

import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;

public class CommentProvider extends ProviderBase 
		implements CommentCursor.Model {
	private static final Logger LOG = Logger.getLogger(CommentProvider.class);
	
	private final IMediaComments mComments;
	private final CommentBinder mBinder;
	private final CommentDataSets mDataSets;
	
	private OnCommentClickListener mItemClickListener = null;
	private OnCommentClickListener mViewClickListener = null;
	private OnCommentClickListener mUserClickListener = null;
	
	public CommentProvider(String name, int iconRes, 
			IMediaComments comments, CommentFactory factory) { 
		super(name, iconRes);
		mComments = comments;
		mBinder = factory.createCommentBinder(this);
		mDataSets = factory.createCommentDataSets(this);
	}

	public void setOnItemClickListener(OnCommentClickListener l) { mItemClickListener = l; }
	public OnCommentClickListener getOnItemClickListener() { return mItemClickListener; }
	
	public void setOnViewClickListener(OnCommentClickListener l) { mViewClickListener = l; }
	public OnCommentClickListener getOnViewClickListener() { return mViewClickListener; }
	
	public void setOnUserClickListener(OnCommentClickListener l) { mUserClickListener = l; }
	public OnCommentClickListener getOnUserClickListener() { return mUserClickListener; }
	
	@Override
	public ProviderBinder getBinder() {
		return mBinder;
	}

	@Override
	public IMediaComments getComments() { 
		return mComments;
	}

	@Override
	public CommentDataSets getCommentDataSets() {
		return mDataSets;
	}

	@Override
	public void onDataSetChanged() {
		if (LOG.isDebugEnabled())
			LOG.debug("onDataSetChanged");
	}
	
	protected void postDataSetChanged() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					getCommentDataSets().notifyContentChanged(true);
					getCommentDataSets().notifyDataSetChanged();
				}
			});
	}
	
}
