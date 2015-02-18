package org.anybox.android.library.app;

import org.javenstudio.provider.app.anybox.user.AnyboxAccountDetails;
import org.javenstudio.provider.app.anybox.user.AnyboxAccountItem;

public class MyAccountDetailsBasic extends AnyboxAccountDetails.AnyboxDetailsBasic {

	private final MyBinderHelper mHelper = MyBinderHelper.BLUE;
	
	public MyAccountDetailsBasic(AnyboxAccountItem account) {
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
