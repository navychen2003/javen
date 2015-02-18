package org.javenstudio.android.information.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;

import org.javenstudio.android.app.SimpleActivity;
import org.javenstudio.android.information.InformationController;
import org.javenstudio.android.information.InformationListModel;
import org.javenstudio.android.information.InformationSource;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.common.util.Logger;

public abstract class InformationActivity extends SimpleActivity {
	private static final Logger LOG = Logger.getLogger(InformationActivity.class);
	protected static final String EXTRA_LOCATION = "info.location";
	
	protected boolean mContentInited = false;
	
	public InformationController getController() { 
		return InformationController.getInstance(); 
	}
	
	protected boolean onActionWeb() { 
		Uri uri = getController().getWebpageUri();
		if (uri != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("onActionWeb: uri=" + uri);
			
	        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	        intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
	        startActivity(intent);
		}
		
		return true;
	}
	
	@Override
	public Intent createShareIntent() { 
		return getController().getShareIntent(); 
	}
	
	public void postSetShareIntent() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					setShareIntent();
				}
			});
	}
	
	@Override
	public void setShareIntent() { 
		setShareIntent(createShareIntent());
	}
	
	@Override
	protected void onInvokeByModel(int action, Object params) { 
		switch (action) { 
		case Model.ACTION_ONLOADERSTART:
		case InformationListModel.ACTION_ONFETCHSTART: 
			postShowProgress(false);
			break; 
		case Model.ACTION_ONLOADERSTOP:
		case InformationListModel.ACTION_ONFETCHSTOP: 
			postHideProgress(false);
			postSetShareIntent();
			break; 
		case InformationListModel.ACTION_ONCONTENTUPDATE: 
			if (params != null && params instanceof InformationSource)
				postUpdateContent((InformationSource)params);
			break; 
		default:
			super.onInvokeByModel(action, params);
			break;
		}
	}
	
	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		String location = getIntent().getStringExtra(EXTRA_LOCATION); 
		getController().initialize(getCallback(), location); 
        
        super.doOnCreate(savedInstanceState);
	}
	
	@Override
	protected boolean onFlingToRight() { 
		onBackPressed(); //onAnimationBack();
		return true; 
	}
	
	@Override
    public void onStart() {
        super.onStart();
        initContent(false);
	}
	
	//protected void onAnimationBack() { 
	//	onBackPressed();
	//	overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_right_exit); 
	//}
	
	@Override
	public boolean onActionHome() { 
		onBackPressed(); //onAnimationBack();
		return true;
	}
	
	@Override
	public boolean onActionRefresh() { 
		refreshContent(true);
		return true;
	}
	
	@Override
	public void setContentFragment() { 
		setContentFragment(new InformationFragment());
	}
	
	@Override
	public void refreshContent(boolean force) { 
		getController().refreshContent(getCallback(), force);
	}
	
	private void initContent(boolean force) { 
		if (mContentInited == false || force) {
			setContentFragment();
			mContentInited = true;
		}
	}
	
	private final void postUpdateContent(final InformationSource source) { 
		if (source == null || source != getController().getInformationSource()) 
			return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				public void run() { 
					source.getInformationDataSets().notifyDataSetChanged();
				}
			});
	}

}
