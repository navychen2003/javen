package org.javenstudio.cocoka.app;

import org.javenstudio.common.util.Logger;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.View;
import android.widget.ShareActionProvider;

final class AndroidMenuItem implements IMenuItem {
	private final Logger LOG = Logger.getLogger(AndroidMenuItem.class);

	protected final android.view.MenuItem mItem;
	
	public AndroidMenuItem(android.view.MenuItem menuItem) { 
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
		try {
			mItem.setTitle(title);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
	}
	
	@Override
	public void setTitle(int title) {
		try {
			mItem.setTitle(title);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
	}
	
	@Override
	public CharSequence getTitle() {
		try {
			return mItem.getTitle();
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
		return null;
	}
	
	@Override
	public void setActionView(View view) {
		try {
			mItem.setActionView(view);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
	}

	@Override
	public void setActionView(int resId) {
		try {
			mItem.setActionView(resId);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
	}

	@Override
	public View getActionView() {
		try {
			return mItem.getActionView();
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
		return null;
	}

	@Override
	public void setIcon(Drawable icon) {
		try {
			mItem.setIcon(icon);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
	}
	
	@Override
	public void setIcon(int iconRes) {
		try {
			mItem.setIcon(iconRes);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
	}
	
	@Override
	public Drawable getIcon() {
		try {
			return mItem.getIcon();
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
		return null;
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
		try {
			final ActionProvider p = mItem.getActionProvider();
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
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn(e.toString(), e);
		}
		return null;
	}
	
}
