package org.javenstudio.provider.people.group;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface IGroupData {

	public String getGroupId();
	public String getGroupName();
	
	public String getTitle();
	public String getSubTitle();
	
	public int getMemberCount();
	public int getTopicCount();
	public int getPhotoCount();
	
	public String getAvatarLocation();
	
	public View.OnClickListener getGroupClickListener();
	public Drawable getProviderIcon();
	
}
