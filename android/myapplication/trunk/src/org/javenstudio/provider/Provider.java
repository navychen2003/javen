package org.javenstudio.provider;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.MenuOperation;
import org.javenstudio.android.data.DataBinderListener;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.IOptionsMenu;

public abstract class Provider {
	//private static final Logger LOG = Logger.getLogger(Provider.class);
	
	protected final IMenuOperation mMenuOperation;
	protected final String mName;
	
	protected int mIconRes;
	protected int mNavigationIconRes;
	
	protected CharSequence mTitle = null;
	protected CharSequence mSubTitle = null;
	protected CharSequence mNavigationTitle = null;
	
	protected IOptionsMenu mOptionsMenu = null;
	protected DataBinderListener mBindListener = null;
	
	protected boolean mActionModeEnabled = false;
	protected boolean mDefault = false;
	
	public Provider(String name, int iconRes) { 
		if (name == null) throw new NullPointerException();
		mMenuOperation = new MenuOperation();
		mName = name;
		mIconRes = iconRes;
		mNavigationIconRes = iconRes;
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	public final long getIdentity() { return mIdentity; }
	
	public final String getName() { return mName; }
	public final String getKey() { return getClass().getName(); }
	
	//public String getNavigationName() { return getName(); }
	public IMenuOperation getMenuOperation() { return mMenuOperation; }
	
	public int getIconRes() { return mIconRes; }
	public void setIconRes(int iconRes) { mIconRes = iconRes; }
	
	public int getNavigationIconRes() { return mNavigationIconRes; }
	public void setNavigationIconRes(int iconRes) { mNavigationIconRes = iconRes; }
	
	public CharSequence getTitle() { return mTitle != null ? mTitle : mName; }
	public CharSequence getSubTitle() { return mSubTitle; }
	public CharSequence getDropdownTitle() { return null; }
	
	public void setTitle(CharSequence s) { mTitle = s; }
	public void setSubTitle(CharSequence s) { mSubTitle = s; }
	
	public void setNavigationTitle(CharSequence s) { mNavigationTitle = s; }
	public CharSequence getNavigationTitle() { 
		return mNavigationTitle != null ? mNavigationTitle : getName(); 
	}
	
	public void setBindListener(DataBinderListener l) { mBindListener = l; }
	public DataBinderListener getBindListener() { return mBindListener; }
	
	public boolean isDefault() { return mDefault; }
	public void setDefault(boolean def) { mDefault = def; }
	
	public int getHomeAsUpIndicatorRes() { return 0; }
	public View getActionCustomView(IActivity activity) { return null; }
	public ViewGroup.LayoutParams getActionCustomLayoutParams(IActivity activity) { return null; }
	
	public ActionItem[] getActionItems(IActivity activity) { return null; }
	public boolean onActionItemSelected(IActivity activity, int position, long id) { return true; }
	public void onPagerItemSelected(IActivity activity) {}
	
	public abstract ProviderBinder getBinder();
	public boolean isContentProgressEnabled() { return false; }
	public void reloadOnThread(ProviderCallback callback, ReloadType type, long reloadId) {}
	public boolean hasNextPage() { return true; }
	
	public CharSequence getLastUpdatedLabel(IActivity activity) { return null; }
	public boolean overridePendingTransitionOnFinish(IActivity activity) { return false; }
	
	public boolean handleClick(IActivity activity, ActionItem item) { return false; }
	public boolean onBackPressed(IActivity activity) { return false; }
	public boolean onActionHome(IActivity activity) { return false; }
	public boolean onFlingToRight(IActivity activity) { return false; }
	
	public void onFragmentPreCreate(IActivity activity) {}
	public void onFragmentCreate(IActivity activity) {}
	public void onAttach(IActivity activity) {}
	public void onDetach(IActivity activity) {}
	
	public void onSaveFragmentState(Bundle savedInstanceState) {}
	//public void onRestoreFragmentState(Bundle savedInstanceState) {}
	public void onSaveActivityState(Bundle savedInstanceState) {}
	public void onRestoreActivityState(Bundle savedInstanceState) {}
	
	public int getActionBarTitleColorRes(IActivity activity) { return 0; }
	public Drawable getActionBarBackground(IActivity activity) { return null; }
	public Drawable getBackground(IActivity activity, int width, int height) { return null; }
	public void setContentBackground(IActivity activity) {}
	
	public IOptionsMenu getOptionsMenu() { return mOptionsMenu; }
	public void setOptionsMenu(IOptionsMenu menu) { mOptionsMenu = menu; }
	
	public boolean isLockOrientationDisabled(int orientation) { return false; }
	public boolean isUnlockOrientationDisabled() { return false; }
	
	public Drawable getIcon() { return getIconDrawable(mIconRes); }
	public Drawable getNavigationIcon() { return getIconDrawable(mNavigationIconRes); }
	
	private Drawable getIconDrawable(int iconRes) {
		if (iconRes != 0) return ResourceHelper.getResources().getDrawable(iconRes); 
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + mIdentity 
				+ "{name=" + mName + "}";
	}
	
}
