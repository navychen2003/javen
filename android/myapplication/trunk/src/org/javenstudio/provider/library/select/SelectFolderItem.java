package org.javenstudio.provider.library.select;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class SelectFolderItem extends SelectListItem {
	private static final Logger LOG = Logger.getLogger(SelectFolderItem.class);

	private int mLoadIndex = 0;
	
	public SelectFolderItem(SelectOperation op, 
			SelectFolderItem parent) {
		super(op, parent);
	}

	public abstract String getName();
	public abstract CharSequence getFolderInfo();
	public abstract Drawable getFolderIcon();
	
	public CharSequence getTitle() { return getName(); }
	
	public int getLoadIndex() { return mLoadIndex; }
	
	public void setLoadIndex(int index) { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("setLoadIndex: item=" + this 
					+ " index=" + index);
		}
		mLoadIndex = index; 
	}
	
	@Override
	public void onItemClick(final Activity activity, 
			final ISelectCallback callback) {
		if (activity == null || callback == null) return;
		if (LOG.isDebugEnabled()) LOG.debug("onItemClick: item=" + this);
		
		final SelectListItem parent = getParent();
		final ISelectData data = getData();
		
		getOperation().postSetDialogTitle(data.getName(), 
			AppResources.getInstance().getSectionDialogIcon(data), 
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getOperation().onParentClick(activity, callback, parent);
				}
			}, 
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getOperation().onRefreshClick(activity, callback, 
							SelectFolderItem.this);
				}
			});
		
		getOperation().startLoader(this, ReloadType.DEFAULT);
	}
	
	@Override
	public int getViewRes() {
		return R.layout.select_list_folder_item;
	}

	@Override
	public void bindView(final Activity activity, 
			final ISelectCallback callback, final View view) {
		if (activity == null || callback == null || view == null) 
			return;
		
		boolean selected = callback.isSelected(this);
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindView: item=" + this + " view=" + view 
					+ " selected=" + selected);
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.select_list_item_title);
		if (titleView != null) {
			titleView.setText(getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.select_list_item_subtitle);
		if (subtitleView != null) {
			subtitleView.setText(getFolderInfo());
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.select_list_item_poster_icon);
		if (iconView != null) {
			Drawable icon = getFolderIcon();
			if (icon != null) iconView.setImageDrawable(icon);
			iconView.setVisibility(View.VISIBLE);
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = getOperation().getItemViewBackgroundRes(selected);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onItemClick(activity, callback);
					}
				});
		}
		
		final int imageWidth = (int)ResourceHelper.getResources().getDimension(R.dimen.select_list_item_poster_width);
		final int imageHeight = (int)ResourceHelper.getResources().getDimension(R.dimen.select_list_item_poster_height);
		
		setImageViewWidth(imageWidth);
		setImageViewHeight(imageHeight);
		
		updateView(view, true);
		requestDownload(activity);
	}
	
	@Override
	public void updateView(View view, boolean restartSlide) {
		if (view == null) return;
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.select_list_item_poster_image);
		if (imageView != null) {
			Drawable fd = getItemDrawable();
			if (LOG.isDebugEnabled()) {
				LOG.debug("updateView: view=" + view + " item=" + this 
						+ " drawable=" + fd);
			}
			
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
	}
	
}
