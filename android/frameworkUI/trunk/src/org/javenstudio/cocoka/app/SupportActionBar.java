package org.javenstudio.cocoka.app;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.javenstudio.common.util.Logger;

public class SupportActionBar implements IActionBar {
	private static final Logger LOG = Logger.getLogger(SupportActionBar.class);
	
	private final ActionBarSherlock mImpl;
	
	SupportActionBar(Activity activity) { 
		mImpl = ActionBarSherlock.wrap(activity, ActionBarSherlock.FLAG_DELEGATE);
	}

	public ActionBarSherlock getImpl() { 
		return mImpl;
	}
	
	@Override
	public Context getThemedContext() {
		return getImpl().getActionBar().getThemedContext();
	}

	@Override
	public void setNavigationMode(int mode) {
		getImpl().getActionBar().setNavigationMode(mode);
	}

	@Override
	public int getDisplayOptions() { 
		return getImpl().getActionBar().getDisplayOptions();
	}
	
	@Override
	public void setDisplayOptions(int options) { 
		getImpl().getActionBar().setDisplayOptions(options);
	}
	
	@Override
	public void setDisplayOptions(int options, int mask) { 
		getImpl().getActionBar().setDisplayOptions(options, mask);
	}
	
	@Override
	public void setHomeButtonEnabled(boolean enabled) { 
		getImpl().getActionBar().setHomeButtonEnabled(enabled);
	}
	
	@Override
	public void setDisplayShowHomeEnabled(boolean showHome) { 
		getImpl().getActionBar().setDisplayShowHomeEnabled(showHome);
	}
	
