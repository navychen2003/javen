package org.javenstudio.cocoka.app;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.actionbarsherlock.widget.ShareActionProvider;

final class SupportMenuItemImpl implements IMenuItem {

	protected final SupportMenuItem mItem;
	
	public SupportMenuItemImpl(SupportMenuItem menuItem) { 
		mItem = menuItem;
	}

	@Override
	public int getItemId() {
		return mItem.getItemId();
	}
	
	@Override
	public int getGroupId() {
		return mItem.getGroupId();
	}
	
	@Override
	public int getOrder() {
		return mItem.getOrder();
	}

	@Override
	public void setTitle(CharSequence title) {
		mItem.setTitle(title);
	}
	
	@Override
	public void setTitle(int title) {
		mItem.setTitle(title);
	}
	
	@Override
	public CharSequence getTitle() {
		return mItem.getTitle();
	}
	
	@Override
	public void setActionView(View view) {
		mItem.setActionView(view);
	}

	@Override
	public void setActionView(int resId) {
		mItem.setActionView(resId);
	}

	@Override
	public View getActionView() {
		return mItem.getActionView();
	}

	@Override
	public void setIcon(Drawable icon) {
		mItem.setIcon(icon);
	}
	
	@Override
	public void setIcon(int iconRes) {
		mItem.setIcon(iconRes);
	}
	
	@Override
	public Drawable getIcon() {
		return mItem.getIcon();
	}
	
	@Override
	public void setVisible(boolean visible) {
		mItem.setVisible(visible);
	}

	@Override
	public boolean isVisible() {
		return mItem.isVisible();
	}

	@Override
	public void setShowAsAction(int actionEnum) {
		mItem.setShowAsAction(actionEnum);
	}
	
	@Override
	public IActionProvider getActionProvider() {
		SupportActionProvider p = mItem.getActionProvider();
		if (p == null) return null;
		
		if (p instanceof ShareActionProvider) { 
			final ShareActionProvider sharep = (ShareActionProvider)p;
			
			return new IShareActionProvider() {
				@Override
				public void setShareHistoryFileName(String shareHistoryFile) {
					sharep.setShareHistoryFileName(shareHistoryFile);
				}
				@Override
				public void setShareIntent(Intent shareIntent) {
					sharep.setShareIntent(shareIntent);
				}
			};
		}
		
		return null;
	}
	
}
