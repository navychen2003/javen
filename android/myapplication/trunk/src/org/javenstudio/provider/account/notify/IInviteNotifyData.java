package org.javenstudio.provider.account.notify;

import android.graphics.drawable.Drawable;

public interface IInviteNotifyData {

	public static interface IInviteItem {
		public String getInviteType();
		public String getInviteTitle();
		public String getInviteMessage();
		
		public long getInviteTime();
		public Drawable getAvatarDrawable(int size, int padding);
	}
	
	public IInviteItem[] getItems();
	public int getTotalCount();
	
}
