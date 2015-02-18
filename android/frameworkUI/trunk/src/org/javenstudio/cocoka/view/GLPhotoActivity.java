package org.javenstudio.cocoka.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.opengl.GLActionBar;
import org.javenstudio.cocoka.opengl.GLActivity;

public abstract class GLPhotoActivity extends GLActivity 
		implements LoadCallback {

	public static final String KEY_DISMISS_KEYGUARD = "dismiss-keyguard";
	
	public void postShowProgress(boolean force) {}
	public void postHideProgress(boolean force) {}
	
	@Override
	public void onLoading() { postShowProgress(false); }
	
	@Override
	public void onLoaded() { postHideProgress(false); }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        if (getIntent().getBooleanExtra(KEY_DISMISS_KEYGUARD, false)) 
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        setContentView();

        if (savedInstanceState != null) 
            getGLStateManager().restoreFromState(savedInstanceState);
        else 
            initializeByIntent();
    }
	
    @Override
    protected void setContentView() { 
    	setContentView(R.layout.gl_main);
    }
    
    @Override
    protected View findGLRootView() { 
    	return findViewById(R.id.gl_root_view);
    }
    
    @Override
    protected View findGLMainRootView() { 
    	return findViewById(R.id.gl_main_root);
    }
    
    @Override
    protected GLActionBar createGLActionBar() { 
    	return new GLPhotoActionBar(this);
    }
    
    public void setShareIntent(Intent shareIntent) { 
    	GLActionBar actionBar = getGLActionBar();
    	if (actionBar != null && actionBar instanceof GLPhotoActionBar) 
    		((GLPhotoActionBar)actionBar).setShareIntent(shareIntent);
    }
    
    protected void initializeByIntent() {}
    protected void onExceptionCatched(Throwable e) {}
    
}
