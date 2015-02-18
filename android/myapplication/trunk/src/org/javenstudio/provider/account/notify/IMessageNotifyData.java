package org.javenstudio.provider.account.notify;

import android.graphics.drawable.Drawable;

public interface IMessageNotifyData {

	public static interface IMessageItem {
		public String getMessageTitle();
		public String getMessageBody();
		
		public long getMessageTime();
		public Drawable getAvatarDrawable(int size, int padding);
	}
	
	public IMessageItem[] getItems();
	public int getTotalCount();
	
}
