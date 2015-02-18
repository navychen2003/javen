package org.javenstudio.cocoka.opengl;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.cocoka.app.BaseActivityHelper;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.SupportActionBar;

public abstract class GLActivity extends GLActivityBase {

	//private class GLActivityHelper extends BaseActivityHelper { 
	//	@Override
	//	public Activity getActivity() { 
	//		return GLActivity.this;
	//	}
	//}
	
	//private final GLActivityHelper mHelper = new GLActivityHelper();
	
	public abstract BaseActivityHelper getActivityHelper();
	
	@Override
	public final SupportActionBar getSupportActionBarOrNull() { 
		return getActivityHelper().getSupportActionBarOrNull();
	}
	
	public final IActionBar getSupportActionBar() { 
		return getActivityHelper().getActionBarAdapter();
	}
	
	@Override
    protected GLActionBar createGLActionBar() { 
    	return new GLActionBar(this);
    }
	
	@Override
	protected GLStateManager createGLStateManager() { 
		return new GLStateManager(this);
	}
	
	public void setActionBarBackgroundResource(int resid) { 
		setActionBarBackground(getResources().getDrawable(resid));
	}
	
	public void setActionBarBackgroundColor(int color) { 
		setActionBarBackground(new ColorDrawable(color));
	}
	
	public void setActionBarBackground(Drawable background) { 
		getSupportActionBar().setBackgroundDrawable(background);
	}
	
	public void setActionBarStackedBackgroundResource(int resid) { 
		setActionBarStackedBackground(getResources().getDrawable(resid));
	}
	
	public void setActionBarStackedBackgroundColor(int color) { 
		setActionBarStackedBackground(new ColorDrawable(color));
	}
	
	public void setActionBarStackedBackground(Drawable background) { 
		getSupportActionBar().setStackedBackgroundDrawable(background);
	}
	
	public void setActivityBackgroundResource(int resid) { 
		setActivityBackground(getResources().getDrawable(resid));
	}
	
	public void setActivityBackgroundColor(int color) { 
		setActivityBackground(new ColorDrawable(color));
	}
	
	public void setActivityBackground(Drawable background) { 
		//getSlidingMenu().setContentBackground(background);
		//getContentView().setBackground(background);
	}
	
	public void setActionBarIcon(Drawable icon) { 
		getSupportActionBar().setIcon(icon);
	}
	
	public void setActionBarIcon(int iconRes) { 
		getSupportActionBar().setIcon(iconRes);
	}
	
	public void setHomeAsUpIndicator(Drawable indicator) { 
		getSupportActionBar().setHomeAsUpIndicator(indicator);
	}
	
	public void setHomeAsUpIndicator(int resId) { 
		getSupportActionBar().setHomeAsUpIndicator(resId);
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
	
	public void postShowProgress(boolean force) {}
	public void postHideProgress(boolean force) {}
	
	//protected void onExceptionCatched(Throwable e) {}
	
	public void startChooser(Intent intent, CharSequence title) {
		if (intent == null) return;
		startActivity(Intent.createChooser(intent, title));
	}
	
}
