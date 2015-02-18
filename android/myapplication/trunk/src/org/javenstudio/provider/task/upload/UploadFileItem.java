package org.javenstudio.provider.task.upload;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.MessageHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.FileAudio;
import org.javenstudio.android.data.image.FileImage;
import org.javenstudio.android.data.image.FileInfo;
import org.javenstudio.android.data.image.FileVideo;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.entitydb.content.UploadData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.graphics.ChartDrawable;
import org.javenstudio.common.util.Logger;

public class UploadFileItem extends UploadItem {
	private static final Logger LOG = Logger.getLogger(UploadFileItem.class);
	
	private final UploadData mData;
	private final Image mImage;
	private final FileInfo mFileInfo;
	
	public UploadFileItem(UploadProvider p, UploadData data) { 
		super(p);
		if (data == null) throw new NullPointerException();
		mData = data;
		
		String contentType = data.getContentType();
		if (contentType != null && contentType.startsWith("video/")) { 
			FileVideo file = new FileVideo(p.getDataApp(), 
					DataPath.fromString(data.getDataPath()), 
					data.getFilePath());
			mImage = file;
			mFileInfo = file.getFileInfo();
			
		} else if (contentType != null && contentType.startsWith("audio/")) { 
			FileAudio file = new FileAudio(p.getDataApp(), 
					DataPath.fromString(data.getDataPath()), 
					data.getFilePath());
			mImage = file;
			mFileInfo = file.getFileInfo();
			
		} else if (contentType != null && contentType.startsWith("image/")) { 
			FileImage file = new FileImage(p.getDataApp(), 
					DataPath.fromString(data.getDataPath()), 
					data.getFilePath());
			mImage = file;
			mFileInfo = file.getFileInfo();
			
		} else {
			mImage = null;
			mFileInfo = new UploadFileInfo(data.getFilePath(), contentType);
		}
	}
	
	public UploadData getData() { return mData; }
	
	public Image getFileImage() { return mImage; }
	public FileInfo getFileInfo() { return mFileInfo; }
	
	public boolean isVideoFile() { return mImage != null && mImage instanceof FileVideo; }
	public boolean isAudioFile() { return mImage != null && mImage instanceof FileAudio; }
	public boolean isImageFile() { return mImage != null && mImage instanceof FileImage; }
	
	public long getUploadId() { return getData().getId(); }
	public int getUploadStatus() { return getData().getStatus(); }
	
	public String getContentName() { return getData().getContentName(); }
	public Uri getContentUri() { return Uri.parse(getData().getContentUri()); }
	public Uri getPlayUri() { return isVideoFile() ? getContentUri() : null; }
	
	public CharSequence getTitle() { return getContentName(); }
	
	public CharSequence getSubTitle() { 
		if (!getFileInfo().exists()) {
			String text = ResourceHelper.getResources().getString(R.string.upload_file_not_exists_message);
			return String.format(text, "\""+getFileInfo().getFilePath()+"\"");
		} else {
			String text = ResourceHelper.getResources().getString(R.string.upload_information_label);
			String sizeInfo = AppResources.getInstance().formatReadableBytes(getFileInfo().getFileLength());
			return String.format(text, sizeInfo, getStatusText());
		}
	}
	
	public boolean isUploadPending() { 
		return getUploadStatus() == UploadData.STATUS_PENDING;
	}
	
	public boolean isUploadRunning() { 
		return getUploadStatus() == UploadData.STATUS_RUNNING;
	}
	
	public boolean isUploadFinished() { 
		return getUploadStatus() == UploadData.STATUS_FINISHED;
	}
	
	public boolean isUploadFailed() { 
		return getUploadStatus() == UploadData.STATUS_FAILED;
	}
	
	public boolean isUploadAborted() { 
		return getUploadStatus() == UploadData.STATUS_ABORTED;
	}
	
