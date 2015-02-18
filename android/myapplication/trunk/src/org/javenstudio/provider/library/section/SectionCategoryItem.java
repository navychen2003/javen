package org.javenstudio.provider.library.section;

import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.library.IVisibleData;

public class SectionCategoryItem extends SectionListItem {
	private static final Logger LOG = Logger.getLogger(SectionCategoryItem.class);

	private final IVisibleData mData;
	
	public SectionCategoryItem(SectionListProvider p, IVisibleData data) {
		super(p);
		if (data == null) throw new NullPointerException();
		mData = data;
	}
	
	public IVisibleData getData() { return mData; }

	public static int getListItemViewRes() {
		return R.layout.section_category_item;
	}

	public static void bindListItemView(IActivity activity, 
			SectionListBinder binder, SectionCategoryItem item, View view) {
		if (activity == null || binder == null || item == null || view == null)
			return;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindListItemView: view=" + view + " item=" + item);
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_item_title);
		if (titleView != null) {
			titleView.setText(item.getData().getName());
		}
	}

	public static void updateListItemView(SectionCategoryItem item, 
			View view, boolean restartSlide) {
	}
	
	public static int getGridItemViewRes() {
		return R.layout.section_category_item;
	}
	
	public static void bindGridItemView(IActivity activity, 
			SectionGridBinder binder, SectionCategoryItem item, View view) {
		if (activity == null || binder == null || item == null || view == null)
			return;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("bindGridItemView: view=" + view + " item=" + item);
		
		final TextView titleView = (TextView)view.findViewById(R.id.section_item_title);
		if (titleView != null) {
			titleView.setText(item.getData().getName());
		}
	}
	
	public static void updateGridItemView(SectionCategoryItem item, 
			View view, boolean restartSlide) {
	}
	
}
