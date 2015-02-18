package org.javenstudio.cocoka.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.graphics.drawable.Drawable;
import android.view.View;

final class AndroidTab implements IActionBar.ITab {

	private final ActionBar.Tab mTab;
	
	public AndroidTab(ActionBar.Tab tab) {
		if (tab == null) throw new NullPointerException();
		mTab = tab;
	}
	
	public ActionBar.Tab getTab() {
		return mTab;
	}
	
	@Override
	public int getPosition() {
		return mTab.getPosition();
	}
	
	@Override
	public Drawable getIcon() {
		return mTab.getIcon();
	}
	
	@Override
	public CharSequence getText() {
		return mTab.getText();
	}
	
	@Override
	public View getCustomView() {
		return mTab.getCustomView();
	}
	
	@Override
	public CharSequence getContentDescription() {
		return mTab.getContentDescription();
	}
	
	@Override
	public IActionBar.ITab setIcon(Drawable icon) {
		mTab.setIcon(icon);
		return this;
	}
	
	@Override
	public IActionBar.ITab setIcon(int resId) {
		mTab.setIcon(resId);
		return this;
	}
	
	@Override
	public IActionBar.ITab setText(CharSequence text) {
		mTab.setText(text);
		return this;
	}
	
	@Override
	public IActionBar.ITab setText(int resId) {
		mTab.setText(resId);
		return this;
	}
	
	@Override
	public IActionBar.ITab setCustomView(View view) {
		mTab.setCustomView(view);
		return this;
	}
	
	@Override
	public IActionBar.ITab setCustomView(int layoutResId) {
		mTab.setCustomView(layoutResId);
		return this;
	}
	
	@Override
	public IActionBar.ITab setContentDescription(int resId) {
		mTab.setContentDescription(resId);
		return this;
	}
	
	@Override
	public IActionBar.ITab setContentDescription(CharSequence contentDesc) {
		mTab.setContentDescription(contentDesc);
		return this;
	}
	
	@Override
	public IActionBar.ITab setTabListener(final IActionBar.ITabListener listener) {
		if (listener != null) {
			mTab.setTabListener(new ActionBar.TabListener() {
				@Override
				public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
					if (tab == mTab) listener.onTabUnselected(AndroidTab.this, ft);
				}
				@Override
				public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
					if (tab == mTab) listener.onTabSelected(AndroidTab.this, ft);
				}
				@Override
				public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
					if (tab == mTab) listener.onTabReselected(AndroidTab.this, ft);
				}
			});
		}
		return this;
	}
	
	@Override
	public IActionBar.ITab setTag(Object obj) {
		mTab.setTag(obj);
		return this;
	}
	
	@Override
	public Object getTag() {
		return mTab.getTag();
	}
	
	@Override
	public void select() {
		mTab.select();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{position=" + getPosition() + "}";
	}
	
}
