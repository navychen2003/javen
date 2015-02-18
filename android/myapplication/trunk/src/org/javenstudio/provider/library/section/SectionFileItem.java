package org.javenstudio.provider.library.section;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.ISectionData;
import org.javenstudio.provider.library.SectionHelper;
import org.javenstudio.provider.library.SectionTouchListener;

public abstract class SectionFileItem extends SectionListItem {
	private static final Logger LOG = Logger.getLogger(SectionFileItem.class);

	private final ISectionData mData;
	private final int mImageWidth, mImageHeight;
	
	public SectionFileItem(SectionListProvider p, ISectionData data) {
		super(p);
		mData = data;
		
		String imageURL = data.getPosterThumbnailURL();
		int imageWidth = data.getWidth();
		int imageHeight = data.getHeight();
		
		mImageWidth = imageWidth;
		mImageHeight = imageHeight;
		addImageItem(imageURL, imageWidth, imageHeight);
	}
	
	public ISectionData getSectionData() { return mData; }
	
	public int getImageWidth() { return mImageWidth; }
	public int getImageHeight() { return mImageHeight; }
	
	public boolean showAnimation() { return isShown() == false; }
	public boolean isSelected(IActivity activity) { return false; }
	
	public static int getListItemViewRes() {
		return R.layout.section_list_file_item;
	}

