package org.javenstudio.cocoka.app;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import org.javenstudio.common.util.Logger;

public class SlidingViewPager extends ViewPager {
	private static final Logger LOG = Logger.getLogger(SlidingViewPager.class);
	
    public SlidingViewPager(Context context) {
        super(context);
    }

    public SlidingViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public void setAdapter(PagerAdapter adapter) {
    	if (LOG.isDebugEnabled()) LOG.debug("setAdapter: adapter=" + adapter);
    	super.setAdapter(adapter);
    }
    
    @Override
    public void setCurrentItem(int item) {
    	if (LOG.isDebugEnabled()) LOG.debug("setCurrentItem: item=" + item);
    	super.setCurrentItem(item);
    }
    
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
    	if (LOG.isDebugEnabled()) 
    		LOG.debug("setCurrentItem: item=" + item + " smoothScroll=" + smoothScroll);
    	super.setCurrentItem(item, smoothScroll);
    }
    
}
