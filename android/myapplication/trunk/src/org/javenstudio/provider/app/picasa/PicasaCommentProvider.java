package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.comment.IMediaComment;
import org.javenstudio.android.data.comment.MediaComments;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.ProviderModel;
import org.javenstudio.provider.publish.BaseCommentProvider;
import org.javenstudio.provider.publish.comment.CommentBinder;
import org.javenstudio.provider.publish.comment.CommentFactory;
import org.javenstudio.provider.publish.comment.CommentProvider;

public class PicasaCommentProvider extends BaseCommentProvider {

	public PicasaCommentProvider(String userId, 
			String name, int iconRes, PicasaUserClickListener listener) { 
		super(name, iconRes, 
				new PicasaMediaComments(userId, iconRes, listener), 
				new PicasaCommentFactory());
	}
	
	static class PicasaCommentFactory extends CommentFactory { 
		@Override
		public CommentBinder createCommentBinder(CommentProvider p) { 
			return new PicasaCommentBinder((PicasaCommentProvider)p);
		}
	}
	
	private PicasaMediaComments getPicasaComments() { 
		return (PicasaMediaComments)getComments();
	}
	
	@Override
	public void reloadOnThread(final ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		PicasaMediaComments comments = getPicasaComments();
		if (comments == null) return;
		
		comments.loadCommentEntry(new LoadCallback() {
				@Override
				public void onLoading() {
					callback.getController().getModel().callbackInvoke(
							ProviderModel.ACTION_ONFETCHSTART, null); 
				}
				@Override
				public void onLoaded() {
					postDataSetChanged();
					callback.getController().getModel().callbackInvoke(
							ProviderModel.ACTION_ONFETCHSTOP, null); 
				}
			}, false, (type == ReloadType.FORCE || !comments.isLoaded()));
	}
	
	static class PicasaMediaComments extends MediaComments { 
		
		private final PicasaUserClickListener mUserClickListener;
		private final String mUserId;
		private final int mIconRes;
		
		private GCommentEntry mCommentEntry = null;
		
		public PicasaMediaComments(String userId, int iconRes, 
				PicasaUserClickListener listener) { 
			mUserClickListener = listener;
			mUserId = userId;
			mIconRes = iconRes;
			
			loadCommentEntry(null, false, false);
		}
		
		public String getUserId() { return mUserId; }
		public int getIconRes() { return mIconRes; }
		
		public boolean isLoaded() { return mCommentEntry != null; }
		
		@Override
		public int getCommentCount() {
			GCommentEntry entry = mCommentEntry;
			return entry != null && entry.comments != null 
					? entry.comments.length : 0;
		}

		@Override
		public IMediaComment getCommentAt(int index) {
			GCommentEntry entry = mCommentEntry;
			if (entry != null && entry.comments != null && 
				index >= 0 && index < entry.comments.length) {
				return entry.comments[index];
			}
			return null;
		}
		
		@Override
		public void onViewBinded(LoadCallback callback, boolean reclick) { 
			//loadCommentEntry(callback, true, reclick);
		}
		
		synchronized void loadCommentEntry(LoadCallback callback, 
				boolean schedule, boolean refetch) { 
			if (mCommentEntry == null || refetch) 
				fetchCommentEntry(callback, schedule, refetch);
		}
		
		synchronized void fetchCommentEntry(final LoadCallback callback, 
				boolean schedule, boolean refetch) { 
			GPhotoHelper.fetchComment(GPhotoHelper.getCommentLocation(getUserId()), 
				new GCommentEntry.FetchListener() {
					public void onCommentFetching(String source) { 
						if (callback != null) callback.onLoading();
					}
					@Override
					public void onCommentFetched(GCommentEntry entry) {
						if (entry != null && entry.userId != null && 
							entry.userId.length() > 0) {
							setCommentEntry(entry);
						}
						if (callback != null) callback.onLoaded();
					}
					@Override
					public GCommentItem createCommentItem() { 
						return new GCommentItem(mUserClickListener, getIconRes());
					}
				}, schedule, refetch);
		}
		
		synchronized void setCommentEntry(GCommentEntry entry) { 
			mCommentEntry = entry;
		}
	}
	
}
