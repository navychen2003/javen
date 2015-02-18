package org.javenstudio.provider.library.section;

import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;

public class SectionEmptyItem extends SectionListItem {

	public SectionEmptyItem(SectionListProvider p) {
		super(p);
	}
	
	public static int getListItemViewRes() {
		return R.layout.section_empty_item;
	}
	
	public static void bindListItemView(final IActivity activity, 
			SectionListBinder binder, final SectionEmptyItem item, View view) {
	}
	
	public static void updateListItemView(SectionEmptyItem item, 
			View view, boolean restartSlide) {
	}
	
	public static int getGridItemViewRes() {
		return R.layout.section_empty_item;
	}
	
	public static void bindGridItemView(final IActivity activity, 
			SectionGridBinder binder, final SectionEmptyItem item, View view) {
	}
	
	public static void updateGridItemView(SectionEmptyItem item, 
			View view, boolean restartSlide) {
	}
	
}
