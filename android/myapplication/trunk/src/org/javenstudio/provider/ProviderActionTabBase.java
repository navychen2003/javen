package org.javenstudio.provider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.android.app.AppResources;

public abstract class ProviderActionTabBase extends ProviderActionTab {
	//private static final Logger LOG = Logger.getLogger(ProviderActionTabBase.class);

	private ArrayList<WeakReference<View>> mViewRefs = 
			new ArrayList<WeakReference<View>>();
	
	public ProviderActionTabBase(TabItem item, String name) { 
		this(item, name, 0);
	}
	
	public ProviderActionTabBase(TabItem item, String name, int iconRes) { 
		super(item, name, iconRes);
	}
	
	public void addActionView(View view) {
		if (view == null) return;
		
		synchronized (mViewRefs) {
			boolean found = false;
			for (int i=0; i < mViewRefs.size(); ) {
				WeakReference<View> ref = mViewRefs.get(i);
				View v = ref != null ? ref.get() : null;
				if (v != null) {
					if (v == view) found = true;
					i ++; continue;
				}
				mViewRefs.remove(i);
			}
			if (!found) mViewRefs.add(new WeakReference<View>(view));
		}
	}
	
	@Override
	public View[] getBindedViews() { 
		ArrayList<View> views = new ArrayList<View>();
		
		synchronized (mViewRefs) {
			for (int i=0; i < mViewRefs.size(); ) {
				WeakReference<View> ref = mViewRefs.get(i);
				View v = ref != null ? ref.get() : null;
				if (v != null) {
					views.add(v);
					i ++; continue;
				}
				mViewRefs.remove(i);
			}
		}
		
		return views.toArray(new View[views.size()]);
	}
	
	@Override
	public void bindActionViews(View[] actionViews, boolean selected) {
		if (actionViews == null || actionViews.length == 0) return;
		for (View view : actionViews) {
			if (view == null || !(view instanceof TextView)) continue;
			TextView actionView = (TextView)view;
			
			if (selected) { 
				actionView.setBackgroundResource(AppResources.getInstance().getDrawableRes(AppResources.drawable.profile_tab_active_item_selector));
				actionView.setTextColor(AppResources.getInstance().getColorStateList(AppResources.color.accountinfo_action_textcolor));
				actionView.setTypeface(Typeface.create(actionView.getTypeface(), Typeface.BOLD));
				
			} else { 
				actionView.setBackgroundResource(AppResources.getInstance().getDrawableRes(AppResources.drawable.profile_tab_inactive_item_selector));
				actionView.setTextColor(AppResources.getInstance().getColorStateList(AppResources.color.accountinfo_action_textcolor));
				actionView.setTypeface(Typeface.create(actionView.getTypeface(), Typeface.NORMAL));
			}
		}
	}
	
}
