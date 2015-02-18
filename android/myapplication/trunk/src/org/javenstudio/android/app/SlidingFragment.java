package org.javenstudio.android.app;

import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.cocoka.app.SlidingMenuFragment;

public class SlidingFragment extends SlidingMenuFragment implements IFragment {
	
	@Override
	public SlidingActivity getSlidingMenuActivity() { 
		return (SlidingActivity)getActivity();
	}

	@Override
	public IOptionsMenu getOptionsMenu() {
		return null;
	}
	
}
