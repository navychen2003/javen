package org.javenstudio.android.information.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.javenstudio.android.app.ContentFragment;
import org.javenstudio.android.information.InformationBinder;
import org.javenstudio.android.information.InformationNavItem;

public final class InformationListFragment extends ContentFragment {

	public InformationListActivity getInformationListActivity() { 
		return (InformationListActivity)getActivity();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		InformationListActivity activity = getInformationListActivity();
		InformationNavItem item = activity.getCurrentItem();
		if (item != null) { 
			InformationBinder binder = item.getBinder();
			if (binder != null) {
				View rootView = binder.inflateView(activity, inflater, container);
				binder.bindView(activity, item, rootView);
				return rootView;
			}
		}
		
		return new LinearLayout(activity.getApplicationContext());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getInformationListActivity().refreshContent(false);
	}
	
}
