package org.javenstudio.cocoka.app;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.javenstudio.cocoka.slidingmenu.SlidingMenu;
import org.javenstudio.cocoka.slidingmenu.SlidingFragmentActivity;
import org.javenstudio.cocoka.slidingmenu.SlidingMenuTransformer;
import org.javenstudio.common.util.Logger;

public abstract class SlidingMenuActivity extends SlidingFragmentActivity 
		implements SlidingMenuFragment.ISlidingMenuActivity, 
			SlidingSecondaryMenuFragment.ISlidingSecondaryMenuActivity {
	private static final Logger LOG = Logger.getLogger(SlidingMenuActivity.class);

	public static final boolean DEBUG = SlidingMenu.DEBUG;
	
	public interface OnMenuVisibilityChangeListener {
		public void onMenuVisibilityChanged(int visibility);
	}
	
	public interface OnSecondaryMenuVisibilityChangeListener {
		public void onSecondaryMenuVisibilityChanged(int visibility);
	}
	
	public interface OnMenuScrolledListener {
		public void onMenuScrolled(float percentOpen);
	}
	
	public interface OnSecondaryMenuScrolledListener {
		public void onSecondaryMenuScrolled(float percentOpen);
	}
	
	private OnMenuVisibilityChangeListener mMenuListener;
	private OnSecondaryMenuVisibilityChangeListener mSecondaryListener;
	
	private OnMenuScrolledListener mOnMenuScrolledListener;
	private OnSecondaryMenuScrolledListener mOnSecondaryMenuScrolledListener;
	
	public void setOnMenuScrolledListener(OnMenuScrolledListener listener) {
		mOnMenuScrolledListener = listener;
	}
	public OnMenuScrolledListener getOnMenuScrolledListener() {
		return mOnMenuScrolledListener;
	}
	
	public void setOnSecondaryMenuScrolledListener(OnSecondaryMenuScrolledListener listener) {
		mOnSecondaryMenuScrolledListener = listener;
	}
	public OnSecondaryMenuScrolledListener getOnSecondaryMenuScrolledListener() {
		return mOnSecondaryMenuScrolledListener;
	}
	
	public void setOnMenuVisibilityChangeListener(OnMenuVisibilityChangeListener listener) {
		mMenuListener = listener;
	}
	public OnMenuVisibilityChangeListener getOnMenuVisibilityChangeListener() {
		return mMenuListener;
	}
	
	public void setOnSecondaryMenuVisibilityChangeListener(OnSecondaryMenuVisibilityChangeListener listener) {
		mSecondaryListener = listener;
	}
	public OnSecondaryMenuVisibilityChangeListener getOnSecondaryMenuVisibilityChangeListener() {
		return mSecondaryListener;
	}
	
	public abstract ActionItem[] getNavigationItems();
	public abstract ActionItem[] getSecondaryNavigationItems();
	
	public abstract SupportActionBar getSupportActionBarOrNull();
	public abstract IActionBar getSupportActionBar();
	
	public IActionMode startActionMode(IActionMode.Callback callback) { 
		return getSupportActionBar().startActionMode(callback);
	}
	
	public int getScreenWidth() { 
		return getResources().getDisplayMetrics().widthPixels; 
	}
	
	public int getScreenHeight() { 
		return getResources().getDisplayMetrics().heightPixels; 
	}
	
	@Override
	protected final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onRequestFeatures(savedInstanceState);

        doOnCreate(savedInstanceState);
        doOnCreateDone(savedInstanceState);
	}
	
	protected void onRequestFeatures(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
	}
	
	protected void doOnCreate(Bundle savedInstanceState) { 
		// set the Menu View
		setMenuContentView(R.layout.slidingmenu_menu);
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.slidingmenu_menu, createMenuFragment())
			.commit();

		setSecondaryMenuContentView(R.layout.slidingmenu_second);
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.slidingmenu_second, createSecondaryMenuFragment())
			.commit();
		
		// set the Content View
		setContentView(R.layout.slidingmenu_content);
		
		// customize the SlidingMenu
		SlidingMenu sm = getSlidingMenu();
		sm.setMenuShadowWidthRes(R.dimen.slidingmenu_shadow_width);
		sm.setMenuShadowDrawable(R.drawable.slidingmenu_shadow);
		sm.setSecondaryMenuShadowDrawable(R.drawable.slidingmenu_shadowsecondary);
		sm.setMenuOffsetRes(R.dimen.slidingmenu_offset);
		sm.setMenuFadeDegree(0.35f);
		sm.setMenuFadeEnabled(true);
		sm.setTouchModeContent(SlidingMenu.TOUCHMODE_FULLSCREEN);
		sm.setMode(SlidingMenu.LEFT_RIGHT);
		sm.setMenuScrollScale(0.0f);
		
		SlidingMenu.CanvasTransformer transformer = createCanvasTransformer();
		sm.setMenuCanvasTransformer(transformer);
		sm.setContentCanvasTransformer(transformer);
		
		sm.setOnScrolledListener(new SlidingMenu.OnScrolledListener() {
				@Override
				public void onMenuScrolled(int page, float percentOpen) {
					//if (LOG.isDebugEnabled()) 
					//	LOG.debug("onMenuScrolled: page=" + page + " percentOpen=" + percentOpen);
					if (page == 1) {
						OnMenuScrolledListener listener = mOnMenuScrolledListener;
						if (listener != null) listener.onMenuScrolled(percentOpen);
					} else if (page == 2) {
						OnSecondaryMenuScrolledListener listener = mOnSecondaryMenuScrolledListener;
						if (listener != null) listener.onSecondaryMenuScrolled(percentOpen);
					}
				}
			});
		
		sm.setOnMenuVisibilityChangeListener(new SlidingMenu.OnMenuVisibilityChangeListener() {
				@Override
				public void onSecondaryMenuVisibilityChanged(int visibility) {
					if (LOG.isDebugEnabled()) LOG.debug("onSecondaryMenuVisibilityChanged: visibility=" + visibility);
					OnSecondaryMenuVisibilityChangeListener listener = mSecondaryListener;
					if (listener != null) listener.onSecondaryMenuVisibilityChanged(visibility);
					SlidingMenuActivity.this.onSlidingSecondaryMenuVisibilityChanged(visibility);
				}
				@Override
				public void onMenuVisibilityChanged(int visibility) {
					if (LOG.isDebugEnabled()) LOG.debug("onMenuVisibilityChanged: visibility=" + visibility);
					OnMenuVisibilityChangeListener listener = mMenuListener;
					if (listener != null) listener.onMenuVisibilityChanged(visibility);
					SlidingMenuActivity.this.onSlidingMenuVisibilityChanged(visibility);
				}
			});
		
		//setSlidingActionBarEnabled(true);
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
	}
    
	protected void onSlidingMenuVisibilityChanged(int visibility) {}
	protected void onSlidingSecondaryMenuVisibilityChanged(int visibility) {}
	
	public View getContentView() { 
		//return getWindow().getDecorView(); 
		return findViewById(R.id.slidingmenu_content_container);
	}
	
	public View getContentAboveView() { 
		return findViewById(R.id.slidingmenu_content_above);
	}
	
	public View getContentBehindView() { 
		return findViewById(R.id.slidingmenu_content_behind);
	}
	
	public void setContentBackgroundResource(int resid) { 
		View view = getContentBehindView();
		if (view != null) {
			view.setBackgroundResource(resid);
			view.setVisibility(View.VISIBLE);
		}
	}
	
	public void setContentBackgroundColor(int color) { 
		View view = getContentBehindView(); 
		if (view != null) {
			view.setBackgroundColor(color);
			view.setVisibility(View.VISIBLE);
		}
	}
	
	public void setContentBackground(Drawable background) { 
		View view = getContentBehindView(); 
		if (view != null) { 
			view.setBackground(background);
			view.setVisibility(View.VISIBLE);
		}
	}
	
	public void setActionBarBackgroundResource(int resid) { 
		setActionBarBackground(getResources().getDrawable(resid));
	}
	
	public void setActionBarBackgroundColor(int color) { 
		setActionBarBackground(new ColorDrawable(color));
	}
	
	public void setActionBarBackground(Drawable background) { 
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setBackgroundDrawable(background);
	}
	
	public void setActionBarStackedBackgroundResource(int resid) { 
		setActionBarStackedBackground(getResources().getDrawable(resid));
	}
	
	public void setActionBarStackedBackgroundColor(int color) { 
		setActionBarStackedBackground(new ColorDrawable(color));
	}
	
	public void setActionBarStackedBackground(Drawable background) { 
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setStackedBackgroundDrawable(background);
	}
	
	public void setActivityBackgroundResource(int resid) { 
		setActivityBackground(getResources().getDrawable(resid));
	}
	
	public void setActivityBackgroundColor(int color) { 
		setActivityBackground(new ColorDrawable(color));
	}
	
	public void setActivityBackground(Drawable background) { 
		SlidingMenu menu = getSlidingMenu(); 
		if (menu != null) menu.setContentBackground(background);
	}
	
	public void setActionBarIcon(Drawable icon) { 
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setIcon(icon);
	}
	
	public void setActionBarIcon(int iconRes) { 
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setIcon(iconRes);
	}
	
	public void setHomeAsUpIndicator(Drawable indicator) { 
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setHomeAsUpIndicator(indicator);
	}
	
	public void setHomeAsUpIndicator(int resId) { 
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setHomeAsUpIndicator(resId);
	}
	
	public TextView setActionBarTitleColor(int color) {
		TextView textView = findTextView("action_bar_title", 
				org.javenstudio.cocoka.app.R.id.abs__action_bar_title);
		if (textView != null) {
			//textView.setShadowLayer(3.0f, 0, 0, Color.BLACK);
			textView.setTextColor(color);
			return textView;
		}
		return null;
	}
	
	public TextView setActionBarSubtitleColor(int color) {
		TextView textView = findTextView("action_bar_subtitle", 
				org.javenstudio.cocoka.app.R.id.abs__action_bar_subtitle);
		if (textView != null) {
			//textView.setShadowLayer(3.0f, 0, 0, Color.BLACK);
			textView.setTextColor(color);
			return textView;
		}
		return null;
	}
	
	public void setActionModeBackground(Drawable background) {
		View view = findActionModeView();
		if (view != null) view.setBackground(background);
	}
	
	public void setActionModeBackgroundResource(int res) {
		View view = findActionModeView();
		if (view != null) view.setBackgroundResource(res);
	}
	
	public void setActionModeBackgroundColor(int color) {
		View view = findActionModeView();
		if (view != null) view.setBackgroundColor(color);
	}
	
	public View findActionModeView() {
		return findViewByName("action_context_bar");
	}
	
	public void setActionModeCloseButtonBackground(Drawable background) {
		View view = findActionModeCloseButtonView();
		if (view != null) view.setBackground(background);
	}
	
	public void setActionModeCloseButtonBackgroundResource(int res) {
		View view = findActionModeCloseButtonView();
		if (view != null) view.setBackgroundResource(res);
	}
	
	public void setActionModeCloseButtonBackgroundColor(int color) {
		View view = findActionModeCloseButtonView();
		if (view != null) view.setBackgroundColor(color);
	}
	
	public View findActionModeCloseButtonView() {
		return findViewByName("action_mode_close_button");
	}
	
	protected void doOnCreateDone(Bundle savedInstanceState) { 
	}
	
	protected Fragment createMenuFragment() { 
		return new SlidingMenuFragment();
	}
	
	protected Fragment createSecondaryMenuFragment() { 
		return new SlidingSecondaryMenuFragment();
	}
	
	protected SlidingMenu.CanvasTransformer createCanvasTransformer() { 
		return new SlidingMenuTransformer();
	}
	
	protected final void setContentFragment(Fragment fragment) { 
		if (fragment == null || !canCommitFragment()) return;
		if (LOG.isDebugEnabled()) LOG.debug("setContentFragment: fragment=" + fragment);
		
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.slidingmenu_content, fragment)
			.commitAllowingStateLoss();
	}
	
	@Override
	public boolean onOptionsItemSelected(IMenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (onActionHome())
				return true;
			else
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	public boolean isSlidingMenuShowing() {
		return getSlidingMenu().isMenuShowing();
	}
	
	public void showSlidingMenu() { 
		showSlidingMenu(true);
	}
	
	public void showSlidingMenu(boolean animate) { 
		getSlidingMenu().showMenu(animate);
	}
	
	public void showSlidingContent() { 
		showSlidingContent(true);
	}
	
	public void showSlidingContent(boolean animate) { 
		getSlidingMenu().showContent(animate);
	}
	
	protected boolean onActionHome() { 
		toggle();
		return true;
	}
	
	protected boolean onActionRefresh() { 
		return false;
	}
	
}
