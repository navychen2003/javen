package org.javenstudio.provider.app.flickr;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;
import org.javenstudio.provider.people.contact.IContactData;

final class YContactEntry extends BaseEntry implements IContactData {

	private final FlickrUserClickListener mUserClickListener;
	private final int mIconRes;
	
	public String userId;
	public String username;
	
	public String iconserver;
	public String iconfarm;
	
	public String ignored;
	
	public YContactEntry(FlickrUserClickListener listener, int iconRes) { 
		mUserClickListener = listener;
		mIconRes = iconRes;
	}
	
	public String getUserId() { return userId; }
	public String getUserName() { return username; }
	
	@Override
	public String getAvatarLocation() { 
		return FlickrHelper.getIconURL(userId, iconfarm, iconserver); 
	}
	
	@Override
	public Drawable getProviderIcon() { 
		if (mIconRes != 0) 
			return ResourceHelper.getResources().getDrawable(mIconRes);
		return null;
	}
	
	@Override
	public View.OnClickListener getUserClickListener() { 
		if (mUserClickListener == null)
			return null;
		
		return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mUserClickListener.onFlickrUserClick(userId, username);
				}
			};
	}
	
	static YContactEntry parseEntry(Node node, FlickrUserClickListener listener, int iconRes) { 
		YContactEntry entry = new YContactEntry(listener, iconRes);
		
		entry.userId = node.getAttribute("nsid");
		entry.username = node.getAttribute("username");
		
		entry.iconserver = node.getAttribute("iconserver");
		entry.iconfarm = node.getAttribute("iconfarm");
		
		entry.ignored = node.getAttribute("ignored");
		
		return entry;
	}
	
}
