package org.javenstudio.provider.library.section;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.TouchHelper;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.cocoka.graphics.MultiImageDrawable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.IThumbnailCallback;
import org.javenstudio.provider.library.SectionTouchListener;

public abstract class SectionFolderItem extends SectionListItem {
	private static final Logger LOG = Logger.getLogger(SectionFolderItem.class);

	private final ISectionFolder mData;
	private final int mImageWidth, mImageHeight;
	
	private MultiImageDrawable.DrawableList mDrawables = null;
	private HttpImageItem[] mSlideImages = null;
	
	private float mImageSpace = 0.0f;
	private int mSlideIndex = 0;
	
	public SectionFolderItem(SectionListProvider p, ISectionFolder data) {
		super(p);
		mData = data;
		
		String imageURL = data.getPosterThumbnailURL();
		int imageWidth = data.getWidth();
		int imageHeight = data.getHeight();
		
		mImageWidth = imageWidth;
		mImageHeight = imageHeight;
		addImageItem(imageURL, imageWidth, imageHeight);
		
		data.getSectionThumbnails(new IThumbnailCallback() {
				@Override
				public boolean onThumbnail(String imageURL, int imageWidth, int imageHeight) {
					addImageItem(imageURL, imageWidth, imageHeight);
					return getImageCount() < 3;
				}
			});
	}
	
	public ISectionFolder getSectionData() { return mData; }
	
	public int getImageWidth() { return mImageWidth; }
	public int getImageHeight() { return mImageHeight; }
	
	public boolean showAnimation() { return isShown() == false; }
	public boolean isSelected(IActivity activity) { return false; }
	
	public synchronized HttpImageItem[] getSlideImages() { 
		if (mSlideImages == null) { 
			ArrayList<HttpImageItem> list = new ArrayList<HttpImageItem>(); 
			
			HttpImageItem[] images = getImageItems(3);
			for (int i=0; images != null && i < images.length; i++) { 
				HttpImageItem image = images[i];
				if (image == null) continue;
				
				boolean found = false;
				for (HttpImageItem img : list) { 
					if (img == image) { 
						found = true; break;
					}
				}
				
				if (!found) { list.add(image); }
				if (list.size() >= 4) break;
			}
			
			mSlideImages = list.toArray(new HttpImageItem[list.size()]);
			mDrawables = null;
			mSlideIndex = 0;
		}
		
		return mSlideImages;
	}
	
	public synchronized HttpImageItem getCurrentSlideImage() { 
		HttpImageItem[] images = getSlideImages();
		int index = mSlideIndex;
		
		if (index >= 0 && images != null && index < images.length)
			return images[index];
		
		return null;
	}
	
	private synchronized MultiImageDrawable.DrawableList getDrawableList() { 
		getSlideImages();
		
		if (mDrawables == null) { 
			MultiImageDrawable.DrawableList drawables = new MultiImageDrawable.DrawableList() {
				private final Map<String, Drawable> mMap = new HashMap<String, Drawable>();
				
				@Override
				public int getCount() {
					HttpImageItem[] images = mSlideImages;
					return images != null ? images.length : 0;
				}

				@Override
				public Drawable getDrawableAt(int index, int width, int height, int padding) {
					if (index < 0 || index >= getCount()) 
						return null;
					
					String key = "" + index + "-" + width + "-" + height;
					Drawable d = mMap.get(key);
					if (d == null) {
						HttpImageItem[] images = mSlideImages;
						if (images != null && index >= 0 && index < images.length) { 
							HttpImageItem image = images[index];
							if (image != null && width > 0 && height > 0) 
								d = image.getImage().getThumbnailDrawable(width, height, padding);
						}
						
						if (d != null) 
							mMap.put(key, d);
					}
					
					return d;
				}

				@Override
				public boolean contains(Drawable d) {
					return d != null ? mMap.values().contains(d) : false;
				}
			};
			
			mDrawables = drawables;
		}
		
		return mDrawables;
	}
	
	public void setImageSpace(float space) { mImageSpace = space; }
	public float getImageSpace() { return mImageSpace; }
	
	public Drawable getMultiDrawable() {
		return getMultiDrawable(getImageViewWidth(), getImageViewHeight());
	}
	
	public Drawable getMultiDrawable(final int width, final int height) { 
		float space = getImageSpace();
		MultiImageDrawable fd = new MultiImageDrawable(getDrawableList(), 
				MultiImageDrawable.Mode.INCLUDE, width, height, space);
		TouchHelper.addListener(fd);
		return fd;
	}
	
	public static int getListItemViewRes() {
		return R.layout.section_list_folder_item;
	}

	public static void bindListItemView(final IActivity activity, 
			SectionListBinder binder, final SectionFolderItem item, View view) {
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
			String text = null;
			int subcount = item.getSectionData().getSubCount();
			long sublen = item.getSectionData().getSubLength();
			String timeAgo = AppResources.getInstance().formatTimeAgo(System.currentTimeMillis() 
					- item.getSectionData().getModifiedTime());
			if (subcount > 0 || sublen > 0) {
				String sizeInfo = AppResources.getInstance().formatReadableBytes(sublen);
				int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_folder_information_label);
				if (infoRes == 0) infoRes = R.string.folder_information_label;
				text = activity.getResources().getString(infoRes);
				text = String.format(text, ""+subcount, sizeInfo, timeAgo);
			} else {
				int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_modified_information_label);
				if (infoRes == 0) infoRes = R.string.modified_information_label;
				text = activity.getResources().getString(infoRes);
				text = String.format(text, timeAgo);
			}
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
			SectionListBinder binder, SectionFolderItem item, View view) {
		if (activity == null || binder == null || item == null || view == null) 
			return;
		
		final int imageWidth = (int)activity.getResources().getDimension(R.dimen.section_list_item_poster_width);
		final int imageHeight = (int)activity.getResources().getDimension(R.dimen.section_list_item_poster_height);
		
		item.setImageViewWidth(imageWidth);
		item.setImageViewHeight(imageHeight);
		
		updateListItemView(item, view, true);
	}
	
	public static void updateListItemView(SectionFolderItem item, 
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
		return R.layout.section_grid_folder_item;
	}
	
	public static void bindGridItemView(final IActivity activity, 
			SectionGridBinder binder, final SectionFolderItem item, View view) {
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
			SectionGridBinder binder, SectionFolderItem item, View view) {
		if (activity == null || binder == null || item == null || view == null) 
			return;
		
		final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
		final float imageSpace = activity.getResources().getDimension(R.dimen.section_folder_image_space);
		final int imageHeight = (int)activity.getResources().getDimension(R.dimen.section_grid_item_poster_height);
		final int imageWidth = screenWidth / binder.getColumnSize() - (int)binder.getColumnSpace();
		
		item.setImageViewWidth(imageWidth);
		item.setImageViewHeight(imageHeight);
		item.setImageSpace(imageSpace);
		
		updateGridItemView(item, view, true);
	}
	
	public static void updateGridItemView(SectionFolderItem item, 
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
