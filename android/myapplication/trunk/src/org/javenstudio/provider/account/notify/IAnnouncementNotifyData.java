package org.javenstudio.provider.account.notify;

import android.graphics.drawable.Drawable;

public interface IAnnouncementNotifyData {

	public static interface IAnnouncementItem {
		public String getAnnouncementTitle();
		public String getAnnouncementBody();
		
		public long getPublishTime();
		public Drawable getPosterDrawable(int size, int padding);
	}
	
	public IAnnouncementItem[] getItems();
	public int getTotalCount();
	
}
