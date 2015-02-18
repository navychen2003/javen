package org.javenstudio.provider;

import android.app.Application;
import android.content.Context;

import org.javenstudio.cocoka.widget.model.BaseController;

public class ProviderController extends BaseController {
	
	private final Context mContext; 
	private final ProviderModel mModel;
	private Provider mProvider = null;
	
	public ProviderController(Application app, Context context) { 
		mContext = context; 
		mModel = new ProviderModel(app);
	}
	
	@Override 
	public Context getContext() { 
		return mContext; 
	}

	@Override
	public ProviderModel getModel() {
		return mModel;
	}
	
	public void setProvider(Provider p) { mProvider = p; }
	
	public Provider getProvider() { 
		if (mProvider != null) return mProvider; 
		return ProviderManager.getDefaultProvider();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{provider=" + getProvider() + "}";
	}
	
}
