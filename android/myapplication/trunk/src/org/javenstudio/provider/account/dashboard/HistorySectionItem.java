package org.javenstudio.provider.account.dashboard;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.common.util.Logger;

public abstract class HistorySectionItem extends DashboardItem {
	private static final Logger LOG = Logger.getLogger(HistorySectionItem.class);

	private final IHistorySectionData mData;
	private final int mImageWidth, mImageHeight;
	
	public HistorySectionItem(DashboardProvider provider, 
			IHistorySectionData data) {
		super(provider);
		mData = data;
		
		String imageURL = data.getSectionImageURL();
		int imageWidth = data.getSectionImageWidth();
		int imageHeight = data.getSectionImageHeight();
		
		mImageWidth = imageWidth;
		mImageHeight = imageHeight;
		
		if (imageURL != null && imageURL.length() > 0) {
			final HttpImageItem imageItem = new HttpImageItem(imageURL, null, null, null, 
					imageWidth, imageHeight);
			
			addImageItem(new ImageItem() {
					@Override
					public HttpImageItem getHttpImageItem() {
						return imageItem;
					}
				});
		}
	}

	public IHistorySectionData getData() { return mData; }
	public int getImageWidth() { return mImageWidth; }
	public int getImageHeight() { return mImageHeight; }
	
	protected void onTitleClick(IActivity activity) {}
	protected void onAvatarClick(IActivity activity) {}
	protected void onImageClick(IActivity activity) {}
	
	@Override
	public int getViewRes() {
		return R.layout.dashboard_section_item;
	}

