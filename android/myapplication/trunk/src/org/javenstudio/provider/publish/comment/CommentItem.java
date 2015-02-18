package org.javenstudio.provider.publish.comment;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.comment.IMediaComment;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.ChangeNotifier;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;

public class CommentItem extends DataBinderItem 
		implements ChangeNotifier {
	private static final Logger LOG = Logger.getLogger(CommentItem.class);
	
	public static final String TITLE = "title";
	public static final String CONTENT = "content";
	public static final String AUTHOR = "author";
	public static final String AUTHORICON = "authoricon";
	public static final String POSTED = "posted";
	
	public interface Binder { 
		public void bindItemView(CommentItem item);
	}
	
	private final IMediaComment mComment;
	private final long mIdentity = ResourceHelper.getIdentity();
	
	private CommentDataSet mDataSet = null;
	private Binder mBinder = null;
	
	public CommentItem(IMediaComment comment) { 
		mComment = comment;
		mComment.addNotifier(this);
	}
	
	void setDataSet(CommentDataSet dataSet) { mDataSet = dataSet; }
	public void setBinder(Binder binder) { mBinder = binder; }
	
	final View getDataSetBindedView() { 
		CommentDataSet dataSet = mDataSet;
		return dataSet != null ? dataSet.getBindedView() : null;
	}
	
	@Override
	public Object get(Object key) {
		if (TITLE.equals(key)) { 
			return getTitle();
		} else if (CONTENT.equals(key)) { 
			return getContent();
		} else if (AUTHOR.equals(key)) { 
			return getAuthor();
		} else if (AUTHORICON.equals(key)) {
			return getAuthorIcon();
		} else if (POSTED.equals(key)) { 
			return getPosted();
		}
		return null;
	}

	public CharSequence getTitle() { 
		return mComment != null ? mComment.getTitle() : null;
	}
	
	public CharSequence getContent() { 
		return mComment != null ? mComment.getContent() : null;
	}
	
	public CharSequence getAuthor() { 
		return mComment != null ? mComment.getAuthor() : null;
	}
	
	public Drawable getAuthorIcon() { 
		if (mComment != null) 
			return mComment.getAuthorDrawable(100, 100);
		return null;
	}
	
	public View.OnClickListener getAuthorClickListener() { 
		if (mComment != null) 
			return mComment.getAuthorClickListener();
		return null;
	}
	
	public String getPostDate() { 
		if (mComment != null) {
			long time = mComment.getPostTime();
			if (time > 0) 
				return Utilities.formatDate(time);
		}
		
		return null;
	}
	
	public String getPosted() { 
		if (mComment != null) {
			long time = mComment.getPostTime();
			if (time > 0) 
				return Utilities.formatDate(time);
		}
		
		return null;
	}

	public Drawable getProviderIcon() { 
		if (mComment != null) 
			return mComment.getProviderIcon();
		return null;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		final Binder binder = mBinder;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("onChange: selfChange=" + selfChange + " binder=" + binder);
		
		if (binder == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					binder.bindItemView(CommentItem.this);
				}
			});
	}
	
	protected void onUpdateViewsOnVisible(boolean restartSlide) {}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + mIdentity + "{" + mComment + "}";
	}
	
}
