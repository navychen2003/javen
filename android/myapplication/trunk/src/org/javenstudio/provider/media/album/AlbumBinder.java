package org.javenstudio.provider.media.album;

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
import org.javenstudio.provider.ProviderBinderBase;
import org.javenstudio.provider.ProviderCallback;

public abstract class AlbumBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(AlbumBinder.class);
	
	protected abstract AlbumDataSets getAlbumDataSets();
	
	protected abstract OnAlbumClickListener getOnAlbumClickListener();
	protected abstract OnAlbumClickListener getOnAlbumViewClickListener();
	
	protected int getColumnSize(IActivity activity) { return 1; }
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) { 
		if (activity == null) return null;
		
		return AlbumAdapter.createAdapter(activity, getAlbumDataSets(), 
				this, R.layout.provider_container, 
        		getColumnSize(activity));
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
		if (adapter == null) return;
		
		AlbumAdapter albumAdapter = (AlbumAdapter)adapter;
		albumAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
		
		// load next albums
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		if (lastVisibleItem > 0 && albumAdapter.getCount() > 0) { 
			ProviderCallback callback = (ProviderCallback)activity.getCallback();
			callback.getController().getModel().loadNextPage(callback, 
					albumAdapter.getCount(), lastVisibleItem);
		}
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null) return -1;
		
		AlbumAdapter albumAdapter = (AlbumAdapter)adapter;
		return albumAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getAlbumItemViewRes();
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
		bindAlbumItem(activity, (AlbumItem)item, view);
	}
	
	@Override
	public final void updateItemView(IActivity activity, 
			DataBinderItem item, View view) { 
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) { 
		requestDownload(activity, (AlbumItem)item);
	}
	
	protected abstract int getAlbumItemViewRes();
	protected abstract int getAlbumItemHeaderViewId();
	protected abstract int getAlbumItemHeaderHeightId();
	protected abstract int getAlbumItemPhotoSpaceId();
	protected abstract int getAlbumItemImageViewId();
	protected abstract int getAlbumItemOverlayViewId();
	protected abstract int getAlbumItemProgressViewId();
	protected abstract int getAlbumItemSelectViewId();
	
	protected abstract int getAlbumItemSelectedDrawableRes(boolean selected);
	
	protected final View findViewById(View view, int viewId) { 
		return view != null && viewId != 0 ? view.findViewById(viewId) : null;
	}
	
	private void requestDownload(IActivity activity, AlbumItem item) {
		if (activity == null || item == null) 
			return;
		
		item.requestDownload();
	}
	
	protected void setImageViewSize(IActivity activity, AlbumItem item, View view) { 
		if (activity == null || item == null || view == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		float photoSpace = (float)activity.getResources().getDimension(getAlbumItemPhotoSpaceId());
		
		int headerHeight = (int)activity.getResources().getDimension(getAlbumItemHeaderHeightId());
		int headerWidth = screenWidth / getColumnSize(activity);
		
		item.setImageViewWidth(headerWidth);
		item.setImageViewHeight(headerHeight);
		//item.setImageDrawable(null);
		
		item.setPhotoSpace(photoSpace);
	}
	
	private void bindAlbumItem(final IActivity activity, final AlbumItem item, View view) { 
		if (activity == null || item == null || view == null) 
			return;
		
		final View headerView = findViewById(view, getAlbumItemHeaderViewId());
		item.bindHeaderView(headerView);
		
		setImageViewSize(activity, item, view);
		
		bindAlbumText(activity, item, view);
		bindClickListener(activity, item, view);
		bindSelectView(activity, item, view);
		
		onUpdateImages(item, false);
	}
	
	private void bindSelectView(IActivity activity, AlbumItem item, View view) { 
		if (activity == null || item == null || view == null) 
			return;
		
		final boolean actionMode = item.isActionMode(activity);
		
		final ImageView selectView = (ImageView)findViewById(view, getAlbumItemSelectViewId());
		if (selectView != null) { 
			selectView.setVisibility(actionMode ? View.VISIBLE : View.GONE);
			selectView.setImageResource(getAlbumItemSelectedDrawableRes(item.isSelected(activity)));
		}
	}
	
	protected void bindClickListener(final IActivity activity, final AlbumItem item, final View view) { 
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
		
		final OnAlbumClickListener listener = getOnAlbumClickListener();
		if (listener != null && !actionMode) { 
			view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onAlbumClick(activity.getActivity(), item);
					}
				});
		}
		
		final ImageView imageView = (ImageView)findViewById(view, getAlbumItemImageViewId());
		if (imageView != null && !actionMode) { 
			imageView.setOnClickListener(null);
			imageView.setOnLongClickListener(null);
			
			final OnAlbumClickListener l = getOnAlbumClickListener(); //getOnAlbumViewClickListener();
			if (l != null) { 
				imageView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							l.onAlbumClick(activity.getActivity(), item);
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
		
		final View overlayView = findViewById(view, getAlbumItemOverlayViewId());
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
	
	// override this
	protected void bindAlbumText(final IActivity activity, final AlbumItem item, View view) {}
	
	protected void onUpdateImages(AlbumItem item, boolean restartSlide) {
		final View view = item != null ? item.getBindView() : null;
		if (view == null) return;
		
		final ImageView imageView = (ImageView)findViewById(view, getAlbumItemImageViewId());
		if (imageView != null) { 
			Drawable fd = getItemDrawable(item);
			item.onImageDrawablePreBind(fd, imageView);
			imageView.setImageDrawable(fd);
			item.onImageDrawableBinded(fd, false);
			
			if (!item.isOverlayVisible()) 
				imageView.setBackgroundResource(0);
		}
		
		onUpdateViews(item);
	}
	
	protected void onUpdateViews(AlbumItem item) { 
		final View view = item != null ? item.getBindView() : null;
		if (view == null) return;
		
		final View overlayView = findViewById(view, getAlbumItemOverlayViewId());
		final View progressView = findViewById(view, getAlbumItemProgressViewId());
		
		if (overlayView != null) 
			overlayView.setVisibility(item.isOverlayVisible() ? View.VISIBLE : View.INVISIBLE);
		
		if (progressView != null) 
			progressView.setVisibility(item.isFetching() ? View.VISIBLE : View.INVISIBLE);
	}
	
	private Drawable getItemDrawable(final AlbumItem item) { 
		if (item == null) return null;
		
		Drawable d = item.getCachedImageDrawable();
		if (d != null) return d;
		
		Drawable fd = item.getAlbumDrawable(
				item.getImageViewWidth(), item.getImageViewHeight());
		
		return fd;
	}
	
}
