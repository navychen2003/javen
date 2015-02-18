package org.javenstudio.cocoka.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.javenstudio.cocoka.app.R;

public class PhotoActionExifs extends PhotoActionDetails {

	public PhotoActionExifs(Context context, String name) { 
		this(context, name, 0);
	}
	
	public PhotoActionExifs(Context context, String name, int iconRes) { 
		super(context, name, iconRes);
	}
	
	@Override
	protected ListAdapter createAdapter(Context context, DetailItem[] items) { 
		return new ExifAdapter(context, items);
	}

	static class ExifAdapter extends ArrayAdapter<DetailItem> { 
		public ExifAdapter(Context context, DetailItem[] items) {
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
		
		protected View getView(int position, View convertView, boolean dropDown) {
			//final Resources res = getContext().getResources();
			final LayoutInflater inflater = LayoutInflater.from(getContext());
			
			if (convertView == null) 
				convertView = inflater.inflate(R.layout.photopage_exifs_item, null);
			
			DetailItem item = getItem(position);
			
			TextView nameView = (TextView)convertView.findViewById(R.id.photopage_exifs_item_name);
			TextView valueView = (TextView)convertView.findViewById(R.id.photopage_exifs_item_value);
			
			nameView.setText(item.getName());
			valueView.setText(item.getValue());
			
			return convertView;
		}
	}
	
}
