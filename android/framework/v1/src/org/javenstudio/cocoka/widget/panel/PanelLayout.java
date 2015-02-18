package org.javenstudio.cocoka.widget.panel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import org.javenstudio.cocoka.widget.SimplePanel;

public class PanelLayout extends SimplePanel {
	
	private PanelController mController = null; 
	private PanelManager.PanelConfig mPanelConfig = null; 
	private PanelManager.PanelConfig mRemovedConfig = null; 
	
	public PanelLayout(Context context) {
		this(context, null);
	}

    public PanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setController(PanelController controller) {
    	mController = controller; 
    }
    
    public View setPanelView(PanelManager.PanelConfig config) {
    	if (config == null) return null; 
    	
    	View saved = getContentView(); 
    	View view = null; 
    	
    	if (saved != null && mPanelConfig == config) {
    		view = saved; 
    		
    	} else { 
    		mRemovedConfig = mPanelConfig; 
    		mPanelConfig = config; 
    		
	    	view = setContentView(config.mViewResource); 
	    	
	    	onPanelCreated(view); 
	    	onPanelShown(); 
    	}
    	
    	return view; 
    }
    
    @Override 
    protected int measureWidth(int widthSpecSize, int contentWidth) {
    	return widthSpecSize; 
    }
    
    @Override 
    protected int measureHeight(int heightSpecSize, int contentHeight) {
    	return contentHeight; 
    }
    
    @Override
    protected void onMeasureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
    	final PanelController controller = mController; 
    	final PanelManager.PanelConfig config = mPanelConfig; 
    	
    	//int widthSpecMode = MeasureSpec.getMode(parentWidthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(parentWidthMeasureSpec);

        //int heightSpecMode = MeasureSpec.getMode(parentHeightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(parentHeightMeasureSpec);
        
        LayoutParams lp = (LayoutParams)child.getLayoutParams(); 
    	if (lp != null && controller != null && config != null && lp.fillscreen) {
    		if (lp.width < 0 || lp.width < widthSpecSize) 
    			lp.width = controller.getPanelMaxWidth(widthSpecSize); 
    		if (lp.height < 0 || lp.width < heightSpecSize) 
    			lp.height = controller.getPanelMaxHeight(heightSpecSize); 
    		
    		return; 
    	}
    	
    	super.onMeasureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec); 
    }
    
    protected void onPanelCreated(View view) {
    	final PanelController controller = mController; 
    	final PanelManager.PanelConfig config = mPanelConfig; 
    	
    	if (controller != null && config != null) {
    		PanelManager manager = controller.getManager(); 
    		
    		controller.onPanelCreated(view); 
    		
    		if (config.mCreateListener != null) 
    			config.mCreateListener.onCreated(manager, config, view); 
    	}
    }
    
    @Override
    protected void onViewAdded(View view) {
    	final PanelController controller = mController; 
    	final PanelManager.PanelConfig config = mPanelConfig; 
    	
    	if (controller != null && config != null) {
    		PanelManager manager = controller.getManager(); 
    		
    		controller.onPanelViewAdded(view); 
    		
    		if (config.mAddListener != null) 
    			config.mAddListener.onAdded(manager, config, view); 
    	}
    }
    
    @Override
    protected void onViewRemoved(View view) {
    	final PanelController controller = mController; 
    	final PanelManager.PanelConfig config = mRemovedConfig; 
    	
    	if (controller != null && config != null) {
    		PanelManager manager = controller.getManager(); 
    		
    		controller.onPanelViewRemoved(view); 
    		
    		if (config.mRemoveListener != null) 
    			config.mRemoveListener.onRemoved(manager, config, view); 
    	}
    }
    
    public void onPanelShown() {
    	final PanelController controller = mController; 
    	final PanelManager.PanelConfig config = mPanelConfig; 
    	
    	if (controller != null && config != null) {
    		PanelManager manager = controller.getManager(); 
    		View view = getContentView(); 
    		
        	controller.onPanelShown(view); 
    		
    		if (config.mShownListener != null) 
    			config.mShownListener.onShown(manager, config, view);
    	}
    }
    
    public void onPanelHidden() {
    	final PanelController controller = mController; 
    	final PanelManager.PanelConfig config = mPanelConfig; 
    	
    	if (controller != null && config != null) {
    		PanelManager manager = controller.getManager(); 
    		View view = getContentView(); 
    		
    		controller.onPanelHidden(view); 
    		
    		if (config.mHiddenListener != null) 
    			config.mHiddenListener.onHidden(manager, config, view);
    	}
    }
    
    public void onPanelDestroy() {
    	final PanelController controller = mController; 
    	final PanelManager.PanelConfig config = mPanelConfig; 
    	
    	if (controller != null && config != null) {
    		PanelManager manager = controller.getManager(); 
    		View view = getContentView(); 
    		
    		onViewRemoved(view); 
    		
    		if (config.mDestroyListener != null) 
    			config.mDestroyListener.onDestroy(manager, config, view);
    	}
    }
    
}
