package org.javenstudio.cocoka.widget.panel;

import android.view.View;

import org.javenstudio.cocoka.widget.ToolBar;

public abstract class PanelController implements PanelManager.PanelAction {

	private PanelManager mManager; 
	private boolean mPanelInited = false; 
	
	public PanelController() {} 
	
	public boolean isInited() { return mPanelInited; } 
	
	public final void initPanels() {
		mPanelInited = false; 
		
		if (!onInitPanels()) return; 
		
		final PanelLayout layout = getPanelLayout(); 
		layout.setController(this); 
		
		mManager = createPanelManager(); 
		mManager.initPanels(layout, getPanelBar()); 
    	
    	mPanelInited = true; 
	}
	
	public final PanelManager getManager() {
		return mManager; 
	}
	
	protected abstract PanelManager createPanelManager(); 
	protected abstract boolean onInitPanels(); 
	protected abstract PanelLayout getPanelLayout(); 
	protected abstract ToolBar getPanelBar(); 
	
	public void onResume() {
		if (!isInited()) return; 
		
		// do nothing
	}
	
	public void onDestroy() {
		if (!isInited()) return; 
		
		mPanelInited = false; 
	}
	
	public boolean onBackPressed() {
		if (!isInited()) return false; 
		
		mManager.onBackPressed(); 
		
		return false; 
	}
	
	@Override
	public void onResult(int code, Object result) {
    	// do nothing
    }
    
    @Override
    public void onIconClicked(PanelManager.PanelConfig conf) {
    	// do nothing
    }
	
    @Override
	public boolean dispatchIconClick(PanelManager.PanelConfig conf) {
    	return false; 
    }
    
    public int getPanelMaxWidth(int widthSpecSize) {
    	return widthSpecSize; 
    }
    
    public int getPanelMaxHeight(int heightSpecSize) {
    	return heightSpecSize; 
    }
    
    public void onPanelCreated(View view) {
    	// do nothing
    }
    
    public void onPanelViewAdded(View view) {
    	// do nothing
    }
    
    public void onPanelViewRemoved(View view) {
    	// do nothing
    }
    
    public void onPanelShown(View view) {
    	// do nothing
    }
    
    public void onPanelHidden(View view) {
    	// do nothing
    }
    
}
