package org.javenstudio.provider.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.SlidingFragment;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.account.AccountMenuBinder;
import org.javenstudio.provider.account.AccountInfoProvider;

public class AccountMenuFragment extends SlidingFragment {

	@Override
	public AccountMenuActivity getSlidingMenuActivity() { 
		return (AccountMenuActivity)getActivity();
	}
	
	@Override
	public IOptionsMenu getOptionsMenu() {
		Provider p = getSlidingMenuActivity().getCurrentProvider();
		if (p != null) return p.getOptionsMenu();
		return null;
	}
	
	public AccountMenuBinder getMenuBinder() {
		AccountUser user = getSlidingMenuActivity().getAccountApp().getAccount();
		if (user != null) {
			AccountInfoProvider p = user.getAccountProvider();
			if (p != null)
				return p.getMenuBinder();
		}
		return null;
	}
	
	@Override
	protected View onCreateHeaderView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) 
			return binder.createHeaderView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
	protected View onCreateFooterView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) 
			return binder.createFooterView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
	protected View onCreateBehindView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) 
			return binder.createBehindView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
	protected View onCreateAboveView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) 
			return binder.createAboveView(this, inflater, container, savedInstanceState);
		return null;
	}
	
	@Override
	protected void setMenuListAdapter() {
		super.setMenuListAdapter();
		
		int selectorRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.card_list_selector);
		if (selectorRes != 0) getListView().setSelector(selectorRes);
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) binder.onViewCreated(this, view, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) binder.onActivityCreated(this, savedInstanceState);
	}
	
	@Override
	public void onMenuVisibilityChanged(int visibility) {
		super.onMenuVisibilityChanged(visibility);
		
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) binder.onMenuVisibilityChanged(this, getView(), visibility);
	}
	
	@Override
	public void onMenuScrolled(float percentOpen) {
		super.onMenuScrolled(percentOpen);
		
		AccountMenuBinder binder = getMenuBinder();
		if (binder != null) binder.onMenuScrolled(this, getView(), percentOpen);
	}
	
}
