package org.javenstudio.cocoka.app;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface SupportMenuItem {

	public int getItemId();
	public int getGroupId();
	public int getOrder();
	
	public View getActionView();
	public CharSequence getTitle();
	public Drawable getIcon();
	
	public SupportMenuItem setTitle(CharSequence title);
	public SupportMenuItem setTitle(int title);
	
	public SupportMenuItem setActionView(View view);
	public SupportMenuItem setActionView(int resId);
	
	public SupportMenuItem setIcon(Drawable icon);
	public SupportMenuItem setIcon(int iconRes);
	
	public SupportMenuItem setVisible(boolean visible);
	public boolean isVisible();
	
	public void setShowAsAction(int actionEnum);
	
	public SupportActionProvider getActionProvider();
	
}