	public String getStatusText() { 
		switch (getUploadStatus()) { 
		case UploadData.STATUS_ADDED: 
			return ResourceHelper.getResources().getString(R.string.upload_item_status_added);
		case UploadData.STATUS_PENDING: 
			return ResourceHelper.getResources().getString(R.string.upload_item_status_pending);
		case UploadData.STATUS_RUNNING: 
			return ResourceHelper.getResources().getString(R.string.upload_item_status_running);
		case UploadData.STATUS_FINISHED: 
			return ResourceHelper.getResources().getString(R.string.upload_item_status_finished);
		case UploadData.STATUS_FAILED: {
			String text = ResourceHelper.getResources().getString(R.string.upload_item_status_failed);
			String error = formatError(getData().getFailedCode(), getData().getFailedMessage());
			return error == null || error.length() == 0 ? text : text + " (" + error + ")";
		} case UploadData.STATUS_ABORTED: 
			return ResourceHelper.getResources().getString(R.string.upload_item_status_aborted);
		}
		return null;
	}
	
	private String formatError(int code, String message) {
		switch (code) {
		case UploadData.CODE_NOACCOUNT:
			return ResourceHelper.getResources().getString(R.string.upload_error_no_account);
		case UploadData.CODE_NOUPLOADER:
			return ResourceHelper.getResources().getString(R.string.upload_error_no_uploader);
		}
		
		String error = MessageHelper.formatHttpStatus(code, message);
		if (error == null || error.length() == 0)
			error = message;
		
		if (error == null || error.length() == 0) {
			if (code != 0) { 
				String text = ResourceHelper.getResources().getString(R.string.upload_error_code);
				error = String.format(text, ""+code);
			}
		}
		
		return error;
	}
	
	public Drawable getDrawable(int width, int height) { 
		Image image = getFileImage();
		if (image != null) return image.getThumbnailDrawable(width, height);
		return null;
	}
	
	private static class ProgressItem implements UploadStreamListener { 
		private final long mUploadId;
		private final ChartDrawable mDrawable;
		private final UploadHandler mHandler;
		
		public ProgressItem(UploadHandler handler, long uploadId) { 
			mUploadId = uploadId;
			
			int abovecolorRes = AppResources.getInstance().getColorRes(AppResources.color.progress_front_color);
			if (abovecolorRes == 0) abovecolorRes = R.color.upload_item_percent_above_color;
			int abovecolor = AppResources.getInstance().getResources().getColor(abovecolorRes);
			int belowcolorRes = R.color.upload_item_percent_below_color;
			int belowcolor = AppResources.getInstance().getResources().getColor(belowcolorRes);
			
			ChartDrawable chart = new ChartDrawable(ChartDrawable.CHART_HISTOGRAM);
			chart.getBelowPaint().setColor(belowcolor);
			chart.getBelowPaint().setStyle(Paint.Style.FILL);
			chart.getAbovePaint().setColor(abovecolor);
			chart.getAbovePaint().setStyle(Paint.Style.FILL);
			mDrawable = chart;
			
			mHandler = handler;
			mHandler.addUploadStreamListener(this);
		}
		
		@Override
		public void onUploadRead(long uploadId, long readSize,
				long totalSize) {
			if (uploadId != mUploadId) return;
			if (totalSize > 0) { 
				mDrawable.setProgress((float)readSize/(float)totalSize);
				mDrawable.postInvalidate();
			}
		}
		
		@Override
		public void onUploadPending(long uploadId) {}
		
		@Override
		public void onUploadRemoved(long uploadId) { 
			if (uploadId != mUploadId) return;
			synchronized (sProgressDrawables) { 
				sProgressDrawables.remove(uploadId);
				mHandler.removeUploadStreamListener(this);
			}
		}
	}
	
	private static Map<Long, ProgressItem> sProgressDrawables = 
			new HashMap<Long, ProgressItem>();
	
