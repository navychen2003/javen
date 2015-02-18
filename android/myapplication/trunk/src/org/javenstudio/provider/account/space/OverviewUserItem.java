package org.javenstudio.provider.account.space;

import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.util.Utilities;

public class OverviewUserItem extends SpaceItem {

	private final IUserSpaceData mData;
	
	public OverviewUserItem(OverviewUsedProvider provider, 
			IUserSpaceData data) {
		super(provider);
		if (data == null) throw new NullPointerException();
		mData = data;
	}
	
	public IUserSpaceData getData() { return mData; }

	@Override
	public int getViewRes() {
		return R.layout.storagespace_user;
	}

	@Override
	public void bindView(View view) {
		if (view == null) return;
		
		final TextView titleView = (TextView)view.findViewById(R.id.userspace_item_title);
		if (titleView != null) {
			String text = AppResources.getInstance().getResources().getString(R.string.host_space_title);
			titleView.setText(String.format(text, getData().getHostName()));
			titleView.setVisibility(View.VISIBLE);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.userspace_item_subtitle);
		if (subtitleView != null) {
			final String text;
			if (getData().isGroup()) {
				text = AppResources.getInstance().getResources().getString(R.string.group_total_used_space_message);
			} else {
				text = AppResources.getInstance().getResources().getString(R.string.user_total_used_space_message);
			}
			String space = Utilities.formatSize(getData().getUsedSpace());
			subtitleView.setText(String.format(text, getData().getDisplayName(), space));
			subtitleView.setVisibility(View.VISIBLE);
		}
	}
	
}
