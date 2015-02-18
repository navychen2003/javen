package org.javenstudio.android.information;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.cocoka.app.RefreshGridView;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.common.util.Logger;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.ModelCallback;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.SlideDrawable;
import org.javenstudio.android.app.TouchHelper;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImageItem;

public abstract class InformationBinder extends DataBinder 
		implements DataBinder.BinderCallback, IRefreshView.RefreshListener {
	static final Logger LOG = Logger.getLogger(InformationBinder.class);

	public InformationBinder() {}
	
	public abstract View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container);
	public abstract View findListView(IActivity activity, View rootView);
	
	protected abstract int getColumnSize(IActivity activity);
	protected abstract float getColumnSpace(IActivity activity);
	
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater) {}
	
	public final void bindView(IActivity activity, InformationNavItem item, View rootView) { 
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		View view = findListView(activity, rootView);
		bindListView(activity, view, createAdapter(activity, item));
	}
	
	public final void bindView(IActivity activity, InformationSource source, View rootView) { 
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		View view = findListView(activity, rootView);
		bindListView(activity, view, createAdapter(activity, source));
	}
	
	public final void bindView(IActivity activity, InformationItem item, View rootView) { 
		//increaseBindVersion(activity.getActivity());
		//activity.getActivityHelper().setRefreshView(null);
		
		View view = findListView(activity, rootView);
		bindListView(activity, view, createAdapter(activity, item));
	}
	
	public void onPullToRefresh(IRefreshView refreshView) {}
	public void onReleaseToRefresh(IRefreshView refreshView) {}
	public void onPullReset(IRefreshView refreshView) {}
	public void onTouchEvent(IRefreshView refreshView, MotionEvent event) {}
	
	private void bindListView(final IActivity activity, View view, 
			final InformationAdapter adapter) { 
		if (activity == null || view == null || adapter == null) 
			return;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindListView: view=" + view + " adapter=" + adapter);
		
		InformationAdapter.ViewListener viewListener = new InformationAdapter.ViewListener() {
				@Override
				public void onGetView(int position, int count) {
					if (position >= count - 1 && count > 0) { 
						ModelCallback callback = activity.getCallback();
						callback.getModel().loadNextPage(callback, count, count);
					}
				}
			};
		
		adapter.setViewListener(viewListener);
		
		AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
	    		private int mFirstVisibleItem = -1;
	    		private InformationAdapter mAdapter = adapter;
	    		
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					TouchHelper.onScrollStateChanged(activity, view, scrollState);
				}
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					if (firstVisibleItem != mFirstVisibleItem) { 
						mFirstVisibleItem = firstVisibleItem;
						onFirstVisibleChanged(activity, mAdapter, firstVisibleItem, visibleItemCount);
					}
				}
	        };
		
		if (view instanceof RefreshListView) { 
			RefreshListView listView = (RefreshListView)view;
			activity.getActivityHelper().setRefreshView(listView);
			
	        listView.setAdapter(adapter); 
	        listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
	        listView.setOnScrollListener(scrollListener);
	        
			listView.setOnRefreshListener(activity.getActivityHelper()
					.createListRefreshListener(activity, this));
			listView.setOnPullEventListener(activity.getActivityHelper()
					.createListPullListener(activity, this));
	        
			int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
	        
		} else if (view instanceof ListView) { 
			ListView listView = (ListView)view;
			listView.setAdapter(adapter); 
	        listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
	        listView.setOnScrollListener(scrollListener);
	        
	        int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
	        
		} else if (view instanceof RefreshGridView) { 
			RefreshGridView listView = (RefreshGridView)view;
			activity.getActivityHelper().setRefreshView(listView);
			
	        listView.setAdapter(adapter); 
	        listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
	        listView.setOnScrollListener(scrollListener);
	        
			listView.setOnRefreshListener(activity.getActivityHelper()
					.createGridRefreshListener(activity, this));
			listView.setOnPullEventListener(activity.getActivityHelper()
					.createGridPullListener(activity, this));
	        
			int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
			
		} else if (view instanceof GridView) { 
			GridView listView = (GridView)view;
			listView.setAdapter(adapter); 
	        listView.setOnItemClickListener(getOnItemClickListener(activity, adapter));
	        listView.setOnScrollListener(scrollListener);
			
	        int position = getFirstVisibleItem(adapter);
	        if (position >= 0)
	        	listView.setSelection(position);
		}
	}
	
	protected AdapterView.OnItemClickListener getOnItemClickListener(
			IActivity activity, InformationAdapter adapter) { 
		return null;
	}
	
	private void onFirstVisibleChanged(IActivity activity, InformationAdapter adapter, 
			int firstVisibleItem, int visibleItemCount) { 
		if (activity == null || adapter == null) return;
		
		adapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
		
		// load next page
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		if (lastVisibleItem > 0 && adapter.getCount() > 0) { 
			ModelCallback callback = activity.getCallback();
			callback.getModel().loadNextPage(callback, adapter.getCount(), lastVisibleItem);
		}
	}
	
	private int getFirstVisibleItem(InformationAdapter adapter) { 
		if (adapter == null) return -1;
		
		return adapter.getFirstVisibleItem();
	}
	
	private final InformationAdapter createAdapter(final IActivity activity, 
			InformationItem item) { 
		return createAdapter(activity, item.getInformationDataSets());
	}
	
	private final InformationAdapter createAdapter(final IActivity activity, 
			InformationSource source) { 
		return createAdapter(activity, source.getInformationDataSets());
	}
	
	private final InformationAdapter createAdapter(final IActivity activity, 
			InformationNavItem navItem) { 
		return createAdapter(activity, navItem.getInformationDataSets());
	}
	
	private final InformationAdapter createAdapter(final IActivity activity, 
			InformationDataSets dataSets) { 
		if (activity == null || dataSets == null) return null;
		
		return InformationAdapter.createAdapter(activity, dataSets, 
				this, R.layout.information_container, 
        		getColumnSize(activity));
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getInformationItemViewRes(activity);
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
	
	protected int getItemViewHeight(IActivity activity, ViewGroup container, View view) { 
		return getDefaultItemViewHeight(activity, container, view);
	}
	
	protected int getDefaultItemViewHeight(IActivity activity, ViewGroup container, View view) { 
		return LinearLayout.LayoutParams.WRAP_CONTENT;
	}
	
	@Override
	public final void bindItemView(IActivity activity, DataBinderItem item, View view) { 
		bindInformationOne(activity, (InformationOne)item, view);
	}
	
	@Override
	public final void updateItemView(IActivity activity, DataBinderItem item, View view) { 
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) { 
		requestDownload(activity, (InformationOne)item);
	}
	
	protected int getInformationItemViewRes(IActivity activity) { 
		return R.layout.information_item; 
	}
	
	protected int getInformationHeaderDimenRes(IActivity activity) { 
		return R.dimen.list_item_header_height; 
	}
	
	protected int getInformationItemHeaderViewId() { return R.id.information_item_header; }
	protected int getInformationItemOverlayViewId() { return R.id.information_item_overlay; }
	protected int getInformationItemImageViewId() { return R.id.information_item_image; }
	protected int getInformationItemProgressViewId() { return R.id.information_item_progress; }
	
	private View findViewById(View view, int viewId) { 
		return view != null && viewId != 0 ? view.findViewById(viewId) : null;
	}
	
	protected void setImageViewSize(IActivity activity, InformationOne one, View view) { 
		if (activity == null || one == null || view == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		int headerHeight = (int)activity.getResources().getDimension(getInformationHeaderDimenRes(activity));
		int headerWidth = screenWidth;
		
		one.setImageViewWidth(headerWidth);
		one.setImageViewHeight(headerHeight);
	}
	
	protected OnInformationClickListener getInformationImageClickListener(InformationOne one) { 
		return one.getImageClickListener();
	}
	
	protected OnInformationClickListener getInformationClickListener(InformationOne one) { 
		return one.getOnClickListener();
	}
	
	protected OnInformationClickListener getInformationLongClickListener(InformationOne one) { 
		return one.getOnLongClickListener();
	}
	
	protected void bindInformationOne(final IActivity activity, final InformationOne one, View view) { 
		if (activity == null || one == null || view == null) 
			return;
		
		final View headerView = findViewById(view, getInformationItemHeaderViewId());
		one.bindHeaderView(headerView);
		
		setImageViewSize(activity, one, view);
		
		setInformationTextView(activity, one);
		onUpdateInformationImages(one, false);
		
		if (one.getImageCount() > 0) {
			final OnInformationClickListener listener = getInformationImageClickListener(one);
			if (listener != null) { 
				headerView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							listener.onInformationClick(activity.getActivity(), one);
						}
					});
			}
		} else
			headerView.setVisibility(View.GONE);
		
		final OnInformationClickListener listener1 = getInformationClickListener(one);
		final OnInformationClickListener listener2 = getInformationLongClickListener(one);
		
		if (listener1 != null || listener2 != null) {
			if (listener1 != null) { 
				view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							listener1.onInformationClick(activity.getActivity(), one);
						}
					});
			}
			if (listener2 != null) {
				view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							return listener2.onInformationClick(activity.getActivity(), one);
						}
					});
			}
		} else
			view.setClickable(false);
		
		final View overlayView = findViewById(view, getInformationItemOverlayViewId());
		if (overlayView != null) {
			final View.OnClickListener actionListener = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!one.isFetching()) 
							HttpImageItem.requestDownload(getShowImageItems(one), true);
						
						onUpdateInformationImages(one, true);
					}
				};
			
			overlayView.setOnClickListener(actionListener);
		}
	}
	
	private void requestDownload(final IActivity activity, InformationOne one) { 
		if (activity == null || one == null) return;
		
		InformationDataSet dataSet = one.getDataSet();
		InformationDataSets dataSets = dataSet != null ? (InformationDataSets)dataSet.getDataSets() : null;
		Information first = dataSets != null ? dataSets.getInformationAt(0) : null;
		
		if (first == one) { 
			one.setImageFetchListener(new Information.ImageFetchListener() {
				@Override
				public void onImageFetched(Information item, Image image) {
					ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() {
							activity.setShareIntent();
						}
					});
				}
			});
		}
		
		requestDownloadImages(activity, one);
	}
	
	protected void requestDownloadImages(IActivity activity, InformationOne one) {
		if (activity == null || one == null) return;
		requestDownload(activity, getShowImageItems(one));
	}
	
	protected abstract void setInformationTextView(IActivity activity, 
			InformationOne one);
	
	protected void onUpdateInformationImages(InformationOne one, boolean restartSlide) { 
		if ( one == null || one.getImageCount() <= 0) 
			return;
		
		final View view = one.getBindView();
		if (view == null) return;
	
		final ImageView imageView = (ImageView)findViewById(view, getInformationItemImageViewId());
		if (imageView != null) {
			Drawable fd = getInformationDrawable(one);
			one.onImageDrawablePreBind(fd, imageView);
			imageView.setImageDrawable(fd);
			one.onImageDrawableBinded(fd, restartSlide);
		}
		
		onUpdateInformationViews(one);
	}
	
	protected int getShowImageCount(InformationOne one) { 
		return one == null || one.getImageCount() <= 1 ? 1 : 2;
	}
	
	protected HttpImageItem[] getShowImageItems(InformationOne one) { 
		return one != null ? one.getImageItems(getShowImageCount(one)) : null;
	}
	
	private Drawable getInformationDrawable(final InformationOne one) { 
		if (one == null) return null;
		
		Drawable d = one.getCachedImageDrawable(); 
		if (d != null) return d;
		
		final int showCount = getShowImageCount(one);
		if (showCount <= 1) { 
			HttpImageItem image = one.getFirstImageItem(false); 
			if (image != null) { 
				return image.getImage().getThumbnailDrawable(
						one.getImageViewWidth(), one.getImageViewHeight());
			}
			
			return null;
		}
		
		SlideDrawable fd = new SlideDrawable(new SlideDrawable.Callback() {
				private int mIndex = 0;
				private int mIndexMax = showCount;
				@Override
				public Drawable next() {
					if (mIndex >= 0 && mIndex < mIndexMax && mIndex < one.getImageCount()) { 
						HttpImageItem image = one.getImageItemAt(mIndex++); 
						if (image == null) return next();
						return image.getImage().getThumbnailDrawable(
								one.getImageViewWidth(), one.getImageViewHeight());
					}
					return null;
				}
			});
		
		return fd;
	}
	
	protected void onUpdateInformationViews(InformationOne one) { 
		if (one == null) return;
		
		final View view = one.getBindView();
		if (view == null) return;
		
		final View overlayView = findViewById(view, getInformationItemOverlayViewId());
		final View progressView = findViewById(view, getInformationItemProgressViewId());
		
		if (overlayView != null) {
			boolean visible = checkNotDownloadImages(one);
			overlayView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		}
		
		if (progressView != null) {
			progressView.setVisibility(one.isFetching() ? View.VISIBLE : View.INVISIBLE);
		}
	}
	
	protected boolean checkNotDownloadImages(InformationOne one) { 
		return one != null ? HttpImageItem.checkNotDownload(getShowImageItems(one)) : false;
	}
	
}