	public static void bindListItemView(final IActivity activity, 
			SectionListBinder binder, final SectionFileItem item, View view) {
		if (activity == null || binder == null || item == null || view == null) 
			return;
		
		boolean selected = item.isSelected(activity);
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindListItemView: view=" + view + " item=" + item 
					+ " selected=" + selected);
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_list_item_title);
		if (titleView != null) {
			titleView.setText(item.getSectionData().getName());
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.section_list_item_subtitle);
		if (subtitleView != null) {
			String text = SectionHelper.getFileSizeInfo(item.getSectionData());
			subtitleView.setText(text);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.section_list_item_poster_icon);
		if (iconView != null) {
			Drawable icon = item.getSectionData().getTypeIcon();
			if (icon != null) iconView.setImageDrawable(icon);
		}
		
		final View posterView = view.findViewById(R.id.section_list_item_poster);
		if (posterView != null) {
			int itembgRes = binder.getItemPosterViewBackgroundRes();
			if (itembgRes != 0) posterView.setBackgroundResource(itembgRes);
		}
		
		final View actionView = view.findViewById(R.id.section_list_item_action);
		if (actionView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.section_action_background);
			if (itembgRes != 0) actionView.setBackgroundResource(itembgRes);
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.onItemInfoClick(activity);
					}
				});
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = binder.getItemViewBackgroundRes(selected);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			SectionTouchListener listener = item.getTouchListener(activity, binder);
			layoutView.setOnClickListener(listener);
			layoutView.setOnLongClickListener(listener);
			layoutView.setOnTouchListener(listener);
		}
		
		initListItemView(activity, binder, item, view);
	}

	private static void initListItemView(IActivity activity, 
			SectionListBinder binder, SectionFileItem item, View view) {
		if (activity == null || binder == null || item == null || view == null) 
			return;
		
		final int imageWidth = (int)activity.getResources().getDimension(R.dimen.section_list_item_poster_width);
		final int imageHeight = (int)activity.getResources().getDimension(R.dimen.section_list_item_poster_height);
		
		item.setImageViewWidth(imageWidth);
		item.setImageViewHeight(imageHeight);
		
		updateListItemView(item, view, true);
	}
	
	public static void updateListItemView(SectionFileItem item, 
			View view, boolean restartSlide) {
		if (item == null || view == null) return;
		
		boolean selected = item.isSelected(item.getProvider().getBinder().getBindedActivity());
		if (LOG.isDebugEnabled()) {
			LOG.debug("updateListItemView: view=" + view + " item=" + item 
					+ " selected=" + selected);
		}
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.section_list_item_poster_image);
		if (imageView != null) {
			Drawable fd = item.getListItemDrawable();
			if (fd != null) {
				item.onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				imageView.setVisibility(View.VISIBLE);
				item.onImageDrawableBinded(fd, restartSlide);
			} else {
				imageView.setImageDrawable(null);
				imageView.setVisibility(View.GONE);
			}
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = item.getProvider().getBinder().getItemViewBackgroundRes(selected);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
		}
	}

	public static int getGridItemViewRes() {
		return R.layout.section_grid_file_item;
	}
	
	public static void bindGridItemView(final IActivity activity, 
			SectionGridBinder binder, final SectionFileItem item, View view) {
		if (activity == null || binder == null || item == null || view == null) 
			return;
		
		boolean selected = item.isSelected(activity);
		if (LOG.isDebugEnabled()) {
			LOG.debug("bindGridItemView: view=" + view + " item=" + item 
					+ " selected=" + selected);
		}
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_grid_item_title);
		if (titleView != null) {
			titleView.setText(item.getSectionData().getName());
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.section_grid_item_file_icon);
		if (iconView != null) {
			Drawable icon = item.getSectionData().getTypeIcon();
			if (icon != null) iconView.setImageDrawable(icon);
		}
		
		final View posterView = view.findViewById(R.id.section_grid_item_poster);
		if (posterView != null) {
			int itembgRes = binder.getItemPosterViewBackgroundRes();
			if (itembgRes != 0) posterView.setBackgroundResource(itembgRes);
		}
		
		final View actionView = view.findViewById(R.id.section_grid_item_action);
		if (actionView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.section_action_background);
			if (itembgRes != 0) actionView.setBackgroundResource(itembgRes);
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.onItemInfoClick(activity);
					}
				});
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = binder.getItemViewBackgroundRes(selected);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
			
			SectionTouchListener listener = item.getTouchListener(activity, binder);
			layoutView.setOnClickListener(listener);
			layoutView.setOnLongClickListener(listener);
			layoutView.setOnTouchListener(listener);
		}
		
		initGridItemView(activity, binder, item, view);
	}
	
	private static void initGridItemView(IActivity activity, 
			SectionGridBinder binder, SectionFileItem item, View view) {
		if (activity == null || binder == null || item == null || view == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		//final int imageWidth = (int)activity.getResources().getDimension(R.dimen.section_grid_item_poster_width);
		final int imageHeight = (int)activity.getResources().getDimension(R.dimen.section_grid_item_poster_height);
		final int imageWidth = screenWidth / binder.getColumnSize() - (int)binder.getColumnSpace();
		
		item.setImageViewWidth(imageWidth);
		item.setImageViewHeight(imageHeight);
		
		updateGridItemView(item, view, true);
	}
	
	public static void updateGridItemView(SectionFileItem item, 
			View view, boolean restartSlide) {
		if (item == null || view == null) return;
		
		boolean selected = item.isSelected(item.getProvider().getBinder().getBindedActivity());
		if (LOG.isDebugEnabled()) {
			LOG.debug("updateGridItemView: view=" + view + " item=" + item 
					+ " selected=" + selected);
		}
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.section_list_item_poster_image);
		if (imageView != null) {
			Drawable fd = item.getGridItemDrawable();
			if (fd != null) {
				item.onImageDrawablePreBind(fd, imageView);
				imageView.setImageDrawable(fd);
				imageView.setVisibility(View.VISIBLE);
				item.onImageDrawableBinded(fd, restartSlide);
			} else {
				imageView.setImageDrawable(null);
				imageView.setVisibility(View.GONE);
			}
		}
		
		View layoutView = view;
		if (layoutView != null) {
			int itembgRes = item.getProvider().getBinder().getItemViewBackgroundRes(selected);
			if (itembgRes != 0) layoutView.setBackgroundResource(itembgRes);
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + getIdentity() 
				+ "{data=" + getSectionData() + "}";
	}
	
}
