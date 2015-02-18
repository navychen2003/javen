package org.javenstudio.provider.library.list;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.app.ActionAdapter;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionFolder;
import org.javenstudio.provider.library.ISectionList;
import org.javenstudio.provider.library.ISectionSearch;
import org.javenstudio.provider.library.SectionHelper;

public class LibraryActionAdapter extends ActionAdapter {
	private static final Logger LOG = Logger.getLogger(LibraryActionAdapter.class);

	public LibraryActionAdapter(Context context, ActionItem[] items) {
		super(context, items);
	}
	
	@Override
	protected View getItemView(ActionItem item, LayoutInflater inflater, 
			View convertView, boolean dropdown) {
		if (dropdown && item instanceof LibraryActionItem) {
			LibraryActionItem actionItem = (LibraryActionItem)item;
			ISectionList data = actionItem.getSectionList();
			if (data instanceof ILibraryData) {
				return getLibraryDropdownView(actionItem, inflater, convertView);
			} else if (data instanceof ISectionFolder) {
				return getFolderDropdownView(actionItem, inflater, convertView);
			} else if (data instanceof ISectionSearch) {
				return getSearchDropdownView(actionItem, inflater, convertView);
			}
		} else if (dropdown && item instanceof LibraryActionCategory) {
			LibraryActionCategory categoryItem = (LibraryActionCategory)item;
			return getCategoryDropdownView(categoryItem, inflater, convertView);
		}
		return item.getView(inflater, convertView, dropdown);
	}
	
	@Override
	protected boolean isItemEnabled(ActionItem item) {
		if (item instanceof LibraryActionItem) {
			LibraryActionItem actionItem = (LibraryActionItem)item;
			LibraryProvider provider = actionItem.getProvider();
			if (provider != null) return true;
		}
		return false; //item.isEnabled();
	}
	
	protected View getCategoryDropdownView(final LibraryActionCategory item, 
			LayoutInflater inflater, View convertView) {
		final View view = inflater.inflate(R.layout.section_dropdown_category, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_dropdown_item_title);
		if (titleView != null) {
			titleView.setText(item.getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
		
		return view;
	}
	
	protected View getLibraryDropdownView(final LibraryActionItem item, 
			LayoutInflater inflater, View convertView) {
		final ILibraryData data = (ILibraryData)item.getSectionList();
		final View view = inflater.inflate(R.layout.section_dropdown_item, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_dropdown_item_title);
		if (titleView != null) {
			titleView.setText(data.getDisplayName());
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.section_dropdown_item_subtitle);
		if (subtitleView != null) {
			subtitleView.setText(SectionHelper.getFolderCountInfo(data));
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.section_dropdown_item_poster_icon);
		if (iconView != null) {
			Drawable icon = AppResources.getInstance().getSectionNavIcon(data);
			if (icon != null) iconView.setImageDrawable(icon);
		}
		
		final View actionView = view.findViewById(R.id.section_dropdown_item_action);
		if (actionView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.section_action_background);
			if (itembgRes != 0) actionView.setBackgroundResource(itembgRes);
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.onItemInfoClick();
					}
				});
		}
		
		updateFolderImageView(item, view, true);
		onItemViewBinded(item, view);
		
		return view;
	}
	
	protected View getFolderDropdownView(final LibraryActionItem item, 
			LayoutInflater inflater, View convertView) {
		final ISectionFolder data = (ISectionFolder)item.getSectionList();
		final View view = inflater.inflate(R.layout.section_dropdown_item, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_dropdown_item_title);
		if (titleView != null) {
			titleView.setText(data.getDisplayName());
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.section_dropdown_item_subtitle);
		if (subtitleView != null) {
			subtitleView.setText(data.getPath());
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.section_dropdown_item_poster_icon);
		if (iconView != null) {
			Drawable icon = AppResources.getInstance().getSectionNavIcon(data);
			if (icon != null) iconView.setImageDrawable(icon);
		}
		
		final View actionView = view.findViewById(R.id.section_dropdown_item_action);
		if (actionView != null) {
			int itembgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.section_action_background);
			if (itembgRes != 0) actionView.setBackgroundResource(itembgRes);
			
			actionView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						item.onItemInfoClick();
					}
				});
		}
		
		updateFolderImageView(item, view, true);
		onItemViewBinded(item, view);
		
		return view;
	}
	
	protected View getSearchDropdownView(final LibraryActionItem item, 
			LayoutInflater inflater, View convertView) {
		final ISectionSearch data = (ISectionSearch)item.getSectionList();
		final View view = inflater.inflate(R.layout.section_dropdown_item, null);
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_dropdown_item_title);
		if (titleView != null) {
			titleView.setText(data.getName());
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.section_dropdown_item_subtitle);
		if (subtitleView != null) {
			String text;
			int count = data.getTotalCount();
			long refreshTime = data.getRefreshTime();
			if (refreshTime > 0) {
				if (count > 0) {
					int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_search_count_information_label);
					if (infoRes == 0) infoRes = R.string.search_count_information_label;
					text = AppResources.getInstance().getResources().getString(infoRes);
					text = String.format(text, ""+count);
				} else {
					int infoRes = AppResources.getInstance().getStringRes(AppResources.string.section_search_notfound_information_label);
					if (infoRes == 0) infoRes = R.string.search_notfound_information_label;
					text = AppResources.getInstance().getResources().getString(infoRes);
				}
			} else {
				text = "";
			}
			subtitleView.setText(text);
			subtitleView.setVisibility(View.VISIBLE);
		}
		
		final ImageView iconView = (ImageView)view.findViewById(R.id.section_dropdown_item_poster_icon);
		if (iconView != null) {
			Drawable icon = AppResources.getInstance().getSectionNavIcon(data);
			if (icon != null) iconView.setImageDrawable(icon);
		}
		
		final View actionView = view.findViewById(R.id.section_dropdown_item_action);
		if (actionView != null) {
			actionView.setVisibility(View.GONE);
		}
		
		updateFolderImageView(item, view, true);
		onItemViewBinded(item, view);
		
		return view;
	}
	
	static void updateFolderImageView(LibraryActionItem item, 
			View view, boolean restartSlide) {
		if (item == null || view == null) return;
		
		final int imageWidth = (int)AppResources.getInstance().getResources().getDimension(R.dimen.section_dropdown_item_poster_width);
		final int imageHeight = (int)AppResources.getInstance().getResources().getDimension(R.dimen.section_dropdown_item_poster_height);
		
		final ImageView imageView = (ImageView)view.findViewById(R.id.section_dropdown_item_poster_image);
		if (imageView != null) {
			Drawable fd = item.getSlideDrawable(imageWidth, imageHeight);
			if (LOG.isDebugEnabled()) {
				LOG.debug("updateFolderImageView: view=" + view + " item=" + item 
						+ " drawable=" + fd);
			}
			
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
	}
	
	protected void onItemViewBinded(LibraryActionItem item, View view) {
		if (item == null || view == null) return;
		if (item.isSelected()) {
			int selectedbgRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.bg_item_selected);
			if (selectedbgRes != 0) view.setBackgroundResource(selectedbgRes);
		}
		item.requestDownload(null);
	}
	
}
