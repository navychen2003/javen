package org.javenstudio.provider.media;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.widget.PopupMenu;
import org.javenstudio.cocoka.widget.PopupMenuListener;

public class PhotoPopup implements PopupMenuListener {

	private static final int POPUP_TAG_SELECT = 1;
	
	private final PhotoProvider mProvider;
	private final IActivity mActivity;
	
	public PhotoPopup(PhotoProvider provider, IActivity activity) { 
		mProvider = provider;
		mActivity = activity;
	}
	
	public void showTagSelect(View view) { 
		mActivity.getActivityHelper().showPopupMenu(POPUP_TAG_SELECT, view, this);
	}
	
	@Override
	public void showPopupMenuAt(int id, PopupMenu menu, View view) {
		switch (id) { 
		case POPUP_TAG_SELECT: 
			menu.showAsDropDown(view, -view.getLeft(), -15);
			break;
		}
	}

	@Override
	public PopupMenu createPopupMenu(int id, View view) {
		switch (id) { 
		case POPUP_TAG_SELECT: {
			final PopupMenu newmenu = new PopupMenu(ResourceHelper.getContext(), 
					R.layout.popup_tagselect); 
			
			View contentView = newmenu.getContentView();
			contentView.setBackgroundResource(org.javenstudio.cocoka.app.
					R.drawable.abs__menu_dropdown_panel_holo_light);
			
			return newmenu; 
		}}
		return null;
	}

	@Override
	public void onPopupMenuCreated(int id, PopupMenu menu, View view) {
	}

	@Override
	public void onPopupMenuShow(int id, final PopupMenu menu, View view) {
		switch (id) { 
		case POPUP_TAG_SELECT: {
			final Resources res = mActivity.getResources();
			
			TextView titleView = (TextView)menu.findViewById(R.id.popup_tagselect_title);
			titleView.setText(res.getString(R.string.label_tagselect_title));
			
			GridView gridView = (GridView)menu.findViewById(R.id.popup_tagselect_layout);
			gridView.setSelector(org.javenstudio.cocoka.app.R.drawable.abs__list_selector_holo_light);
			
			final TagAdapter adapter = getTagAdapter(mProvider, mActivity);
			gridView.setAdapter(adapter);
			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						adapter.onItemClick(mProvider, mActivity, position);
						menu.dismiss();
					}
				});
			
			menu.setWidth(mActivity.getResources().getDisplayMetrics().widthPixels); 
			menu.setHeight((int)res.getDimension(R.dimen.popup_tagselect_height));
			
			break;
		}}
	}

	@Override
	public void onPopupMenuDismiss(int id, PopupMenu menu, View view) {
	}

	public static TagAdapter getTagAdapter(PhotoProvider provider, IActivity activity) { 
		return new TagAdapter(activity.getApplicationContext(), getTagItems(provider, activity));
	}
	
	static class TagItem extends ActionItem { 
		private final String mTag;
		
		public TagItem(String name, String tag) { 
			super(name, 0);
			mTag = tag;
		}
		
		public String getTag() { return mTag; }
	}
	
	static class TagAdapter extends ArrayAdapter<TagItem> {
		public TagAdapter(Context context, TagItem[] items) {
			super(context, 0, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, false);
		}
		
		@Override
	    public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, true);
		}
		
		public void onItemClick(PhotoProvider provider, IActivity activity, int position) { 
			final TagItem item = getItem(position);
			if (provider != null && item != null) 
				provider.onQueryTagSubmit(activity, item.getTag());
		}
		
		private View getView(int position, View convertView, boolean dropDown) {
			final Resources res = getContext().getResources();
			final LayoutInflater inflater = LayoutInflater.from(getContext());
			
			View view = convertView;
			if (view == null) 
				view = inflater.inflate(org.javenstudio.cocoka.app.R.layout.actionbar_custom_item, null);
			
			//View layout = view.findViewById(org.javenstudio.cocoka.app.R.id.actionbar_custom_item_text);
			view.setBackgroundResource(org.javenstudio.cocoka.app.R.drawable.photo_action_background_white);
			
			TextView title = (TextView)view.findViewById(org.javenstudio.cocoka.app.R.id.actionbar_custom_item_title);
			TextView subtitle = (TextView)view.findViewById(org.javenstudio.cocoka.app.R.id.actionbar_custom_item_subtitle);
			subtitle.setVisibility(View.GONE);
			
			final TagItem item = getItem(position);
			boolean hasSubTitle = false;
			
			if (dropDown) {
				CharSequence text = item.getTitle();
				CharSequence subtext = item.getSubTitle();
				if (subtext != null && subtext.length() > 0) 
					text = subtext;
				
				title.setText(text);
				
			} else { 
				title.setText(item.getTitle());
				
				CharSequence subtext = item.getSubTitle();
				if (subtext != null && subtext.length() > 0) {
					subtitle.setText(subtext);
					subtitle.setVisibility(View.VISIBLE);
					hasSubTitle = true;
				}
			}
			
			title.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(hasSubTitle ? 
					org.javenstudio.cocoka.app.R.dimen.action_bar_title_text_size : (dropDown ?
					org.javenstudio.cocoka.app.R.dimen.action_bar_title_text_size_dropdown_nosub : 
					org.javenstudio.cocoka.app.R.dimen.action_bar_title_text_size_nosub)));
			
			return view;
		}
	}
	
	static TagItem[] getTagItems(PhotoProvider provider, IActivity activity) { 
		final String[] tags = provider.getPhotoTags();
		final int tagLength = tags != null ? tags.length : 0;
		
		final Resources res = activity.getResources();
		final TagItem[] items = new TagItem[tagLength+1];
		
		for (int i=0; i < items.length; i++) { 
			if (i == 0) {
				items[i] = new TagItem(res.getString(
						R.string.label_tagselect_all), 
						null);
				
			} else {
				String tag = tags[i-1];
				items[i] = new TagItem(tag, tag);
			}
		}
		
		return items; 
	}
	
}
