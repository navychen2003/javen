package org.javenstudio.provider.account.host;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class HostListClusterItem extends HostListItem {
	private static final Logger LOG = Logger.getLogger(HostListClusterItem.class);

	private final HostDialogHelper mHelper;
	private final HostData mData;
	
	public HostListClusterItem(HostDialogHelper helper, HostData data) {
		if (helper == null || data == null) throw new NullPointerException();
		mHelper = helper;
		mData = data;
	}
	
	public HostDialogHelper getHelper() { return mHelper; }
	public HostData getData() { return mData; }

	public CharSequence getTitle() { return getData().getDisplayName(); }
	public CharSequence getSubTitle() { return getData().getRequestAddressPort(); }
	
	public Drawable getIcon() { return AppResources.getInstance().getSectionNavIcon(this); }
	public boolean isSelected() { return false; }
	
	public void onItemClick(Activity activity) { getHelper().actionSelect(activity, this); }
	public void onItemRemove(Activity activity) { getHelper().actionRemove(activity, this); }
	
	@Override
	public int getViewRes() { return R.layout.select_list_host_item; }

	@Override
	public void bindView(final Activity activity, View view) {
		if (activity == null || view == null) 
			return;
		
		boolean selected = isSelected();
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
			subtitleView.setText(getSubTitle());
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.select_list_item_poster_icon);
		if (iconView != null) {
			Drawable icon = getIcon();
			if (icon != null) iconView.setImageDrawable(icon);
			iconView.setVisibility(View.VISIBLE);
		}
		
		final ImageView actionView = (ImageView)view.findViewById(R.id.select_list_item_action_image);
		if (actionView != null) {
			int backgroundRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_host_action_selector);
			if (backgroundRes != 0) actionView.setBackgroundResource(backgroundRes);
			
			int iconRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.host_action_icon_remove);
			if (iconRes != 0) actionView.setImageResource(iconRes);
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onItemRemove(activity);
					}
				});
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = getHelper().getItemViewBackgroundRes(selected);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			layoutView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onItemClick(activity);
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
