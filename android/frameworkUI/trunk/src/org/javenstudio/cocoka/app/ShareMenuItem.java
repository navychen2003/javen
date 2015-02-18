package org.javenstudio.cocoka.app;

import android.content.Intent;

public class ShareMenuItem {

	private final IMenuItem mMenuItem;
	private final IShareActionProvider mShareActionProvider;
    private Intent mShareIntent = null;
	
    public ShareMenuItem(IMenuItem menuItem) { 
    	if (menuItem == null) throw new NullPointerException();
    	mMenuItem = menuItem;
    	
    	IActionProvider p = menuItem.getActionProvider();
    	if (p != null && p instanceof IShareActionProvider) { 
	    	mShareActionProvider = (IShareActionProvider)p;
	        mShareActionProvider.setShareHistoryFileName("share_history.xml");
	        mShareActionProvider.setShareIntent(mShareIntent);
    	} else { 
    		mShareActionProvider = null;
    	}
    }
    
    public final IMenuItem getMenuItem() { return mMenuItem; }
    public final Intent getShareIntent() { return mShareIntent; }
    
    public void setShareIntent(Intent shareIntent) {
        mShareIntent = shareIntent;
        if (mShareActionProvider != null) 
            mShareActionProvider.setShareIntent(shareIntent);
        
        getMenuItem().setVisible(shareIntent != null);
    }
    
}
