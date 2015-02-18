package org.anybox.android.library.app;

import android.view.View;

import org.javenstudio.provider.account.notify.NotifyBinder;
import org.javenstudio.provider.account.notify.NotifyFactory;
import org.javenstudio.provider.account.notify.NotifyItem;
import org.javenstudio.provider.account.notify.NotifyProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxNotifyBinder;
import org.javenstudio.provider.app.anybox.user.AnyboxNotifyProvider;

public class MyNotifyBinder extends AnyboxNotifyBinder {

	static final NotifyFactory FACTORY = new NotifyFactory() {
			@Override
			public NotifyBinder createNotifyBinder(NotifyProvider p) {
				return new MyNotifyBinder((AnyboxNotifyProvider)p);
			}
		};
	
	public MyNotifyBinder(AnyboxNotifyProvider provider) {
		super(provider);
	}
	
	@Override
	protected void onBindView(NotifyItem item, View view) { 
		super.onBindView(item, view);
		
		if (view != null) {
			view.setBackground(null);
		}
	}
	
}
