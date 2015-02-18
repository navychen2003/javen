package org.javenstudio.android.information.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.javenstudio.android.app.ContentFragment;
import org.javenstudio.android.information.InformationBinder;
import org.javenstudio.android.information.InformationItem;
import org.javenstudio.android.information.InformationSource;

public final class InformationFragment extends ContentFragment {

	public InformationActivity getInformationActivity() { 
		return (InformationActivity)getActivity();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		InformationActivity activity = getInformationActivity();
		InformationSource source = activity.getController().getInformationSource();
		if (source != null) { 
			InformationBinder binder = source.getBinder();
			if (binder != null) {
				View rootView = binder.inflateView(activity, inflater, container);
				binder.bindView(activity, source, rootView);
				return rootView;
			}
		} else { 
			InformationItem item = activity.getController().getInformationItem();
			if (item != null) { 
				InformationBinder binder = item.getBinder();
				if (binder != null) { 
					View rootView = binder.inflateView(activity, inflater, container);
					binder.bindView(activity, item, rootView);
					return rootView;
				}
			}
		}
		
		return new LinearLayout(activity.getApplicationContext());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getInformationActivity().refreshContent(false);
	}
	
}
