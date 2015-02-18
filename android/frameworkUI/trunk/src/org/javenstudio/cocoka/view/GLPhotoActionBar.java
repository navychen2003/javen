package org.javenstudio.cocoka.view;

import android.app.Activity;
import android.content.Intent;

import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.app.ShareMenuItem;
import org.javenstudio.cocoka.opengl.GLActionBar;

public class GLPhotoActionBar extends GLActionBar {

    private ShareMenuItem mMenuItem = null;
    private Intent mShareIntent = null;
	
	public GLPhotoActionBar(GLPhotoActivity activity) {
		super(activity);
	}
	
	@Override
    protected void onActionBarMenuCreated(IMenu menu) {
        super.onActionBarMenuCreated(menu);
        
        IMenuItem shareItem = menu.findItem(R.id.photo_action_share);
        if (shareItem != null) {
        	int iconRes = BaseResources.getInstance().getDrawableRes(BaseResources.drawable.icon_menu_share);
        	if (iconRes != 0) shareItem.setIcon(iconRes);
        }
        
        IMenuItem downloadItem = menu.findItem(R.id.photo_action_download);
        if (downloadItem != null) {
        	int iconRes = BaseResources.getInstance().getDrawableRes(BaseResources.drawable.icon_menu_download);
        	if (iconRes != 0) downloadItem.setIcon(iconRes);
        }
        
        IMenuItem sharetoItem = menu.findItem(R.id.photo_action_shareto);
        if (sharetoItem != null) {
            mMenuItem = new ShareMenuItem(sharetoItem);
            mMenuItem.setShareIntent(mShareIntent);
        }
    }
	
    public void setShareIntent(Intent shareIntent) {
        mShareIntent = shareIntent;
        if (mMenuItem != null) 
        	mMenuItem.setShareIntent(shareIntent);
    }
	
	public void onActionShareTo(Activity activity) { 
		Intent intent = mShareIntent;
		
		if (activity != null && intent != null) { 
			activity.startActivity(Intent.createChooser(intent, 
					activity.getString(R.string.label_action_share_photo)));
		}
	}
	
}
