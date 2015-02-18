package org.javenstudio.provider.publish.discuss;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface IReplyData {

	public String getReplyId();
	public String getReplySubject();
	public String getMessage();
	
	public String getUserId();
	public String getUserName();
	public String getAvatarLocation();
	
	public long getCreateDate();
	
	public View.OnClickListener getUserClickListener();
	public Drawable getProviderIcon();
	
}
