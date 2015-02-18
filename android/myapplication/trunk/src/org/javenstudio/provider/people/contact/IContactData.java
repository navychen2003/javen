package org.javenstudio.provider.people.contact;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface IContactData {

	public String getUserId();
	public String getUserName();
	public String getAvatarLocation();
	
	public View.OnClickListener getUserClickListener();
	public Drawable getProviderIcon();
	
}
