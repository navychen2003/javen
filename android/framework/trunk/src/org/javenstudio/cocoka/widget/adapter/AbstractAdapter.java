package org.javenstudio.cocoka.widget.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.cocoka.widget.AdvancedAdapter;

public abstract class AbstractAdapter<T extends IDataSetObject> extends AdvancedAdapter {

	public AbstractAdapter(Context context, AbstractDataSets<T> data,
            int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
		
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return super.getView(position, convertView, parent); 
	}
	
}
