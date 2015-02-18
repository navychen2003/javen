package org.javenstudio.provider.activity;

import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IMenuExecutor;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.graphics.DelegatedBitmapDrawable;
import org.javenstudio.cocoka.graphics.TransformDrawable;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;

public abstract class AccountMenuActivity extends ProviderListActivity {
	private static final Logger LOG = Logger.getLogger(AccountMenuActivity.class);

	public abstract AccountApp getAccountApp();
	
	@Override
	protected AccountMenuFragment createMenuFragment() { 
		return new AccountMenuFragment();
	}
	
	@Override
	protected AccountSecondaryMenuFragment createSecondaryMenuFragment() { 
		return new AccountSecondaryMenuFragment();
	}
	
	@Override
	protected void onStart() {
        super.onStart();
        getAccountApp().onActivityStart();
	}
	
	@Override
	protected void onDestroy() {
		getAccountApp().onActivityDestroy();
		super.onDestroy();
	}
	
	@Override
	protected void onRequestFeatures(Bundle savedInstanceState) {
		super.onRequestFeatures(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	@Override
	protected boolean isLockOrientationDisabled(int orientation) { 
		return true; 
	}
	
	@Override
	protected boolean isUnlockOrientationDisabled() { 
		return true; 
	}
	
	private TransformDrawable mBackground = null;
	
	@Override
	public void setContentBackground(Provider p) {
		if (LOG.isDebugEnabled()) LOG.debug("setContentBackground: provider=" + p);
		
		//Provider p = getCurrentProvider();
		Drawable contentbg = null; 
		if (p != null) { 
			contentbg = p.getBackground(this, getScreenWidth(), getScreenHeight());
			Drawable actionbarbg = p.getActionBarBackground(this);
			int colorRes = p.getActionBarTitleColorRes(this);
			if (actionbarbg != null) setActionBarBackground(actionbarbg);
			if (colorRes != 0) setActionBarTitleColor(getResources().getColor(colorRes));
		}
		
		if (contentbg == null) contentbg = getBackgroundDrawable();
		if (contentbg != null) {
			if (contentbg instanceof BitmapDrawable || 
				contentbg instanceof DelegatedBitmapDrawable) {
				initTransformBackground(contentbg);
				postStartBackgroundTransforming(1500);
			} else {
				setActivityBackground(contentbg);
				setContentBackground(null);
			}
		}
	}
	
	@Override
	public TextView setActionBarTitleColor(int color) {
		TextView titleView = super.setActionBarTitleColor(color);
		//if (titleView != null) titleView.setShadowLayer(3.0f, 0, 0, Color.BLACK);
		return titleView;
	}
	
	protected void initTransformBackground(Drawable image) {
		if (image == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("initTransformBackground: image=" + image);
		
		TransformDrawable d = new TransformDrawable(image);
		setActivityBackground(d);
		setContentBackground(null);
		
		TransformDrawable old = mBackground;
		mBackground = d;
		if (old != null)
			old.stopTransforming();
	}
	
	protected void startBackgroundTransforming() {
		TransformDrawable d = mBackground;
		if (d != null) 
			d.startTransforming();
	}
	
	protected void stopBackgroundTransforming() {
		TransformDrawable d = mBackground;
		if (d != null) 
			d.stopTransforming();
	}
	
	protected void postStartBackgroundTransforming(long delayMillis) {
		ResourceHelper.getHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					startBackgroundTransforming();
				}
		    }, delayMillis);
	}
	
	public void showAccountProfile() {
		if (LOG.isDebugEnabled()) LOG.debug("showAccountProfile");
		AccountUser user = getAccountApp().getAccount();
		if (user != null) {
			Provider p = user.getAccountProvider();
			if (p != null) {
				setContentProvider(p);
				showContent();
			}
		}
	}
	
	public void showAccountSpaces() {
		if (LOG.isDebugEnabled()) LOG.debug("showAccountSpaces");
		AccountUser user = getAccountApp().getAccount();
		if (user != null) {
			Provider p = user.getSpacesProvider();
			if (p != null) {
				setContentProvider(p);
				showContent();
			}
		}
	}
	
	private IMenu mMenu = null;
	private IMenuItem mProgressMenuItem = null;
	
	@Override
	public void setContentFragment() { 
		super.setContentFragment();
		
		IMenuOperation mo = getMenuOperation();
		if (mo != null) mo.onUpdateContent(this, mMenu);
	}
	
	@Override
	public IMenu getActivityMenu() { 
		return mMenu;
	}
	
	@Override
	public boolean onOptionsItemSelected(IMenuItem item) {
		IMenuExecutor me = getMenuExeccutor();
		if (me != null) { 
			if (me.onOptionsItemSelected(this, item))
				return true;
		}
		
		IMenuOperation mo = getMenuOperation();
		if (mo != null && mo.onOptionsItemSelected(this, item))
			return true;
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(IMenu menu) {
		getSupportMenuInflater().inflate(getOptionsMenuRes(), menu);
		mMenu = menu;
		
		IMenuOperation mo = getMenuOperation();
		if (mo != null) mo.onCreateOptionsMenu(this, menu);
		
		mProgressMenuItem = menu.findItem(getOptionsMenuProgressItemId());
		mProgressMenuItem.setVisible(false);
		
		if (isProgressRunning()) showProgressView();
		
		return true;
	}
	
	@Override
	public void showProgressView() { 
		super.showProgressView();
		getActivityHelper().showProgressActionView(mProgressMenuItem);
	}
	
	@Override
	public void hideProgressView() { 
		super.hideProgressView();
		getActivityHelper().hideProgressActionView(mProgressMenuItem);
	}
	
	protected int getOptionsMenuRes() { return R.menu.app_menu; }
	protected int getOptionsMenuProgressItemId() { return R.id.app_action_progress; }
	protected Drawable getBackgroundDrawable() { return null; }
	
}
