package org.javenstudio.cocoka.opengl;

import java.util.Stack;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;

import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;

public class GLStateManager {
	private static final Logger LOG = Logger.getLogger(GLStateManager.class);
	
    private static final String KEY_MAIN = "activity-state";
    private static final String KEY_DATA = "data";
    private static final String KEY_STATE = "bundle";
    private static final String KEY_CLASS = "class";

    private GLActivity mActivity;
    private Stack<StateEntry> mStack = new Stack<StateEntry>();
    private GLActivityState.ResultEntry mResult;
    private boolean mIsResumed = false;

    public GLStateManager(GLActivity activity) {
        mActivity = activity;
    }

    public void startState(Class<? extends GLActivityState> klass,
            Bundle data) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("startState " + klass);
    	
        GLActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        
        if (!mStack.isEmpty()) {
        	GLActivityState top = getTopState();
            top.transitionOnNextPause(top.getClass(), klass,
                    StateTransitionAnimation.Transition.Incoming);
            
            if (mIsResumed) 
            	top.onPause();
        }
        
        state.initialize(mActivity, data);

        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        
        if (mIsResumed) 
        	state.resume();
    }

    public void startStateForResult(Class<? extends GLActivityState> klass,
            int requestCode, Bundle data) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("startStateForResult " + klass + ", " + requestCode);
    	
        GLActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        
        state.initialize(mActivity, data);
        state.mResult = new GLActivityState.ResultEntry();
        state.mResult.requestCode = requestCode;

        if (!mStack.isEmpty()) {
            GLActivityState as = getTopState();
            as.transitionOnNextPause(as.getClass(), klass,
                    StateTransitionAnimation.Transition.Incoming);
            as.mReceivedResults = state.mResult;
            
            if (mIsResumed) 
            	as.onPause();
        } else {
            mResult = state.mResult;
        }

        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        
        if (mIsResumed) 
        	state.resume();
    }

    public boolean createOptionsMenu(IMenu menu) {
        if (mStack.isEmpty()) 
            return false;
        
        return getTopState().onCreateActionBar(menu);
    }

    public void onConfigurationChange(Configuration config) {
        for (StateEntry entry : mStack) {
            entry.activityState.onConfigurationChanged(config);
        }
    }

    public void resume() {
        if (mIsResumed) return;
        mIsResumed = true;
        
        if (!mStack.isEmpty()) 
        	getTopState().resume();
    }

    public void pause() {
        if (!mIsResumed) return;
        mIsResumed = false;
        
        if (!mStack.isEmpty()) 
        	getTopState().onPause();
    }

    public void notifyActivityResult(int requestCode, int resultCode, Intent data) {
        getTopState().onStateResult(requestCode, resultCode, data);
    }

    public void clearActivityResult() {
        if (!mStack.isEmpty()) 
            getTopState().clearStateResult();
    }

    public int getStateCount() {
        return mStack.size();
    }

    public boolean itemSelected(IMenuItem item) {
        if (!mStack.isEmpty()) {
            if (getTopState().onItemSelected(item)) 
            	return true;
            
            if (item.getItemId() == android.R.id.home) {
                if (mStack.size() > 1) 
                    getTopState().onBackPressed();
                
                return true;
            }
        }
        
        return false;
    }

    public boolean onBackPressed() {
        if (!mStack.isEmpty()) {
            getTopState().onBackPressed();
            return true;
        }
        return false;
    }

    public void finishState(GLActivityState state) {
        finishState(state, true);
    }

    public void clearTasks() {
        // Remove all the states that are on top of the bottom PhotoPage state
        while (mStack.size() > 1) {
            mStack.pop().activityState.onDestroy();
        }
    }

    void finishState(GLActivityState state, boolean fireOnPause) {
        // The finish() request could be rejected (only happens under Monkey),
        // If it is rejected, we won't close the last page.
        if (mStack.size() == 1) {
            Activity activity = (Activity) mActivity.getActivityContext();
            if (mResult != null) 
                activity.setResult(mResult.resultCode, mResult.resultData);
            
            activity.finish();
            if (!activity.isFinishing()) {
            	if (LOG.isWarnEnabled())
                	LOG.warn("finish is rejected, keep the last state");
            	
                return;
            }
            
            if (LOG.isDebugEnabled())
            	LOG.debug("no more state, finish activity");
        }

        if (LOG.isDebugEnabled())
        	LOG.debug("finishState " + state);
        
        if (state != mStack.peek().activityState) {
            if (state.isDestroyed()) {
            	if (LOG.isDebugEnabled())
                	LOG.debug("The state is already destroyed");
            	
                return;
            } else {
                throw new IllegalArgumentException("The stateview to be finished"
                        + " is not at the top of the stack: " + state + ", "
                        + mStack.peek().activityState);
            }
        }

        // Remove the top state.
        mStack.pop();
        state.mIsFinishing = true;
        GLActivityState top = !mStack.isEmpty() ? mStack.peek().activityState : null;
        if (mIsResumed && fireOnPause) {
            if (top != null) {
                state.transitionOnNextPause(state.getClass(), top.getClass(),
                        StateTransitionAnimation.Transition.Outgoing);
            }
            state.onPause();
        }
        mActivity.getGLRoot().setContentPane(null);
        state.onDestroy();

        if (top != null && mIsResumed) 
        	top.resume();
    }

    public void switchState(GLActivityState oldState,
            Class<? extends GLActivityState> klass, Bundle data) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("switchState " + oldState + ", " + klass);
    	
        if (oldState != mStack.peek().activityState) {
            throw new IllegalArgumentException("The stateview to be finished"
                    + " is not at the top of the stack: " + oldState + ", "
                    + mStack.peek().activityState);
        }
        
        // Remove the top state.
        mStack.pop();
        //if (!data.containsKey(PhotoPage.KEY_APP_BRIDGE)) {
        //    // Do not do the fade out stuff when we are switching camera modes
        //    oldState.transitionOnNextPause(oldState.getClass(), klass,
        //            StateTransitionAnimation.Transition.Incoming);
        //}
        if (mIsResumed) oldState.onPause();
        oldState.onDestroy();

        // Create new state.
        GLActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        
        state.initialize(mActivity, data);
        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed) state.resume();
    }

    public void destroy() {
    	if (LOG.isDebugEnabled())
        	LOG.debug("destroy");
    	
        while (!mStack.isEmpty()) {
            mStack.pop().activityState.onDestroy();
        }
        mStack.clear();
    }

    @SuppressWarnings("unchecked")
    public void restoreFromState(Bundle inState) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("restoreFromState");
    	
        Parcelable list[] = inState.getParcelableArray(KEY_MAIN);
        for (Parcelable parcelable : list) {
            Bundle bundle = (Bundle) parcelable;
            Class<? extends GLActivityState> klass =
                    (Class<? extends GLActivityState>) bundle.getSerializable(KEY_CLASS);

            Bundle data = bundle.getBundle(KEY_DATA);
            Bundle state = bundle.getBundle(KEY_STATE);

            GLActivityState activityState;
            try {
            	if (LOG.isDebugEnabled())
            		LOG.debug("restoreFromState " + klass);
            	
                activityState = klass.newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            activityState.initialize(mActivity, data);
            activityState.onCreate(data, state);
            mStack.push(new StateEntry(data, activityState));
        }
    }

    public void saveState(Bundle outState) {
    	if (LOG.isDebugEnabled())
        	LOG.debug("saveState");

        Parcelable list[] = new Parcelable[mStack.size()];
        int i = 0;
        for (StateEntry entry : mStack) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_CLASS, entry.activityState.getClass());
            bundle.putBundle(KEY_DATA, entry.data);
            Bundle state = new Bundle();
            entry.activityState.onSaveState(state);
            bundle.putBundle(KEY_STATE, state);
            
            if (LOG.isDebugEnabled())
            	LOG.debug("saveState " + entry.activityState.getClass());
            
            list[i++] = bundle;
        }
        outState.putParcelableArray(KEY_MAIN, list);
    }

    public boolean hasStateClass(Class<? extends GLActivityState> klass) {
        for (StateEntry entry : mStack) {
            if (klass.isInstance(entry.activityState)) 
                return true;
        }
        return false;
    }

    public GLActivityState getTopState() {
        Utils.assertTrue(!mStack.isEmpty());
        return mStack.peek().activityState;
    }

    private static class StateEntry {
        public final Bundle data;
        public final GLActivityState activityState;

        public StateEntry(Bundle data, GLActivityState state) {
            this.data = data;
            this.activityState = state;
        }
    }
    
}
