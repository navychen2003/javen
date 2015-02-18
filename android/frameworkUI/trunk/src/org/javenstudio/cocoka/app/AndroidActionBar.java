package org.javenstudio.cocoka.app;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import org.javenstudio.common.util.Logger;

final class AndroidActionBar implements IActionBar {
	private static final Logger LOG = Logger.getLogger(AndroidActionBar.class);
	
	private final Activity mActivity;
	private final ActionBar mActionBar;
	
	AndroidActionBar(Activity activity, ActionBar actionBar) { 
		mActivity = activity;
		mActionBar = actionBar;
	}
	
	@Override
	public Context getThemedContext() {
		return mActionBar != null ? mActionBar.getThemedContext() : 
			mActivity.getApplicationContext();
	}
	
	@Override
	public void setNavigationMode(int mode) {
		if (mActionBar == null) return;
		mActionBar.setNavigationMode(mode);
	}

	@Override
	public void setHomeButtonEnabled(boolean enabled) { 
		if (mActionBar == null) return;
		mActionBar.setHomeButtonEnabled(enabled);
	}
	
	@Override
	public void setDisplayShowHomeEnabled(boolean showHome) { 
		if (mActionBar == null) return;
		mActionBar.setDisplayShowHomeEnabled(showHome);
	}
	
