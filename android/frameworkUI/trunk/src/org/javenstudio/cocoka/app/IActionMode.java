package org.javenstudio.cocoka.app;

import android.view.View;

public interface IActionMode {

	public void setTag(Object tag);
	public Object getTag();
	
	public void setTitle(CharSequence title);
	public void setTitle(int resId);
	
	public void setSubtitle(CharSequence subtitle);
	public void setSubtitle(int resId);
	
	public void setTitleOptionalHint(boolean titleOptional);
	public boolean getTitleOptionalHint();
	public boolean isTitleOptional();
	
	public void setCustomView(View view);
	public void invalidate();
	public void finish();
	
	public IMenu getMenu();
	public IMenuInflater getMenuInflater();
	
	public CharSequence getTitle();
	public CharSequence getSubtitle();
	public View getCustomView();
	
	//public boolean isUiFocusable();
	
	public static interface Callback { 
		
		public boolean onCreateActionMode(IActionMode mode, IMenu menu);
		public boolean onPrepareActionMode(IActionMode mode, IMenu menu);
		
		public boolean onActionItemClicked(IActionMode mode, IMenuItem item);
		public void onDestroyActionMode(IActionMode mode);
		
	}
	
}
