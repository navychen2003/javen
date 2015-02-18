package org.javenstudio.provider.account.notify;

import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;

public class SystemNotifyItem extends NotifyItem {

	private final ISystemNotifyData mData;
	
	public SystemNotifyItem(NotifyProvider provider, 
			ISystemNotifyData data) {
		super(provider);
		mData = data;
	}

	public ISystemNotifyData getData() { return mData; }
	
	@Override
	public int getViewRes() {
		return R.layout.notify_systemalert;
	}

	@Override
	public void bindView(View view) {
		if (view == null) return;
		
		final TextView titleView = (TextView)view.findViewById(R.id.notify_alert_title);
		if (titleView != null) {
			CharSequence title = AppResources.getInstance().getStringText(AppResources.string.notify_systemalert_title);
			if (title == null || title.length() == 0)
				title = AppResources.getInstance().getResources().getString(R.string.notify_systemalert_title);
			titleView.setText(title);
		}
		
		final TextView subtitleView = (TextView)view.findViewById(R.id.notify_alert_subtitle);
		if (subtitleView != null) {
			ISystemNotifyData data = getData();
			if (data != null) subtitleView.setText(data.getNotice());
		}
	}

}