	@Override
	public void bindView(final IActivity activity, DashboardBinder binder, View view) {
		if (activity == null || binder == null || view == null) 
			return;
		
		//final TextView hintView = (TextView)view.findViewById(R.id.dashboardsection_item_hint);
		//if (hintView != null) {
		//	String timeago = AppResources.getInstance().formatTimeAgo(
		//			System.currentTimeMillis() - getData().getAccessTime());
		//	String hint = activity.getResources().getString(R.string.visited_timeago_title);
		//	hintView.setText(String.format(hint, timeago));
		//}
		
		final TextView usertitleView = (TextView)view.findViewById(R.id.dashboardsection_item_user_title);
		if (usertitleView != null) {
			int colorRes = binder.getUserTitleColorStateListRes();
			if (colorRes != 0) usertitleView.setTextColor(AppResources.getInstance().getResources().getColorStateList(colorRes));
			usertitleView.setText(getData().getUserTitle());
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.dashboardsection_item_title);
		if (titleView != null) {
			int colorRes = binder.getItemTitleColorStateListRes();
			if (colorRes != 0) titleView.setTextColor(AppResources.getInstance().getResources().getColorStateList(colorRes));
			titleView.setText(getData().getTitle());
			titleView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onTitleClick(activity);
					}
				});
		}
		
		final TextView sizeinfoView = (TextView)view.findViewById(R.id.dashboardsection_item_sizeinfo);
		if (sizeinfoView != null) {
			String title = getData().getTitleInfo();
			if (title != null && title.length() > 0)
				title = "(" + title + ")";
			sizeinfoView.setText(title);
		}
		
		final ImageView avatarView = (ImageView)view.findViewById(R.id.dashboardsection_item_user_avatar);
		if (avatarView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.accountinfo_avatar_round_selector);
			if (backgroundRes != 0) avatarView.setBackgroundResource(backgroundRes);
			
			int size = activity.getResources().getDimensionPixelSize(R.dimen.dashboardsection_avatar_size);
			Drawable avatarIcon = getData().getUserAvatarRoundDrawable(size, 0);
			if (avatarIcon != null) 
				avatarView.setImageDrawable(avatarIcon);
			
			avatarView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onAvatarClick(activity);
					}
				});
		}
		
		final ViewGroup headerView = (ViewGroup)view.findViewById(R.id.dashboardsection_item_headerbg);
		if (headerView != null) {
			int backgroundRes = binder.getItemHeaderViewBackgroundRes();
			if (backgroundRes != 0) headerView.setBackgroundResource(backgroundRes);
		}
		
		final ViewGroup bodyView = (ViewGroup)view.findViewById(R.id.dashboardsection_item_body);
		if (bodyView != null) {
			int backgroundRes = binder.getItemBodyViewBackgroundRes();
			if (backgroundRes != 0) bodyView.setBackgroundResource(backgroundRes);
			addSectionView(activity, binder, bodyView);
		}
	}
	
	private View.OnClickListener mImageListener = null;
	
	protected void addSectionView(final IActivity activity, 
			DashboardBinder binder, ViewGroup container) {
		if (activity == null || binder == null || container == null) 
			return;
		
		mImageListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onImageClick(activity);
				}
			};
		
		if (getImageCount() > 0) {
			addImageView(activity, binder, container);
		}
	}
	
	protected void addImageView(final IActivity activity, 
			DashboardBinder binder, ViewGroup container) {
		if (activity == null || binder == null || container == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("addImageView: activity=" + activity + " binder=" + binder 
					+ " container=" + container);
		}
		
		LayoutInflater inflater = LayoutInflater.from(activity.getActivity());
		View view = inflater.inflate(R.layout.dashboard_section_image, null);
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		final int minHeight = (int)activity.getResources().getDimension(R.dimen.dashboardsection_image_minheight);
		
		int imageWidth = screenWidth;
		int columns = binder.getColumnSize(activity);
		if (columns > 1) imageWidth = imageWidth / columns;
		int imageHeight = imageWidth / 2;
		
		int imgWidth = getImageWidth();
		int imgHeight = getImageHeight();
		
		if (imgWidth > 0 && imgHeight > 0) {
			float rate = (float)imgWidth / (float)imgHeight;
			if (imgWidth >= imageWidth - 10 && rate < 1.1f) {
				//if (rate < 0.8f)
				//	imageHeight = imageWidth;
				//else
					imageHeight = (int)((float)imageWidth * 0.8f);
			} else {
				//imageHeight = imageWidth / 2;
				imageHeight = (int)((float)imageWidth * 0.8f);
			}
		} else {
			imageHeight = (int)((float)imageWidth * 0.8f);
		}
		
		if (imageHeight < minHeight)
			imageHeight = minHeight;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("addImageView: imageWidth=" + imgWidth + " imageHeight=" + imgHeight 
					+ " viewWidth=" + imageWidth + " viewHeight=" + imageHeight);
		}
		
		setImageViewWidth(imageWidth);
		setImageViewHeight(imageHeight);
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.dashboard_section_image);
		if (imageView != null) {
			int backgroundRes = binder.getItemPosterViewBackgroundRes();
			if (backgroundRes != 0) imageView.setBackgroundResource(backgroundRes);
			imageView.setOnClickListener(mImageListener);
		}
		
		final TextView imageText = (TextView)view.findViewById(R.id.dashboard_section_image_text);
		if (imageText != null) {
			int colorRes = AppResources.getInstance().getColorRes(AppResources.color.dashboard_image_text_color);
			if (colorRes != 0) imageText.setTextColor(activity.getResources().getColorStateList(colorRes));
			int textRes = AppResources.getInstance().getStringRes(AppResources.string.dashboard_download_image_label);
			if (textRes == 0) textRes = R.string.download_image_label;
			imageText.setText(textRes);
		}
		
		updateView(view, true);
		
		container.addView(view, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}
	
	@Override
	public void updateView(View view, boolean restartSlide) {
		if (view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("updateImageView: view=" + view + " restartSlide=" + restartSlide);
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.dashboard_section_image);
		if (imageView != null) {
			Drawable fd = getItemDrawable();
			if (fd != null) {
				onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				onImageDrawableBinded(fd, restartSlide);
			}
		}
		
		final TextView imageText = (TextView)view.findViewById(R.id.dashboard_section_image_text);
		if (imageText != null) {
			if (isFetching() || isShowImageDownloaded() || imageView == null) {
				imageText.setVisibility(View.GONE);
				if (imageView != null) imageView.setOnClickListener(mImageListener);
			} else {
				imageText.setVisibility(View.VISIBLE);
				imageView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							DataBinder.requestDownload(getShowImageItems());
						}
					});
			}
		}
		
		final View progressView = view.findViewById(R.id.dashboard_section_progressbar);
		if (progressView != null) {
			progressView.setVisibility(isFetching() ? View.VISIBLE : View.GONE);
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + mIdentity 
				+ "{data=" + getData() + "}";
	}
	
}