	@Override
	public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
		getImpl().getActionBar().setDisplayHomeAsUpEnabled(showHomeAsUp);
	}

	@Override
	public void setDisplayShowTitleEnabled(boolean showTitle) {
		getImpl().getActionBar().setDisplayShowTitleEnabled(showTitle);
	}

	@Override
	public void setDisplayUseLogoEnabled(boolean useLogo) { 
		getImpl().getActionBar().setDisplayUseLogoEnabled(useLogo);
	}
	
	@Override
	public void setDisplayShowCustomEnabled(boolean showCustom) { 
		getImpl().getActionBar().setDisplayShowCustomEnabled(showCustom);
	}
	
	@Override
	public void setListNavigationCallbacks(SpinnerAdapter adapter,
			final OnNavigationListener callback) {
		getImpl().getActionBar().setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					return callback.onNavigationItemSelected(itemPosition, itemId);
				}
			});
	}

	private Map<OnMenuVisibilityListener, ActionBar.OnMenuVisibilityListener> mMenuListeners = null;
	
	@Override
	public void addOnMenuVisibilityListener(final OnMenuVisibilityListener listener) { 
		if (listener == null) return;
		
		if (mMenuListeners == null) 
			mMenuListeners = new HashMap<OnMenuVisibilityListener, ActionBar.OnMenuVisibilityListener>();
		
		ActionBar.OnMenuVisibilityListener lstr = new ActionBar.OnMenuVisibilityListener() {
				@Override
				public void onMenuVisibilityChanged(boolean isVisible) {
					listener.onMenuVisibilityChanged(isVisible);
				}
			};
		
		mMenuListeners.put(listener, lstr);
		getImpl().getActionBar().addOnMenuVisibilityListener(lstr);
	}
	
	@Override
	public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) { 
		if (listener == null) return;
		if (mMenuListeners == null) return;
		
		ActionBar.OnMenuVisibilityListener lstr = mMenuListeners.get(listener);
		if (lstr != null) 
			getImpl().getActionBar().removeOnMenuVisibilityListener(lstr);
	}
	
	@Override
	public void setSelectedNavigationItem(int position) {
		getImpl().getActionBar().setSelectedNavigationItem(position);
	}
	
	@Override
	public int getSelectedNavigationIndex() { 
		return getImpl().getActionBar().getSelectedNavigationIndex();
	}
	
	@Override
	public int getNavigationItemCount() { 
		return getImpl().getActionBar().getNavigationItemCount();
	}
	
	@Override
	public int getNavigationMode() { 
		return getImpl().getActionBar().getNavigationMode();
	}
	
	@Override
	public void setTitle(int resId) { 
		getImpl().getActionBar().setTitle(resId);
	}
	
	@Override
	public void setTitle(CharSequence title) { 
		getImpl().getActionBar().setTitle(title);
	}
	
	@Override
	public CharSequence getTitle() { 
		return getImpl().getActionBar().getTitle();
	}
	
	@Override
	public void setSubtitle(int resId) { 
		getImpl().getActionBar().setSubtitle(resId);
	}
	
	@Override
	public void setSubtitle(CharSequence title) { 
		getImpl().getActionBar().setSubtitle(title);
	}
	
	@Override
	public CharSequence getSubtitle() { 
		return getImpl().getActionBar().getSubtitle();
	}
	
	@Override
	public void setBackgroundDrawable(Drawable d) { 
		getImpl().getActionBar().setBackgroundDrawable(d);
	}
	
	@Override
	public void setStackedBackgroundDrawable(Drawable d) {
		getImpl().getActionBar().setStackedBackgroundDrawable(d);
	}
	
	@Override
	public void setLogo(Drawable logo) { 
		getImpl().getActionBar().setLogo(logo);
	}
	
	@Override
	public void setLogo(int resId) { 
		getImpl().getActionBar().setLogo(resId);
	}
	
	@Override
	public void setIcon(Drawable icon) { 
		getImpl().getActionBar().setIcon(icon);
	}
	
	@Override
	public void setIcon(int resId) { 
		getImpl().getActionBar().setIcon(resId);
	}
	
	@Override
	public void setHomeAsUpIndicator(Drawable indicator) {
		if (LOG.isDebugEnabled()) LOG.debug("setHomeAsUpIndicator: drawable=" + indicator);
		getImpl().getActionBar().setHomeAsUpIndicator(indicator);
	}
	
	@Override
	public void setHomeAsUpIndicator(int resId) {
		if (LOG.isDebugEnabled()) LOG.debug("setHomeAsUpIndicator: res=" + resId);
		getImpl().getActionBar().setHomeAsUpIndicator(resId);
	}
	
	@Override
	public void setCustomView(View view, ViewGroup.LayoutParams layoutParams) { 
		getImpl().getActionBar().setCustomView(view, (ActionBar.LayoutParams)layoutParams);
	}
	
	@Override
	public void setCustomView(View view) { 
		getImpl().getActionBar().setCustomView(view);
	}
	
	@Override
	public void setCustomView(int resId) { 
		getImpl().getActionBar().setCustomView(resId);
	}
	
	@Override
	public View getCustomView() { 
		return getImpl().getActionBar().getCustomView();
	}
	
	@Override
	public int getHeight() { 
		return getImpl().getActionBar().getHeight();
	}
	
	@Override
	public void show() { 
		getImpl().getActionBar().show();
	}
	
	@Override
	public void hide() { 
		getImpl().getActionBar().hide();
	}

	@Override
	public ITab newTab() {
		return new SupportTab(getImpl().getActionBar().newTab());
	}
	
	@Override
	public void addTab(ITab tab) {
		if (tab != null && tab instanceof SupportTab) {
			getImpl().getActionBar().addTab(((SupportTab)tab).getTab());
		}
	}
	
	@Override
	public void addTab(ITab tab, int position) {
		if (tab != null && tab instanceof SupportTab) {
			getImpl().getActionBar().addTab(((SupportTab)tab).getTab(), position);
		}
	}
	
	@Override
	public void addTab(ITab tab, int position, boolean setSelected) {
		if (tab != null && tab instanceof SupportTab) {
			getImpl().getActionBar().addTab(((SupportTab)tab).getTab(), position, setSelected);
		}
	}
	
	@Override
	public void removeTab(ITab tab) {
		if (tab != null && tab instanceof SupportTab) {
			getImpl().getActionBar().removeTab(((SupportTab)tab).getTab());
		}
	}
	
	@Override
	public void removeTabAt(int position) {
		getImpl().getActionBar().removeTabAt(position);
	}
	
	@Override
	public void removeAllTabs() {
		getImpl().getActionBar().removeAllTabs();
	}
	
	@Override
	public void selectTab(ITab tab) {
		if (tab != null && tab instanceof SupportTab) {
			getImpl().getActionBar().selectTab(((SupportTab)tab).getTab());
		}
	}
	
	@Override
	public ITab getSelectedTab() {
		ActionBar.Tab tab = getImpl().getActionBar().getSelectedTab();
		if (tab != null) return new SupportTab(tab);
		return null;
	}
	
	@Override
	public boolean hasEmbeddedTabs() {
		return getImpl().getActionBar().hasEmbeddedTabs();
	}
	
	@Override
	public IActionMode startActionMode(final IActionMode.Callback callback) {
		if (callback == null) return null;
		
		ActionMode actionMode = getImpl().startActionMode(new ActionMode.Callback() {
				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					return callback.onPrepareActionMode(wrapActionMode(mode), new SupportMenuImpl(menu));
				}
				
				@Override
				public void onDestroyActionMode(ActionMode mode) {
					callback.onDestroyActionMode(wrapActionMode(mode));
				}
				
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					return callback.onCreateActionMode(wrapActionMode(mode), new SupportMenuImpl(menu));
				}
				
				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					return callback.onActionItemClicked(wrapActionMode(mode), new SupportMenuItemImpl(item));
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
			}

			@Override
			public boolean getTitleOptionalHint() {
				return false;
			}

			@Override
			public boolean isTitleOptional() {
				return false;
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
				return new SupportMenuImpl(mode.getMenu());
			}

			@Override
			public IMenuInflater getMenuInflater() {
				return new SupportMenuInflaterImpl(mode.getMenuInflater());
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
		};
	}
	
}
