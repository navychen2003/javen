package org.javenstudio.provider.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.SlidingSecondaryFragment;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.account.AccountInfoProvider;
import org.javenstudio.provider.account.AccountSecondaryMenuBinder;

public class AccountSecondaryMenuFragment extends SlidingSecondaryFragment {

	@Override
	public AccountMenuActivity getSlidingSecondaryMenuActivity() { 
		return (AccountMenuActivity)getActivity();
	}
	
	@Override
	public IOptionsMenu getOptionsMenu() {
		Provider p = getSlidingSecondaryMenuActivity().getCurrentProvider();
		if (p != null) return p.getOptionsMenu();
		return null;
	}
	
	public AccountSecondaryMenuBinder getSecondaryMenuBinder() {
		AccountUser user = getSlidingSecondaryMenuActivity().getAccountApp().getAccount();
		if (user != null) {
			AccountInfoProvider p = user.getAccountProvider();
			if (p != null)
				return p.getSecondaryMenuBinder();
		}
		return null;
	}
	
	@Override
	protected View onCreateHeaderView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) 
			return binder.createHeaderView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
	protected View onCreateFooterView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) 
			return binder.createFooterView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
	protected View onCreateBehindView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) 
			return binder.createBehindView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
	protected View onCreateAboveView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) 
			return binder.createAboveView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) binder.onViewCreated(this, view, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) binder.onActivityCreated(this, savedInstanceState);
	}
	
	@Override
	public void onSecondaryMenuVisibilityChanged(int visibility) {
		super.onSecondaryMenuVisibilityChanged(visibility);
		
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) binder.onSecondaryMenuVisibilityChanged(this, getView(), visibility);
	}
	
	@Override
	public void onSecondaryMenuScrolled(float percentOpen) {
		super.onSecondaryMenuScrolled(percentOpen);
		
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) binder.onSecondaryMenuScrolled(this, getView(), percentOpen);
	}
	
	@Override
	protected void setSecondaryMenuListAdapter() {
		//super.setSecondaryMenuListAdapter();
		
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) binder.setSecondaryMenuListAdapter(this);
	}
	
	@Override
	protected void onSecondaryMenuListItemClick(int position) {
		//super.onSecondaryMenuListItemClick(position);
		
		AccountSecondaryMenuBinder binder = getSecondaryMenuBinder();
		if (binder != null) binder.onSecondaryMenuListItemClick(this, position);
	}
	
}
