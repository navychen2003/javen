package org.javenstudio.android.app;

import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.cocoka.app.SlidingSecondaryMenuFragment;

public class SlidingSecondaryFragment extends SlidingSecondaryMenuFragment 
		implements IFragment {

	@Override
	public SlidingActivity getSlidingSecondaryMenuActivity() { 
		return (SlidingActivity)getActivity();
	}

	@Override
	public IOptionsMenu getOptionsMenu() {
		return null;
	}
	
}
