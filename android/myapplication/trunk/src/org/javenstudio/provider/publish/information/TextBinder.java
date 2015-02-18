package org.javenstudio.provider.publish.information;

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
import org.javenstudio.android.app.SlideDrawable;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderBinderBase;

public class TextBinder extends ProviderBinderBase 
		implements DataBinder.BinderCallback {
	//private static final Logger LOG = Logger.getLogger(TextBinder.class);

	private final TextProvider mProvider;
	private final boolean mUseGrid;
	
	public TextBinder(TextProvider provider, boolean useGrid) { 
		mProvider = provider;
		mUseGrid = useGrid;
	}

	public final TextProvider getProvider() { return mProvider; }
	public final boolean useGrid() { return mUseGrid; }
	
	@Override
	public View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) {
		return inflater.inflate(R.layout.provider_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	protected int getColumnSize(IActivity activity) { 
		int size = activity.getResources().getInteger(R.integer.list_column_size);
		return useGrid() && size > 1 ? size : 1;
	}
	
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.list_column_space_size);
	}
	
	@Override
	public ListAdapter createListAdapter(final IActivity activity) {
		if (activity == null) return null;
		
		return TextAdapter.createAdapter(activity, 
				mProvider.getDataSets(), this, R.layout.provider_container, 
        		getColumnSize(activity));
	}
	
	@Override
	protected void onFirstVisibleChanged(IActivity activity, ListAdapter adapter, 
			AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) { 
		if (adapter == null) return;
		
		TextAdapter textAdapter = (TextAdapter)adapter;
		textAdapter.onFirstVisibleChanged(firstVisibleItem, visibleItemCount);
	}
	
	@Override
	protected int getFirstVisibleItem(ListAdapter adapter) { 
		if (adapter == null) return -1;
		
		TextAdapter textAdapter = (TextAdapter)adapter;
		return textAdapter.getFirstVisibleItem();
	}
	
	@Override
	public int getItemViewRes(IActivity activity, DataBinderItem item) { 
		return getInformationItemViewRes();
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
		bindTextItem(activity, (TextItem)item, view);
	}
	
	@Override
	public final void updateItemView(IActivity activity, 
			DataBinderItem item, View view) { 
	}
	
	@Override
	public void onItemViewBinded(IActivity activity, DataBinderItem item) { 
		requestDownload(activity, (TextItem)item);
	}
	
	private int getInformationItemViewRes() { return R.layout.information_item; }
	
	private void bindTextItem(final IActivity activity, final TextItem item, View view) { 
		final View headerView = view.findViewById(R.id.information_item_header);
		item.bindHeaderView(headerView);
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		int headerHeight = (int)activity.getResources().getDimension(R.dimen.list_item_header_height);
		int headerWidth = screenWidth;
		
		item.setImageViewWidth(headerWidth);
		item.setImageViewHeight(headerHeight);
		
		setTextView(activity, item);
		onUpdateImages(item, false);
		
		if (item.getImageCount() <= 0) 
			headerView.setVisibility(View.GONE);
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.information_item_image);
		if (imageView != null) {
			final OnTextClickListener listener = getProvider().getOnViewClickListener();
			if (listener != null) {
				imageView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							listener.onTextClick(activity.getActivity(), item);
						}
					});
			}
		}
		
		final View overlayView = view.findViewById(R.id.information_item_overlay);
		if (overlayView != null) {
			final View.OnClickListener actionListener = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!item.isFetching()) 
							HttpImageItem.requestDownload(getShowImageItems(item), true);
						
						onUpdateImages(item, true);
					}
				};
			
			overlayView.setOnClickListener(actionListener);
		}
		
		final OnTextClickListener listener1 = getProvider().getOnItemClickListener();
		final OnTextClickListener listener2 = getProvider().getOnLongClickListener();
		
		if (listener1 != null || listener2 != null) { 
			if (listener1 != null) { 
				view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							listener1.onTextClick(activity.getActivity(), item);
						}
					});
			}
			if (listener2 != null) { 
				view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							return listener2.onTextClick(activity.getActivity(), item);
						}
					});
			}
		} else 
			view.setClickable(false);
	}
	
	private void requestDownload(final IActivity activity, TextItem item) {
		if (activity == null || item == null) return;
		
		//if (NetworkHelper.getInstance().isWifiAvailable() || 
		//	(HttpImageItem.checkNotDownload(item.getImages()) && activity != null && 
		//	activity.getActivityHelper().confirmAutoFetch(true))) {
		//	HttpImageItem.requestDownload(item.getImages(), false);
		//	return;
		//}
		
		TextItem first = mProvider.getDataSets().getTextItemAt(0);
		if (first == item) { 
			item.setImageFetchListener(new TextItem.ImageFetchListener() {
				@Override
				public void onImageFetched(TextItem item, Image image) {
					ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() {
							activity.setShareIntent();
						}
					});
				}
			});
		}
		
		requestDownload(activity, getShowImageItems(item));
	}
	
	protected int getShowImageCount(TextItem item) { 
		return item == null || item.getImageCount() <= 1 ? 1 : 2;
	}
	
	protected HttpImageItem[] getShowImageItems(TextItem item) { 
		return item != null ? item.getImageItems(getShowImageCount(item)) : null;
	}
	
	protected boolean checkNotDownloadImages(TextItem item) { 
		return item != null ? HttpImageItem.checkNotDownload(getShowImageItems(item)) : false;
	}
	
	protected void onUpdateImages(TextItem item, boolean restartSlide) { 
		if (item == null || item.getImageCount() <= 0) 
			return;
		
		final View view = item.getBindView();
		if (view == null) return;
	
		final ImageView imageView = (ImageView)view.findViewById(R.id.information_item_image);
		if (imageView != null) {
			Drawable fd = getItemDrawable(item);
			item.onImageDrawablePreBind(fd, imageView);
			imageView.setImageDrawable(fd);
			item.onImageDrawableBinded(fd, restartSlide);
		}
		
		onUpdateViews(item);
	}
	
	private Drawable getItemDrawable(final TextItem item) { 
		if (item == null) return null;
		
		Drawable d = item.getCachedImageDrawable();
		if (d != null) return d;
		
		final int showCount = getShowImageCount(item);
		if (showCount <= 1) {
			HttpImageItem image = item.getFirstImageItem(false); 
			if (image != null) { 
				return image.getImage().getThumbnailDrawable(
						item.getImageViewWidth(), item.getImageViewHeight());
			}
			
			return null;
		}
		
		SlideDrawable fd = new SlideDrawable(new SlideDrawable.Callback() {
				private int mIndex = 0;
				private int mIndexMax = showCount;
				@Override
				public Drawable next() {
					if (mIndex >= 0 && mIndex < mIndexMax && mIndex < item.getImageCount()) { 
						HttpImageItem image = item.getImageItemAt(mIndex++); 
						if (image == null) return next();
						return image.getImage().getThumbnailDrawable(
								item.getImageViewWidth(), item.getImageViewHeight());
					}
					return null;
				}
			});
		
		return fd;
	}
	
	protected void onUpdateViews(TextItem item) { 
		final View view = item != null ? item.getBindView() : null;
		if (view == null) return;
		
		final View overlayView = view.findViewById(R.id.information_item_overlay);
		final View progressView = view.findViewById(R.id.information_item_progress);
		
		if (overlayView != null) {
			boolean visible = checkNotDownloadImages(item);
			overlayView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		}
		
		if (progressView != null) {
			progressView.setVisibility(item.isFetching() ? View.VISIBLE : View.INVISIBLE);
		}
	}
	
	private void setTextView(IActivity activity, TextItem item) { 
		final View view = item.getBindView();
		if (view == null) return;
		
		String title = item.getTitle();
		String text = item.getSummary();
		
		if (title != null && title.length() > 30) { 
			if (text == null || text.length() == 0) 
				text = title;
			
			title = title.substring(0, 30) + 
					activity.getResources().getString(R.string.ellipsize_end);
		}
		
		TextView titleView = (TextView)view.findViewById(R.id.information_item_title);
		titleView.setText(InformationHelper.formatTitleSpanned(title));
		
		TextView dateView = (TextView)view.findViewById(R.id.information_item_date);
		dateView.setText(item.getDate());
		
		TextView textView = (TextView)view.findViewById(R.id.information_item_text);
		textView.setText(InformationHelper.formatContentSpanned(text));
	}
	
}