	public Drawable getProgressDrawable() { 
		final long uploadId = getData().getId();
		synchronized (sProgressDrawables) { 
			ProgressItem item = sProgressDrawables.get(uploadId);
			if (item == null) { 
				item = new ProgressItem(getProvider().getUploadHandler(), uploadId);
				sProgressDrawables.put(uploadId, item);
			}
			ChartDrawable pd = item.mDrawable;
			if (pd != null && isUploadFinished())
				pd.setProgress(1.0f);
			return pd;
		}
	}
	
	@Override
	public int getViewRes() { return R.layout.upload_item; }
	
	@Override
	public void bindView(final IActivity activity, UploadBinder binder, View view) {
		if (activity == null || binder == null || view == null) 
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindView: activity=" + activity + " binder=" + binder
					+ " item=" + this + " view=" + view);
		}
		
		final int imageWidth = (int)ResourceHelper.getResources().getDimension(R.dimen.upload_item_poster_width);
		final int imageHeight = (int)ResourceHelper.getResources().getDimension(R.dimen.upload_item_poster_height);
		
		setImageViewWidth(imageWidth);
		setImageViewHeight(imageHeight);
		
		final View posterView = view.findViewById(R.id.upload_item_poster);
		final View actionView = view.findViewById(R.id.upload_item_action);
		bindHeaderView(posterView);
		
		if (actionView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_task_action_selector);
			if (backgroundRes != 0) actionView.setBackgroundResource(backgroundRes);
		}
		
		final OnUploadClickListener itemListener = getProvider().getOnItemClickListener();
		if (actionView != null && (isUploadFinished() || isUploadAborted())) { 
			final ImageView actionIcon = (ImageView)view.findViewById(R.id.upload_item_action_icon);
			if (actionIcon != null) {
				actionIcon.setImageResource(isUploadFinished() ? 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_task_done) : 
							AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_task_cancel));
			}
			actionView.setOnClickListener(null);
			
		} else if (actionView != null && itemListener != null) { 
			final ImageView actionIcon = (ImageView)view.findViewById(R.id.upload_item_action_icon);
			if (actionIcon != null) {
				actionIcon.setImageResource(
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_task_cancel));
			}
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						itemListener.onUploadClick(activity.getActivity(), UploadFileItem.this);
					}
				});
		}
		
		final OnUploadClickListener imageListener = getProvider().getOnViewClickListener();
		if (posterView != null && imageListener != null) { 
			posterView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						imageListener.onUploadClick(activity.getActivity(), UploadFileItem.this);
					}
				});
		}
		
		updateView(view, true);
	}
	
	@Override
	public void updateView(View view, boolean restartSlide) { 
		if (view == null) return;
		
		TextView titleView = (TextView)view.findViewById(R.id.upload_item_title);
		if (titleView != null) {
			titleView.setText(getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
		
		TextView subtitleView = (TextView)view.findViewById(R.id.upload_item_subtitle);
		if (subtitleView != null) {
			subtitleView.setText(getSubTitle());
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		ImageView imageView = (ImageView)view.findViewById(R.id.upload_item_poster_image);
		if (imageView != null) {
			Drawable fd = getDrawable(getImageViewWidth(), getImageViewHeight());
			if (fd != null) {
				onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				imageView.setVisibility(View.VISIBLE);
				onImageDrawableBinded(fd, restartSlide);
			} else {
				imageView.setImageDrawable(null);
				imageView.setVisibility(View.GONE);
			}
		}
		
		updateProgress(view);
	}
	
	public void updateProgress(View view) { 
		if (view == null) return;
		
		final ImageView percentView = (ImageView)view.findViewById(R.id.upload_item_percent);
		if (percentView != null) {
			Drawable fd = getProgressDrawable();
			if (fd != null) {
				onImageDrawablePreBind(fd, percentView);
				percentView.setImageDrawable(fd);
				percentView.setVisibility(isUploadRunning() ? View.VISIBLE : View.GONE);
				onImageDrawableBinded(fd, false);
			} else {
				percentView.setImageDrawable(null);
				percentView.setVisibility(View.GONE);
			}
		}
	}
	
}
