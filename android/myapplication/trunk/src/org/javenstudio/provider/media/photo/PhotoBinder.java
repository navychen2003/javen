package org.javenstudio.provider.media.photo;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.provider.ProviderBinderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class PhotoBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(PhotoBinder.class);
	
	protected abstract PhotoDataSets getPhotoDataSets();
	
	protected abstract OnPhotoClickListener getOnPhotoClickListener();
	protected abstract OnPhotoClickListener getOnPhotoViewClickListener();
	protected abstract OnPhotoClickListener getOnPhotoUserClickListener();
	
	protected int getColumnSize(IActivity activity) { return 1; }
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	public final ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return PhotoAdapter.createAdapter(activity, 
				getPhotoDataSets(), this, R.layout.provider_container, 
				getColumnSize(activity));
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
		if (adapter == null) return;
		
		PhotoAdapter photoAdapter = (PhotoAdapter)adapter;
		photoAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
		
		// load next photos
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		if (lastVisibleItem > 0 && photoAdapter.getCount() > 0) { 
			ProviderCallback callback = (ProviderCallback)activity.getCallback();
			callback.getController().getModel().loadNextPage(callback, 
					photoAdapter.getCount(), lastVisibleItem);
		}
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null) return -1;
		
		PhotoAdapter photoAdapter = (PhotoAdapter)adapter;
		return photoAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getPhotoItemViewRes();
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
	public final void bindItemView(IActivity activity, 
			DataBinderItem item, View view) { 
		bindPhotoItem(activity, (PhotoItem)item, view);
	}
	
	@Override
	public final void updateItemView(IActivity activity, 
			DataBinderItem item, View view) { 
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) { 
		requestDownload(activity, (PhotoItem)item);
	}
	
	protected abstract int getPhotoItemViewRes();
	protected abstract int getPhotoItemHeaderViewId();
	protected abstract int getPhotoItemHeaderHeightId();
	protected abstract int getPhotoItemImageViewId();
	protected abstract int getPhotoItemAvatarViewId();
	protected abstract int getPhotoItemOverlayViewId();
	protected abstract int getPhotoItemProgressViewId();
	protected abstract int getPhotoItemSelectViewId();
	
	protected abstract int getPhotoItemSelectedDrawableRes(boolean selected);
	
	private View findViewById(View view, int viewId) { 
		return view != null && viewId != 0 ? view.findViewById(viewId) : null;
	}
	
	protected void requestDownload(IActivity activity, PhotoItem item) {
		if (activity == null || item == null) 
			return;
		
		final Image bitmapImage = item.getPhoto().getBitmapImage();
		if (bitmapImage != null && bitmapImage instanceof HttpImage) 
			requestDownload(activity, (HttpImage)bitmapImage);
	}
	
	protected void setImageViewSize(IActivity activity, PhotoItem item, View view) { 
		if (activity == null || item == null || view == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		int headerHeight = (int)activity.getResources().getDimension(getPhotoItemHeaderHeightId());
		int headerWidth = screenWidth / getColumnSize(activity);
		
		item.setImageViewWidth(headerWidth);
		item.setImageViewHeight(headerHeight);
	}
	
	private void bindPhotoItem(IActivity activity, PhotoItem item, View view) { 
		if (activity == null || item == null || view == null) 
			return;
		
		final View headerView = findViewById(view, getPhotoItemHeaderViewId());
		item.bindHeaderView(headerView);
		
		setImageViewSize(activity, item, view);
		
		bindPhotoText(activity, item, view);
		bindClickListener(activity, item, view);
		bindSelectView(activity, item, view);
		
		onUpdateImages(item, false);
	}
	
	private void bindSelectView(IActivity activity, PhotoItem item, View view) { 
		if (activity == null || item == null || view == null) 
			return;
		
		final boolean actionMode = item.isActionMode(activity);
		
		final ImageView selectView = (ImageView)findViewById(view, getPhotoItemSelectViewId());
		if (selectView != null) { 
			selectView.setVisibility(actionMode ? View.VISIBLE : View.GONE);
			selectView.setImageResource(getPhotoItemSelectedDrawableRes(item.isSelected(activity)));
		}
	}
	
	protected void bindClickListener(final IActivity activity, final PhotoItem item, final View view) { 
		if (activity == null || item == null || view == null) 
			return;
		
		final boolean actionMode = item.isActionMode(activity);
		final boolean actionModeEnabled = item.isActionModeEnabled();
		
		if (actionMode && actionModeEnabled) { 
			view.setOnLongClickListener(null);
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.setSelected(activity, !item.isSelected(activity));
						bindSelectView(activity, item, view);
					}
				});
			
		} else if (actionModeEnabled) { 
			view.setOnClickListener(null);
			view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return item.enterActionMode(activity);
					}
				});
		}
		
		final OnPhotoClickListener listener = getOnPhotoClickListener();
		if (listener != null && !actionMode) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onPhotoClick(activity.getActivity(), item);
					}
				});
		}
		
		final ImageView imageView = (ImageView)findViewById(view, getPhotoItemImageViewId());
		if (imageView != null && !actionMode) { 
			imageView.setOnClickListener(null);
			imageView.setOnLongClickListener(null);
			
			final OnPhotoClickListener l = getOnPhotoViewClickListener();
			if (l != null) { 
				imageView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							l.onPhotoClick(activity.getActivity(), item);
						}
					});
			}
			
			imageView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return item.enterActionMode(activity);
					}
				});
			
		} else if (imageView != null) { 
			imageView.setOnClickListener(null);
			imageView.setOnLongClickListener(null);
			
			imageView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.setSelected(activity, !item.isSelected(activity));
						bindSelectView(activity, item, view);
					}
				});
		}
		
		final View avatarView = findViewById(view, getPhotoItemAvatarViewId());
		if (avatarView != null && !actionMode) { 
			avatarView.setOnClickListener(null);
			avatarView.setOnLongClickListener(null);
			
			final OnPhotoClickListener l = getOnPhotoUserClickListener();
			if (l != null) { 
				avatarView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							l.onPhotoClick(activity.getActivity(), item);
						}
					});
			}
			
		} else if (avatarView != null) { 
			avatarView.setOnClickListener(null);
			avatarView.setOnLongClickListener(null);
		}
		
		final View overlayView = findViewById(view, getPhotoItemOverlayViewId());
		if (overlayView != null && !actionMode) {
			overlayView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (item.onOverlayClick())
							onUpdateImages(item, true);
					}
				});
			
			overlayView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return item.enterActionMode(activity);
					}
				});
			
		} else if (overlayView != null) { 
			overlayView.setOnClickListener(null);
			overlayView.setOnLongClickListener(null);
			
			overlayView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.setSelected(activity, !item.isSelected(activity));
						bindSelectView(activity, item, view);
					}
				});
		}
	}
	
	protected void bindPhotoText(final IActivity activity, final PhotoItem item, View view) {}
	protected void onUpdateAvatar(PhotoItem item) {}
	
	protected void onUpdateImages(PhotoItem item, boolean restartSlide) {
		final View view = item != null ? item.getBindView() : null;
		if (view == null) return;
		
		final ImageView imageView = (ImageView)findViewById(view, getPhotoItemImageViewId());
		if (imageView != null) { 
			Drawable fd = getItemDrawable(item);
			item.onImageDrawablePreBind(fd, imageView);
			imageView.setImageDrawable(fd);
			item.onImageDrawableBinded(fd, restartSlide);
		}
		
		onUpdateViews(item);
		onUpdateAvatar(item);
	}
	
	protected void onUpdateViews(PhotoItem item) { 
		final View view = item != null ? item.getBindView() : null;
		if (view == null) return;
		
		final View overlayView = findViewById(view, getPhotoItemOverlayViewId());
		final View progressView = findViewById(view, getPhotoItemProgressViewId());
		
		if (overlayView != null) 
			overlayView.setVisibility(item.isOverlayVisible() ? View.VISIBLE : View.INVISIBLE);
		
		if (progressView != null) 
			progressView.setVisibility(item.isFetching() ? View.VISIBLE : View.INVISIBLE);
	}
	
	private Drawable getItemDrawable(final PhotoItem item) { 
		if (item == null) return null;
		
		Drawable d = item.getCachedImageDrawable();
		if (d != null) return d;
		
		Drawable fd = item.getDrawable(
				item.getImageViewWidth(), item.getImageViewHeight());
		
		return fd;
	}
	
}
