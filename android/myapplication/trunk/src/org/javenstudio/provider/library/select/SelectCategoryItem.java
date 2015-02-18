package org.javenstudio.provider.library.select;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.R;

public class SelectCategoryItem extends SelectListItem 
		implements ISelectData {

	private final CharSequence mTitle;
	
	public SelectCategoryItem(SelectOperation op, 
			SelectFolderItem parent, CharSequence title) {
		super(op, parent);
		mTitle = title;
	}
	
	public ISelectData getData() { return this; }
	public CharSequence getTitle() { return mTitle; }
	
	@Override
	public String getName() {
		CharSequence title = mTitle;
		return title != null ? title.toString() : null;
	}
	
	@Override
	public int getViewRes() {
		return R.layout.select_list_category;
	}

	@Override
	public void bindView(Activity activity, 
			final ISelectCallback callback, View view) {
		if (activity == null || callback == null || view == null) 
			return;
		
		final TextView titleView = (TextView)view.findViewById(R.id.select_list_item_title);
		if (titleView != null) {
			titleView.setText(getTitle());
			titleView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void updateView(View view, boolean restartSlide) {
	}

}
