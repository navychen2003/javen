package org.javenstudio.provider.publish.discuss;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface ITopicData {

	public String getTopicId();
	public String getTopicSubject();
	public String getMessage();
	
	public String getUserId();
	public String getUserName();
	public String getAvatarLocation();
	
	public int getReplyCount();
	
	public long getCreateDate();
	public long getUpdateDate();
	
	public View.OnClickListener getUserClickListener();
	public Drawable getProviderIcon();
	
}
