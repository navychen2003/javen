package org.javenstudio.cocoka.view;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.opengl.BottomControls;

public class PhotoAction extends ActionItem 
		implements BottomControls.BottomAction {

	private boolean mDefault = false;
	private View mView = null;
	private ActionItem[] mActionItems = null;
	private boolean mSelected = false;
	
	public PhotoAction(String name, int iconRes) { 
		super(name, iconRes);
	}

	public boolean isSelected() { return mSelected; }
	
	public boolean isDefault() { return mDefault; }
	public void setDefault(boolean def) { mDefault = def; }
	
	void setBindedView(View view) { mView = view; }
	View getBindedView() { return mView; }
	
	void setActionItems(ActionItem[] items) { mActionItems = items; }
	ActionItem[] getActionItems() { return mActionItems; }
	
	protected void onActionClick(Activity activity, View root, boolean reclick) {}
	
	@Override
	public final void actionClick(Activity activity, View root) { 
		final ViewGroup container = (ViewGroup)root;
		container.removeAllViews();
		
		boolean reclick = false;
		ActionItem[] items = mActionItems;
		
		for (int i=0; items != null && i < items.length; i++) { 
			final PhotoAction actionItem = (PhotoAction)items[i];
			if (actionItem == null) continue;
			
			TextView actionView = (TextView)actionItem.getBindedView();
			if (actionView == null) continue;
			
			if (actionItem == this) { 
				if (actionItem.mSelected) reclick = true;
				actionItem.mSelected = true;
				actionView.setBackgroundResource(getActionBackgroundRes(true));
				actionView.setTextColor(getActionTextColor(true));
				
			} else { 
				actionItem.mSelected = false;
				actionView.setBackgroundResource(getActionBackgroundRes(false));
				actionView.setTextColor(getActionTextColor(false));
			}
		}
		
		onActionClick(activity, root, reclick);
	}
	
	protected int getActionTextColor(boolean selected) {
		return selected ? Color.WHITE : Color.GRAY;
	}
	
	protected int getActionBackgroundRes(boolean selected) {
		if (selected) {
			int backgroundRes = BaseResources.getInstance().getDrawableRes(BaseResources.drawable.photo_action_selected_background);
			if (backgroundRes == 0) backgroundRes = R.drawable.photo_action_pressed_background;
			return backgroundRes;
		} else {
			int backgroundRes = BaseResources.getInstance().getDrawableRes(BaseResources.drawable.photo_action_background);
			if (backgroundRes == 0) backgroundRes = R.drawable.photo_action_background;
			return backgroundRes;
		}
	}
	
}
