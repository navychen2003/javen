package org.javenstudio.provider.people.group;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;

public class GroupDetailsProfile implements GroupDetails.DetailsItem {

	public static class ProfileItem { 
		private final String mName;
		private final CharSequence mValue;
		
		public ProfileItem(String name, CharSequence value) { 
			mName = name;
			mValue = value;
		}
		
		public String getName() { return mName; }
		public CharSequence getValue() { return mValue; }
	}
	
	private final List<ProfileItem> mItems = new ArrayList<ProfileItem>();
	
	public GroupDetailsProfile() {}
	
	public String getTitle() { 
		return ResourceHelper.getResources().getString(R.string.label_title_groupprofile); 
	}
	
	public void clearProfileItems() { 
		synchronized (mItems) { 
			mItems.clear();
		}
	}
	
	public void addProfileItem(int nameRes, CharSequence value) { 
		addProfileItem(ResourceHelper.getResources().getString(nameRes), value);
	}
	
	public void addProfileItem(String name, CharSequence value) { 
		if (name == null || value == null || name.length() == 0 || value.length() == 0)
			return;
		
		synchronized (mItems) { 
			mItems.add(new ProfileItem(name, value));
		}
	}
	
	public void addProfileItem(CharSequence value) { 
		if (value == null || value.length() == 0)
			return;
		
		synchronized (mItems) { 
			mItems.add(new ProfileItem(null, value));
		}
	}
	
	@Override
	public View getView(Context context, View convertView, boolean dropDown) { 
		final LayoutInflater inflater = LayoutInflater.from(context);
		
		if (convertView == null || convertView.getId() != R.layout.group_profile) 
			convertView = inflater.inflate(R.layout.group_profile, null);
		
		TextView title = (TextView)convertView.findViewById(R.id.group_profile_title);
		title.setText(getTitle());
		
		ViewGroup itemsRoot = (ViewGroup)convertView.findViewById(R.id.group_profile_items);
		itemsRoot.removeAllViews();
		
		synchronized (mItems) { 
			for (ProfileItem item : mItems) { 
				if (item == null) continue;
				
				View itemView = inflater.inflate(R.layout.group_profile_item, itemsRoot, false);
				
				TextView itemName = (TextView)itemView.findViewById(R.id.group_profile_item_name);
				TextView itemValue = (TextView)itemView.findViewById(R.id.group_profile_item_value);
				
				String name = item.getName();
				if (name != null && name.length() > 0) {
					itemName.setText(name);
					itemName.setVisibility(View.VISIBLE);
				} else { 
					itemName.setVisibility(View.GONE);
				}
				
				itemValue.setText(item.getValue());
				
				itemsRoot.addView(itemView);
			}
		}
		
		return convertView;
	}
	
}
