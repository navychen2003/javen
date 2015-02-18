package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class ActionTabLayout extends LinearLayout {
	//private static final Logger LOG = Logger.getLogger(ActionTabLayout.class);

	public ActionTabLayout(Context context) {
        super(context);
    }

    public ActionTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public ActionTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
    @Override
	public void offsetTopAndBottom(int offset) {
    	//if (LOG.isDebugEnabled()) LOG.debug("offsetTopAndBottom: offset=" + offset);
		super.offsetTopAndBottom(offset);
	}
    
}
