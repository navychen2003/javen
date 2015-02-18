package org.anybox.android.library.app;

import org.javenstudio.provider.app.anybox.user.AnyboxAccountDetails;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountItem;

public class MyAccountDetailsBrief extends AnyboxAccountDetails.AnyboxDetailsBrief {

	private final MyBinderHelper mHelper = MyBinderHelper.BLUE;
	
	public MyAccountDetailsBrief(AnyboxAccountItem account) { 
		super(account);
	}
	
	@Override
	protected int getCardBackgroundRes() {
		if (mHelper != null) return mHelper.getCardBackgroundRes();
		return super.getCardBackgroundRes();
	}
	
	@Override
	protected int getCardItemBackgroundRes() {
		return super.getCardItemBackgroundRes();
	}
	
}
