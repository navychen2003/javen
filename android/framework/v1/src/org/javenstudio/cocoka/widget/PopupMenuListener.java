package org.javenstudio.cocoka.widget;

import android.view.View;

public interface PopupMenuListener {

	public void showPopupMenuAt(int id, PopupMenu menu, final View view); 
	public PopupMenu createPopupMenu(int id, final View view); 
	
	public void onPopupMenuCreated(int id, PopupMenu menu, final View view); 
	public void onPopupMenuShow(int id, PopupMenu menu, final View view); 
	public void onPopupMenuDismiss(int id, PopupMenu menu, final View view); 
	
}
