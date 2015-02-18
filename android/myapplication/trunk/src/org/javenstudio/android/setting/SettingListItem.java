package org.javenstudio.android.setting;

import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.widget.CustomLayout;
import org.javenstudio.cocoka.widget.CustomLinearLayout;

public class SettingListItem extends CustomLinearLayout {

	public interface OnButtonClickListener { 
		public void onRightButtonClicked(View view); 
	}
	
	private OnButtonClickListener mOnButtonClickListener = null; 
	
	public SettingListItem(Context context) {
        super(context);
    }

    public SettingListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) { 
    	mOnButtonClickListener = listener; 
    }
    
    @Override 
    public void onInitClickArea(List<CustomLayout.ClickArea> areas) { 
    	final View rightBtn = findViewById(R.id.setting_child_rightlayout); 
    	if (rightBtn != null) { 
    		areas.add(new CustomLayout.ClickArea(
        			new Rect(rightBtn.getLeft() - 10, rightBtn.getTop() - 10, 
        					rightBtn.getRight() + 10, rightBtn.getBottom() + 10), 
        			new CustomLayout.OnClickListener() {
    					@Override
    					public void onAreaClicked(CustomLayout.ClickArea area) {
    						onRightButtonClicked(rightBtn); 
    					}
    				}
        		)); 
    	}
    }
    
    protected void onRightButtonClicked(View view) { 
    	OnButtonClickListener listener = mOnButtonClickListener; 
    	if (listener != null) 
    		listener.onRightButtonClicked(view); 
    }
    
}
