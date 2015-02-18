package org.javenstudio.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;

public interface ProviderBinder {

	public View bindView(IActivity activity, LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState);
	
	public void bindBehindAbove(IActivity activity, LayoutInflater inflater, 
			Bundle savedInstanceState);
	
}
