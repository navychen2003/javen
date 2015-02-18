package org.javenstudio.provider.account.space;

import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.util.Utilities;

public class OverviewFreeItem extends SpaceItem {

	private final IUserSpaceData mData;
	
	public OverviewFreeItem(OverviewSpaceProvider provider, 
			IUserSpaceData data) {
		super(provider);
		if (data == null) throw new NullPointerException();
		mData = data;
	}
	
	public IUserSpaceData getData() { return mData; }

	@Override
	public int getViewRes() {
		return R.layout.storagespace_free;
	}

	@Override
	public void bindView(View view) {
		if (view == null) return;
		
		final TextView titleView = (TextView)view.findViewById(R.id.overviewfree_item_title);
		if (titleView != null) {
			String text = AppResources.getInstance().getResources().getString(R.string.host_space_title);
			titleView.setText(String.format(text, getData().getHostName()));
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.overviewfree_item_subtitle);
		if (subtitleView != null) {
			long totalspace = getData().getTotalSpace();
			String category = getData().getCategory();
			if (category != null && category.equalsIgnoreCase("normal")) {
				String text = AppResources.getInstance().getResources().getString(R.string.total_free_space_normal);
				String space = Utilities.formatSize(totalspace);
				CharSequence title = String.format(text, space);
				if (title != null) subtitleView.setText(title);
			} else {
				String text = AppResources.getInstance().getResources().getString(R.string.total_free_space);
				String space = Utilities.formatSize(totalspace);
				CharSequence title = String.format(text, space);
				if (title != null) subtitleView.setText(title);
			}
			subtitleView.setVisibility(View.VISIBLE);
		}
	}
	
}
