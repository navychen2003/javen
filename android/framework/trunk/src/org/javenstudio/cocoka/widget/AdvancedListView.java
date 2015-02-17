package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class AdvancedListView extends ListView {

	public AdvancedListView(Context context) {
		super(context); 
		initViews(); 
	}
	
	public AdvancedListView(Context context, AttributeSet attrs) {
		super(context, attrs); 
		initViews(); 
	}
	
	public AdvancedListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
		initViews(); 
	}
	
	protected void initViews() {
	}
	
}
