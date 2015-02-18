package org.javenstudio.provider.task.download;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.entitydb.content.DownloadData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.graphics.ChartDrawable;
import org.javenstudio.common.util.Logger;

public class DownloadFileItem extends DownloadItem {
	private static final Logger LOG = Logger.getLogger(DownloadFileItem.class);
	
	private final DownloadData mData;
	
	public DownloadFileItem(DownloadProvider p, DownloadData data) {
		super(p);
		if (data == null) throw new NullPointerException();
		mData = data;
	}
	
	public DownloadData getData() { return mData; }
	
	public long getDownloadId() { return getData().getId(); }
	public int getDownloadStatus() { return getData().getStatus(); }
	
	public String getContentName() { return getData().getContentName(); }
	public Uri getContentUri() { return Uri.parse(getData().getContentUri()); }
	
	public CharSequence getTitle() { return getContentName(); }
	public CharSequence getSubTitle() { return null; }
	
	public boolean isDownloadPending() { 
		return getDownloadStatus() == DownloadData.STATUS_PENDING;
	}
	
	public boolean isDownloadRunning() { 
		return getDownloadStatus() == DownloadData.STATUS_RUNNING;
	}
	
	public boolean isDownloadFinished() { 
		return getDownloadStatus() == DownloadData.STATUS_FINISHED;
	}
	
	public boolean isDownloadFailed() { 
		return getDownloadStatus() == DownloadData.STATUS_FAILED;
	}
	
	public boolean isDownloadAborted() { 
		return getDownloadStatus() == DownloadData.STATUS_ABORTED;
	}
	
	public Drawable getDrawable(int width, int height) { 
		return null;
	}

	@Override
	public int getViewRes() { return R.layout.download_item; }

	@Override
	public void bindView(final IActivity activity, DownloadBinder binder, View view) {
		if (activity == null || view == null) 
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onBindView: activity=" + activity + " binder=" + binder
					+ " item=" + this + " view=" + view);
		}
		
		final int imageWidth = (int)ResourceHelper.getResources().getDimension(R.dimen.download_item_poster_width);
		final int imageHeight = (int)ResourceHelper.getResources().getDimension(R.dimen.download_item_poster_height);
		
		setImageViewWidth(imageWidth);
		setImageViewHeight(imageHeight);
		
		final View posterView = view.findViewById(R.id.download_item_poster);
		final View actionView = view.findViewById(R.id.download_item_action);
		bindHeaderView(posterView);
		
		if (actionView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_task_action_selector);
			if (backgroundRes != 0) actionView.setBackgroundResource(backgroundRes);
		}
		
		final OnDownloadClickListener itemListener = getProvider().getOnItemClickListener();
		if (actionView != null && (isDownloadFinished() || isDownloadAborted())) { 
			final ImageView actionIcon = (ImageView)view.findViewById(R.id.download_item_action_icon);
			if (actionIcon != null) {
				actionIcon.setImageResource(isDownloadFinished() ? 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_task_done) : 
							AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_task_cancel));
			}
			actionView.setOnClickListener(null);
			
		} else if (actionView != null && itemListener != null) { 
			final ImageView actionIcon = (ImageView)view.findViewById(R.id.download_item_action_icon);
			if (actionIcon != null) {
				actionIcon.setImageResource(
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_task_cancel));
			}
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						itemListener.onDownloadClick(activity.getActivity(), 
								DownloadFileItem.this);
					}
				});
		}
		
		final OnDownloadClickListener imageListener = getProvider().getOnViewClickListener();
		if (posterView != null && imageListener != null) { 
			posterView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						imageListener.onDownloadClick(activity.getActivity(), 
								DownloadFileItem.this);
					}
				});
		}
		
		updateView(view, true);
	}
	
	public void onUpdateView(View view, boolean restartSlide) { 
		if (view == null) return;
		
		TextView titleView = (TextView)view.findViewById(R.id.download_item_title);
		if (titleView != null) {
			titleView.setText(getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
		
		TextView subtitleView = (TextView)view.findViewById(R.id.download_item_subtitle);
		if (subtitleView != null) {
			subtitleView.setText(getSubTitle());
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		ImageView imageView = (ImageView)view.findViewById(R.id.download_item_poster_image);
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
		
		final ImageView percentView = (ImageView)view.findViewById(R.id.download_item_percent);
		if (percentView != null) {
			int abovecolorRes = AppResources.getInstance().getColorRes(AppResources.color.progress_front_color);
			if (abovecolorRes == 0) abovecolorRes = R.color.download_item_percent_above_color;
			int abovecolor = AppResources.getInstance().getResources().getColor(abovecolorRes);
			int belowcolorRes = R.color.download_item_percent_below_color;
			int belowcolor = AppResources.getInstance().getResources().getColor(belowcolorRes);
			
			ChartDrawable chart = new ChartDrawable(ChartDrawable.CHART_HISTOGRAM);
			chart.getBelowPaint().setColor(belowcolor);
			chart.getBelowPaint().setStyle(Paint.Style.FILL);
			chart.getAbovePaint().setColor(abovecolor);
			chart.getAbovePaint().setStyle(Paint.Style.FILL);
			chart.setPercent(10); //getData().getUsedPercent());
			
			percentView.setImageDrawable(chart);
			percentView.setVisibility(View.VISIBLE);
		}
	}
	
}
