package org.javenstudio.provider.activity;

import android.os.Bundle;

import org.javenstudio.android.app.IMenuExecutor;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.provider.ProviderController;

public abstract class AccountAppActivity extends ProviderActivity {

	private ProviderController mController = null;
	private IMenuItem mProgressMenuItem = null;
	private IMenu mMenu = null;
	
	@Override
	public IMenu getActivityMenu() { 
		return mMenu;
	}
	
	@Override
	public synchronized ProviderController getController() {
		if (mController == null) { 
			mController = new ProviderController(ResourceHelper.getApplication(), 
					ResourceHelper.getContext());
		}
		return mController;
	}

	@Override
	public void setContentFragment() { 
		super.setContentFragment();
		
		IMenuExecutor me = getMenuExeccutor();
		if (me != null) me.onUpdateContent(this);
		
		IMenuOperation mo = getMenuOperation();
		if (mo != null) mo.onUpdateContent(this, mMenu);
	}
	
	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		super.doOnCreate(savedInstanceState);
		
		IMenuExecutor me = getMenuExeccutor();
		if (me != null) me.onCreate(this, savedInstanceState);
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
		IMenuExecutor me = getMenuExeccutor();
		if (me != null) return me.onCreateOptionsMenu(this, menu);
		
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
		
		IMenuExecutor me = getMenuExeccutor();
		if (me != null) {
			me.showProgressView(this);
			return;
		}
		
		getActivityHelper().showProgressActionView(mProgressMenuItem);
	}
	
	@Override
	public void hideProgressView() { 
		super.hideProgressView();
		
		IMenuExecutor me = getMenuExeccutor();
		if (me != null) {
			me.hideProgressView(this);
			return;
		}
		
		getActivityHelper().hideProgressActionView(mProgressMenuItem);
	}
	
	protected int getOptionsMenuRes() { return R.menu.app_menu; }
	protected int getOptionsMenuProgressItemId() { return R.id.app_action_progress; }
	
}
