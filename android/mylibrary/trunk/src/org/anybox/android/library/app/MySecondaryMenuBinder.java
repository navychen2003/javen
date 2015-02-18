package org.anybox.android.library.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.anybox.android.library.R;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.activity.AccountSecondaryMenuFragment;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountProvider;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountSecondaryMenuBinder;

public class MySecondaryMenuBinder extends AnyboxAccountSecondaryMenuBinder {
	private static final Logger LOG = Logger.getLogger(MySecondaryMenuBinder.class);

	private final MyMenuHelper mHelper = MyMenuHelper.BEACH;
	
	public MySecondaryMenuBinder(AnyboxAccountProvider p) { 
		super(p);
	}
	
	@Override
	public View createAboveView(AccountSecondaryMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.secondmenu_above, null);
	}
	
	@Override
	protected void updateAboveView(AccountSecondaryMenuFragment fragment, View view) {
		if (fragment == null || view == null) return;
		if (LOG.isDebugEnabled()) 
			LOG.debug("updateAboveView: fragment=" + fragment + " view=" + view);
		
		//final AccountMenuActivity menuactivity = (AccountMenuActivity)activity;
		final AccountUser user = getProvider().getAccountItem().getAccountUser();
		
		TextView titleView = (TextView)view.findViewById(R.id.secondmenu_above_title);
		if (titleView != null) {
			if (user.getNotifyProvider().getNotifyDataSets().getCount() > 0) {
				titleView.setVisibility(View.GONE);
			} else {
				titleView.setText(R.string.notify_empty_message);
				titleView.setVisibility(View.VISIBLE);
			}
		}
		
		ImageView imageView = (ImageView)view.findViewById(R.id.secondmenu_above_image);
		if (imageView != null) {
			if (user.getNotifyProvider().getNotifyDataSets().getCount() > 0) {
				imageView.setVisibility(View.GONE);
			} else {
				imageView.setImageResource(R.drawable.emptystate_notifications);
				imageView.setVisibility(View.VISIBLE);
			}
		}
	}
	
	@Override
	public View createBehindView(AccountSecondaryMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (mHelper != null) {
			return mHelper.createSecondaryMenuBehindView(fragment, 
					inflater, container, savedInstanceState);
		}
		
		return super.createBehindView(fragment, 
				inflater, container, savedInstanceState);
	}
	
	@Override
	public void onViewCreated(AccountSecondaryMenuFragment fragment, 
			View view, Bundle savedInstanceState) {
		super.onViewCreated(fragment, view, savedInstanceState);
		
		if (mHelper != null) {
			mHelper.onSecondaryMenuViewCreated(fragment, view, savedInstanceState);
		}
	}
	
}
