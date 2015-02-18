package org.javenstudio.provider.account;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;

public class AccountDetailsItem extends AccountDetailsTab.DetailsItem {

	public static class NameValue implements View.OnClickListener { 
		private final String mName;
		private final CharSequence mValue;
		private boolean mEditable = false;
		
		public NameValue(String name, CharSequence value) {
			this(name, value, true);
		}
		
		public NameValue(String name, CharSequence value, boolean editable) { 
			mName = name;
			mValue = value;
			mEditable = editable;
		}
		
		public String getName() { return mName; }
		public CharSequence getValue() { return mValue; }
		
		public boolean isEditable() { return mEditable; }
		public void setEditable(boolean b) { mEditable = b; }

		@Override
		public void onClick(View v) {
		}
	}
	
	private final List<NameValue> mItems = new ArrayList<NameValue>();
	private final String mName;
	private CharSequence mTitle = null;
	
	public AccountDetailsItem(int name) {
		this(ResourceHelper.getResources().getString(name));
	}
	
	public AccountDetailsItem(String name) {
		mName = name;
	}
	
	public String getName() { return mName; }
	
	public CharSequence getTitle() { return mTitle != null ? mTitle : mName; }
	public void setTitle(CharSequence title) { mTitle = title; }
	
	public void clearNameValues() { 
		synchronized (mItems) { 
			mItems.clear();
		}
	}
	
	public NameValue addNameValue(int nameRes, CharSequence value) { 
		return addNameValue(ResourceHelper.getResources().getString(nameRes), value);
	}
	
	public NameValue addNameValue(String name, CharSequence value) { 
		if (name == null || value == null || name.length() == 0)
			return null;
		
		synchronized (mItems) { 
			NameValue item = new NameValue(name, value);
			mItems.add(item);
			return item;
		}
	}
	
	public NameValue addNameValue(CharSequence value) { 
		if (value == null) return null;
		
		synchronized (mItems) { 
			NameValue item = new NameValue(null, value);
			mItems.add(item);
			return item;
		}
	}
	
	public void addNameValue(NameValue item) { 
		if (item == null) return;
		
		synchronized (mItems) { 
			mItems.add(item);
		}
	}
	
	@Override
	public View getView(Context context, View convertView, boolean dropDown) { 
		final LayoutInflater inflater = LayoutInflater.from(context);
		
		View view = convertView;
		if (view == null || view.getId() != R.layout.accountinfo_profile) 
			view = inflater.inflate(R.layout.accountinfo_profile, null);
		
		View contentView = view.findViewById(R.id.accountinfo_details_content);
		if (contentView != null) {
			int backgroundRes = getCardBackgroundRes();
			if (backgroundRes != 0) contentView.setBackgroundResource(backgroundRes);
		}
		
		TextView titleView = (TextView)view.findViewById(R.id.accountinfo_profile_title);
		if (titleView != null) { 
			int colorRes = getCardTitleColorRes();
			if (colorRes != 0) titleView.setTextColor(context.getResources().getColor(colorRes));
			titleView.setText(getTitle());
		}
		
		ViewGroup itemsRoot = (ViewGroup)view.findViewById(R.id.accountinfo_profile_items);
		if (itemsRoot != null) {
			itemsRoot.removeAllViews();
			bindNameValues(inflater, itemsRoot);
		}
		
		return view;
	}
	
	protected void bindNameValues(LayoutInflater inflater, ViewGroup itemsRoot) {
		if (inflater == null || itemsRoot == null) return;
		
		synchronized (mItems) { 
			for (final NameValue item : mItems) { 
				if (item == null) continue;
				
				View itemView = inflater.inflate(R.layout.accountinfo_profile_item, itemsRoot, false);
				
				TextView itemName = (TextView)itemView.findViewById(R.id.accountinfo_profile_item_name);
				TextView itemValue = (TextView)itemView.findViewById(R.id.accountinfo_profile_item_value);
				View buttonView = (View)itemView.findViewById(R.id.accountinfo_profile_item_button);
				
				int itembgRes = getCardItemBackgroundRes();
				if (itembgRes != 0) itemView.setBackgroundResource(itembgRes);
				
				String name = item.getName();
				if (name != null && name.length() > 0) {
					itemName.setText(name);
					itemName.setVisibility(View.VISIBLE);
				} else { 
					itemName.setVisibility(View.GONE);
				}
				
				itemValue.setText(item.getValue());
				
				if (item.isEditable()) {
					itemView.setOnClickListener(item);
					//buttonView.setOnClickListener(item);
					buttonView.setVisibility(View.VISIBLE);
				} else {
					buttonView.setVisibility(View.INVISIBLE);
				}
				
				itemsRoot.addView(itemView);
			}
		}
	}
	
	protected int getCardBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.accountinfo_card_background);
	}
	
	protected int getCardItemBackgroundRes() {
		return AppResources.getInstance().getDrawableRes(
				AppResources.drawable.card_list_selector);
	}
	
	protected int getCardTitleColorRes() {
		return AppResources.getInstance().getColorRes(
				AppResources.color.card_title_color);
	}
	
}
