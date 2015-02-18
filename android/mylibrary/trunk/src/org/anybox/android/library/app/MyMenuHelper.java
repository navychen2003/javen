package org.anybox.android.library.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.anybox.android.library.R;
import org.javenstudio.provider.activity.AccountMenuFragment;
import org.javenstudio.provider.activity.AccountSecondaryMenuFragment;

public abstract class MyMenuHelper {

	public abstract int getMenuHeaderBottomBackgroundRes();
	public abstract View createMenuBehindView(AccountMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);
	public abstract void onMenuViewCreated(AccountMenuFragment fragment, 
			View view, Bundle savedInstanceState);
	
	public abstract View createSecondaryMenuBehindView(AccountSecondaryMenuFragment fragment, 
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);
	
	public abstract void onSecondaryMenuViewCreated(AccountSecondaryMenuFragment fragment, 
			View view, Bundle savedInstanceState);
	
	public static final MyMenuHelper BEACH = new BeachMenuHelper();
	
	private static class BeachMenuHelper extends MyMenuHelper {
		
		@Override
		public int getMenuHeaderBottomBackgroundRes() {
			return R.drawable.bg_accountmenu_headerbottom;
		}
		
		@Override
		public View createMenuBehindView(AccountMenuFragment fragment, 
				LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.accountmenu_behind, null);
			
			ImageView imageView = (ImageView)view.findViewById(R.id.accountmenu_behind_image);
			if (imageView != null) {
				imageView.setImageResource(R.drawable.background_menu);
				imageView.setVisibility(View.VISIBLE);
			}
			
			View layoutView = view;
			if (layoutView != null) {
				layoutView.setBackgroundResource(R.color.white);
			}
			
			return view;
		}
		
		@Override
		public void onMenuViewCreated(AccountMenuFragment fragment, 
				View view, Bundle savedInstanceState) {
			if (fragment == null) return;
			
			View listView = fragment.getListView();
			if (listView != null) {
				listView.setBackground(null);
			}
			
			View headerView = fragment.getHeaderView();
			if (headerView != null) {
				//headerView.setBackground(null);
			}
			
			View footerView = fragment.getFooterView();
			if (footerView != null) {
				footerView.setBackground(null);
			}
		}
		
		@Override
		public View createSecondaryMenuBehindView(AccountSecondaryMenuFragment fragment, 
				LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.secondmenu_behind, null);
			
			ImageView imageView = (ImageView)view.findViewById(R.id.secondmenu_behind_image);
			if (imageView != null) {
				imageView.setImageResource(R.drawable.background_menu2);
				imageView.setVisibility(View.VISIBLE);
			}
			
			View layoutView = view;
			if (layoutView != null) {
				layoutView.setBackgroundResource(R.color.yellow_menu_background_color);
			}
			
			return view;
		}
		
		@Override
		public void onSecondaryMenuViewCreated(AccountSecondaryMenuFragment fragment, 
				View view, Bundle savedInstanceState) {
			if (fragment == null) return;
			
			View listView = fragment.getListView();
			if (listView != null) {
				listView.setBackground(null);
			}
			
			View headerView = fragment.getHeaderView();
			if (headerView != null) {
				headerView.setBackground(null);
			}
			
			View footerView = fragment.getFooterView();
			if (footerView != null) {
				footerView.setBackground(null);
			}
		}
		
	}
	
}
