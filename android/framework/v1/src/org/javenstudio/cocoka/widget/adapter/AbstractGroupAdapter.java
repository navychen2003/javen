package org.javenstudio.cocoka.widget.adapter;

import android.content.Context;

import org.javenstudio.cocoka.widget.ExpandableAdapter;

public abstract class AbstractGroupAdapter<T extends IExpandableObject, E extends IExpandableObject> 
					extends ExpandableAdapter {

	public AbstractGroupAdapter(Context context, AbstractGroupDataSets<T, E> dataSets, 
			int groupResource, String[] groupFrom, int[] groupTo, 
			int childResource, String[] childFrom, int[] childTo) { 
		super(context, dataSets, groupResource, groupFrom, groupTo, childResource, childFrom, childTo); 
	}
	
}