	@Override
	public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
		if (mActionBar == null) return;
		mActionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
	}

	@Override
	public void setDisplayShowTitleEnabled(boolean showTitle) {
		if (mActionBar == null) return;
		mActionBar.setDisplayShowTitleEnabled(showTitle);
	}

	@Override
	public void setDisplayUseLogoEnabled(boolean useLogo) { 
		if (mActionBar == null) return;
		mActionBar.setDisplayUseLogoEnabled(useLogo);
	}
	
	@Override
	public void setDisplayShowCustomEnabled(boolean showCustom) { 
		if (mActionBar == null) return;
		mActionBar.setDisplayShowCustomEnabled(showCustom);
	}
	
	@Override
	public void setListNavigationCallbacks(SpinnerAdapter adapter,
			final OnNavigationListener callback) {
		if (mActionBar == null) return;
		mActionBar.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					return callback.onNavigationItemSelected(itemPosition, itemId);
				}
			});
	}

	private Map<OnMenuVisibilityListener, ActionBar.OnMenuVisibilityListener> mMenuListeners = null;
	
	@Override
	public void addOnMenuVisibilityListener(final OnMenuVisibilityListener listener) { 
		if (listener == null || mActionBar == null) return;
		
		if (mMenuListeners == null) 
			mMenuListeners = new HashMap<OnMenuVisibilityListener, ActionBar.OnMenuVisibilityListener>();
		
		ActionBar.OnMenuVisibilityListener lstr = new ActionBar.OnMenuVisibilityListener() {
				@Override
				public void onMenuVisibilityChanged(boolean isVisible) {
					listener.onMenuVisibilityChanged(isVisible);
				}
			};
		
		mMenuListeners.put(listener, lstr);
		mActionBar.addOnMenuVisibilityListener(lstr);
	}
	
	@Override
	public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) { 
		if (listener == null || mActionBar == null) return;
		if (mMenuListeners == null) return;
		
		ActionBar.OnMenuVisibilityListener lstr = mMenuListeners.get(listener);
		if (lstr != null) 
			mActionBar.removeOnMenuVisibilityListener(lstr);
	}
	
	@Override
	public void setSelectedNavigationItem(int position) {
		if (mActionBar == null) return;
		mActionBar.setSelectedNavigationItem(position);
	}
	
	@Override
	public int getSelectedNavigationIndex() { 
		if (mActionBar == null) return -1;
		return mActionBar.getSelectedNavigationIndex();
	}
	
	@Override
	public int getNavigationItemCount() { 
		if (mActionBar == null) return 0;
		return mActionBar.getNavigationItemCount();
	}
	
	@Override
	public int getNavigationMode() { 
		if (mActionBar == null) return 0;
		return mActionBar.getNavigationMode();
	}
	
	@Override
	public void setTitle(int resId) { 
		if (mActionBar == null) return;
		mActionBar.setTitle(resId);
	}
	
	@Override
	public void setTitle(CharSequence title) { 
		if (mActionBar == null) return;
		mActionBar.setTitle(title);
	}
	
	@Override
	public CharSequence getTitle() { 
		if (mActionBar == null) return null;
		return mActionBar.getTitle();
	}
	
	@Override
	public void setSubtitle(int resId) { 
		if (mActionBar == null) return;
		mActionBar.setSubtitle(resId);
	}
	
	@Override
	public void setSubtitle(CharSequence title) { 
		if (mActionBar == null) return;
		mActionBar.setSubtitle(title);
	}
	
	@Override
	public CharSequence getSubtitle() { 
		if (mActionBar == null) return null;
		return mActionBar.getSubtitle();
	}
	
	@Override
	public int getDisplayOptions() { 
		if (mActionBar == null) return 0;
		return mActionBar.getDisplayOptions();
	}
	
	@Override
	public void setDisplayOptions(int options) { 
		if (mActionBar == null) return;
		mActionBar.setDisplayOptions(options);
	}
	
	@Override
	public void setDisplayOptions(int options, int mask) { 
		if (mActionBar == null) return;
		mActionBar.setDisplayOptions(options, mask);
	}
	
	@Override
	public void setBackgroundDrawable(Drawable d) { 
		if (mActionBar == null) return;
		mActionBar.setBackgroundDrawable(d);
	}
	
	@Override
	public void setStackedBackgroundDrawable(Drawable d) {
		if (mActionBar == null) return;
		mActionBar.setStackedBackgroundDrawable(d);
	}
	
	@Override
	public void setLogo(Drawable logo) { 
		if (mActionBar == null) return;
		mActionBar.setLogo(logo);
	}
	
	@Override
	public void setLogo(int resId) { 
		if (mActionBar == null) return;
		mActionBar.setLogo(resId);
	}
	
	@Override
	public void setIcon(Drawable icon) { 
		if (mActionBar == null) return;
		mActionBar.setIcon(icon);
	}
	
	@Override
	public void setIcon(int resId) { 
		if (mActionBar == null) return;
		mActionBar.setIcon(resId);
	}
	
	@Override
	public void setHomeAsUpIndicator(Drawable indicator) {
		if (mActionBar == null) return;
		if (LOG.isDebugEnabled()) LOG.debug("setHomeAsUpIndicator: drawable=" + indicator);
		try {
			Method method = mActionBar.getClass().getMethod("setHomeAsUpIndicator", Drawable.class);
			method.invoke(mActionBar, indicator);
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("setHomeAsUpIndicator: error: " + e, e);
		}
	}
	
	@Override
	public void setHomeAsUpIndicator(int resId) {
		if (mActionBar == null) return;
		if (LOG.isDebugEnabled()) LOG.debug("setHomeAsUpIndicator: res=" + resId);
		try {
			Method method = mActionBar.getClass().getMethod("setHomeAsUpIndicator", int.class);
			method.invoke(mActionBar, resId);
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("setHomeAsUpIndicator: error: " + e, e);
		}
	}
	
	@Override
	public void setCustomView(View view, ViewGroup.LayoutParams layoutParams) { 
		if (mActionBar == null) return;
		mActionBar.setCustomView(view, (ActionBar.LayoutParams)layoutParams);
	}
	
	@Override
	public void setCustomView(View view) { 
		if (mActionBar == null) return;
		mActionBar.setCustomView(view);
	}
	
	@Override
	public void setCustomView(int resId) { 
		if (mActionBar == null) return;
		mActionBar.setCustomView(resId);
	}
	
	@Override
	public View getCustomView() { 
		if (mActionBar == null) return null;
		return mActionBar.getCustomView();
	}
	
	@Override
	public int getHeight() { 
		if (mActionBar == null) return 0;
		return mActionBar.getHeight();
	}
	
	@Override
	public void show() { 
		if (mActionBar == null) return;
		mActionBar.show();
	}
	
	@Override
	public void hide() { 
		if (mActionBar == null) return;
		mActionBar.hide();
	}

	@Override
	public ITab newTab() {
		if (mActionBar == null) return null;
		return new AndroidTab(mActionBar.newTab());
	}
	
	@Override
	public void addTab(ITab tab) {
		if (mActionBar != null && tab != null && tab instanceof AndroidTab) {
			mActionBar.addTab(((AndroidTab)tab).getTab());
		}
	}
	
	@Override
	public void addTab(ITab tab, int position) {
		if (mActionBar != null && tab != null && tab instanceof AndroidTab) {
			mActionBar.addTab(((AndroidTab)tab).getTab(), position);
		}
	}
	
	@Override
	public void addTab(ITab tab, int position, boolean setSelected) {
		if (mActionBar != null && tab != null && tab instanceof AndroidTab) {
			mActionBar.addTab(((AndroidTab)tab).getTab(), position, setSelected);
		}
	}
	
	@Override
	public void removeTab(ITab tab) {
		if (mActionBar != null && tab != null && tab instanceof AndroidTab) {
			mActionBar.removeTab(((AndroidTab)tab).getTab());
		}
	}
	
	@Override
	public void removeTabAt(int position) {
		if (mActionBar != null) {
			mActionBar.removeTabAt(position);
		}
	}
	
	@Override
	public void removeAllTabs() {
		if (mActionBar != null) {
			mActionBar.removeAllTabs();
		}
	}
	
	@Override
	public void selectTab(ITab tab) {
		if (mActionBar != null && tab != null && tab instanceof AndroidTab) {
			mActionBar.selectTab(((AndroidTab)tab).getTab());
		}
	}
	
	@Override
	public ITab getSelectedTab() {
		if (mActionBar != null) {
			ActionBar.Tab tab = mActionBar.getSelectedTab();
			if (tab != null) return new AndroidTab(tab);
		}
		return null;
	}
	
	@Override
	public boolean hasEmbeddedTabs() {
		if (mActionBar == null) return false;
		try {
			Method method = mActionBar.getClass().getMethod("hasNonEmbeddedTabs");
			return !((Boolean)method.invoke(mActionBar));
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("hasNonEmbeddedTabs: error: " + e, e);
		}
		return false;
	}
	
	@Override
	public IActionMode startActionMode(final IActionMode.Callback callback) {
		if (callback == null) return null;
		
		ActionMode actionMode = mActivity.startActionMode(new ActionMode.Callback() {
				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					return callback.onPrepareActionMode(wrapActionMode(mode), new AndroidMenu(menu));
				}
				
				@Override
				public void onDestroyActionMode(ActionMode mode) {
					callback.onDestroyActionMode(wrapActionMode(mode));
				}
				
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					return callback.onCreateActionMode(wrapActionMode(mode), new AndroidMenu(menu));
				}
				
				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					return callback.onActionItemClicked(wrapActionMode(mode), new AndroidMenuItem(item));
				}
			});
		
		if (LOG.isDebugEnabled())
			LOG.debug("startActionMode: mode=" + actionMode + " callback=" + callback);
		
		return wrapActionMode(actionMode);
	}
	
	private IActionMode wrapActionMode(final ActionMode mode) { 
		if (mode == null) return null;
		
		return new IActionMode() {
			@Override
			public void setTag(Object tag) {
				mode.setTag(tag);
			}

			@Override
			public Object getTag() {
				return mode.getTag();
			}

			@Override
			public void setTitle(CharSequence title) {
				mode.setTitle(title);
			}

			@Override
			public void setTitle(int resId) {
				mode.setTitle(resId);
			}

			@Override
			public void setSubtitle(CharSequence subtitle) {
				mode.setSubtitle(subtitle);
			}

			@Override
			public void setSubtitle(int resId) {
				mode.setSubtitle(resId);
			}

			@Override
			public void setTitleOptionalHint(boolean titleOptional) {
				mode.setTitleOptionalHint(titleOptional);
			}

			@Override
			public boolean getTitleOptionalHint() {
				return mode.getTitleOptionalHint();
			}

			@Override
			public boolean isTitleOptional() {
				return mode.isTitleOptional();
			}

			@Override
			public void setCustomView(View view) {
				mode.setCustomView(view);
			}

			@Override
			public void invalidate() {
				mode.invalidate();
			}

			@Override
			public void finish() {
				mode.finish();
			}

			@Override
			public IMenu getMenu() {
				return new AndroidMenu(mode.getMenu());
			}

			@Override
			public IMenuInflater getMenuInflater() {
				return new AndroidMenuInflater(mode.getMenuInflater());
			}

			@Override
			public CharSequence getTitle() {
				return mode.getTitle();
			}

			@Override
			public CharSequence getSubtitle() {
				return mode.getSubtitle();
			}

			@Override
			public View getCustomView() {
				return mode.getCustomView();
			}

			//@Override
			//public boolean isUiFocusable() {
			//	return mode.isUiFocusable();
			//}
		};
	}
	
}
