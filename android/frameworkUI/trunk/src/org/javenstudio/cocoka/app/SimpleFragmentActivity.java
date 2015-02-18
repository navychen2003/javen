package org.javenstudio.cocoka.app;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.javenstudio.cocoka.slidingmenu.SlidingMenu;
import org.javenstudio.common.util.Logger;

public abstract class SimpleFragmentActivity extends BaseFragmentActivity {
	private static final Logger LOG = Logger.getLogger(SimpleFragmentActivity.class);
	
	public static final boolean DEBUG = SlidingMenu.DEBUG;
	
	//private final GestureDetector.OnGestureListener mGestureListener = 
	//		new SlidingGestureListener();
		
	private GestureDetector mGestureDetector;
	
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
		
		mGestureDetector = onCreateGestureDetector();
		
	    doOnCreate(savedInstanceState);
	    doOnCreateDone(savedInstanceState);
	}
	
	protected void onRequestFeatures(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
	}
	
	protected GestureDetector onCreateGestureDetector() {
		return new GestureDetector(this, new SlidingGestureListener() { 
				@Override
				protected boolean onFlingToRight() { 
					return SimpleFragmentActivity.this.onFlingToRight(); 
				}
			});
	}
	
	protected void doOnCreate(Bundle savedInstanceState) { 
		// set the Above View
		setContentView(R.layout.slidingmenu_content);
		//setContentFragment(createContentFragment(0));
		
		IActionBar actionBar = getSupportActionBar(); 
		if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
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
		//getSlidingMenu().setContentBackground(background);
		View view = getContentView();
		if (view != null) view.setBackground(background);
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
	
	public boolean onActionHome() { return false; }
	public boolean onActionRefresh() { return false; }
	
	@Override    
	public boolean dispatchTouchEvent(MotionEvent event) {
		GestureDetector detector = mGestureDetector;
		if (detector != null && detector.onTouchEvent(event))
			return true;
		
		if (super.dispatchTouchEvent(event)) 
			return true;
		
		return false;
	}
	
	protected final void setContentFragment(Fragment fragment) { 
		if (fragment == null || !canCommitFragment()) return;
		if (LOG.isDebugEnabled()) LOG.debug("setContentFragment: fragment=" + fragment);
		
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.slidingmenu_content, fragment)
			.commitAllowingStateLoss();
	}
	
	//protected abstract Fragment createContentFragment();
	protected boolean onFlingToRight() { return false; }
	
}
