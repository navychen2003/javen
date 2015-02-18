package org.javenstudio.cocoka.opengl;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import org.javenstudio.common.util.Logger;

public abstract class BottomControls implements OnClickListener {
	private static final Logger LOG = Logger.getLogger(BottomControls.class);

    public interface Delegate {
        public boolean canDisplayBottomControls();
        public boolean canDisplayBottomControl(View control);
        public boolean isBottomControl(View control);
        public void onBottomControlClicked(View control);
        public void refreshBottomControlsWhenReady();
        public Activity getActivity();
    }
    
    public interface BottomAction { 
    	public String getName();
    	public int getIconRes();
    	public void actionClick(Activity activity, View root);
    }
    
    protected final Delegate mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;

    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap<View, Boolean>();

    private Animation mContainerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mContainerAnimOut = new AlphaAnimation(1f, 0f);
    private static final int CONTAINER_ANIM_DURATION_MS = 200;

    private static final int CONTROL_ANIM_DURATION_MS = 150;
    private static Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);
        anim.setDuration(CONTROL_ANIM_DURATION_MS);
        return anim;
    }

    public BottomControls(Context context, Delegate delegate, ViewGroup layout) {
        mDelegate = delegate;
        mParentLayout = layout;

        LayoutInflater inflater = LayoutInflater.from(context);
        mContainer = (ViewGroup) inflateContainerView(inflater, mParentLayout);
        mParentLayout.addView(mContainer, createContainerLayoutParams());

        for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            if (delegate.isBottomControl(child)) {
	            child.setOnClickListener(this);
	            mControlsVisible.put(child, false);
            }
        }

        //mContainer.setOnClickListener(this);
        initViews(inflater, mContainer);
        
        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshBottomControlsWhenReady();
    }
    
    private void hide() {
    	if (mContainer == null) return;
        mContainer.clearAnimation();
        mContainerAnimOut.reset();
        mContainer.startAnimation(mContainerAnimOut);
        mContainer.setVisibility(View.INVISIBLE);
    }

    private void show() {
    	if (mContainer == null) return;
        mContainer.clearAnimation();
        mContainerAnimIn.reset();
        mContainer.startAnimation(mContainerAnimIn);
        mContainer.setVisibility(View.VISIBLE);
    }

    public void refresh(Object item) {
        boolean visible = mDelegate.canDisplayBottomControls();
        boolean visibilityChanged = (visible != mContainerVisible);
        
        if (LOG.isDebugEnabled())
        	LOG.debug("refresh: visible=" + visible + " changed=" + visibilityChanged);
        
        if (visibilityChanged) {
            if (visible) show();
            else hide();
            mContainerVisible = visible;
        }
        if (!mContainerVisible) 
            return;
        
        for (View control : mControlsVisible.keySet()) {
            Boolean prevVisibility = mControlsVisible.get(control);
            boolean curVisibility = mDelegate.canDisplayBottomControl(control);
            
            if (LOG.isDebugEnabled()) {
            	LOG.debug("refresh: view=" + control + " prevVisible=" + prevVisibility 
            			+ " curVisible=" + curVisibility);
            }
            
            if (prevVisibility != null && prevVisibility.booleanValue() != curVisibility) {
                if (!visibilityChanged) {
                    control.clearAnimation();
                    control.startAnimation(getControlAnimForVisibility(curVisibility));
                }
                control.setVisibility(curVisibility ? View.VISIBLE : View.INVISIBLE);
                mControlsVisible.put(control, curVisibility);
            }
        }
        
        refreshViews(item, mContainer);
        
        // Force a layout change
        mContainer.requestLayout(); // Kick framework to draw the control.
    }

    public void cleanup() {
        mParentLayout.removeView(mContainer);
        mControlsVisible.clear();
        mContainer = null;
    }

    @Override
    public void onClick(View view) {
        if (mContainerVisible) {
        	Boolean visible = mControlsVisible.get(view);
        	if (visible == null || visible.booleanValue())
        		mDelegate.onBottomControlClicked(view);
        }
    }
    
    protected void initViews(LayoutInflater inflater, View view) { 
    	View controls = findControlsView(view);
    	if (controls != null) 
    		controls.setOnClickListener(this);
    	
    	//View body = findBodyView(view);
    	//if (body != null) 
    	//	body.setOnClickListener(this);
    }
    
    protected void refreshViews(Object item, View view) {}
    
    public boolean isControlsView(View view) { 
    	return view != null && view == findControlsView(mContainer); 
    }
    
    public boolean isBodyView(View view) { 
    	return view != null && view == findBodyView(mContainer); 
    }
    
    protected abstract View findControlsView(View view);
    protected abstract View findBodyView(View view);
    protected abstract View inflateContainerView(LayoutInflater inflater, ViewGroup root);
    protected abstract ViewGroup.LayoutParams createContainerLayoutParams();
    
}
