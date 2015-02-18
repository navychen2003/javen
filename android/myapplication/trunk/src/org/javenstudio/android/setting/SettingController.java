package org.javenstudio.android.setting;

import android.app.Application;
import android.content.Context;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.model.BaseController;
import org.javenstudio.cocoka.widget.model.BaseModel;
import org.javenstudio.cocoka.widget.model.Controller;

public class SettingController extends BaseController {
	
	private static SettingController sInstance = null; 
	private static final Object sLock = new Object(); 
	
	public static SettingController getInstance() { 
		synchronized (sLock) { 
			if (sInstance == null) {
				sInstance = new SettingController(
						ResourceHelper.getApplication(), ResourceHelper.getContext()); 
			}
			return sInstance; 
		}
	}
	
	private final Context mContext; 
	private final SettingModel mModel;
	
	private SettingController(Application app, Context context) { 
		mContext = context; 
		mModel = new SettingModel(app);
	}
	
	@Override 
	public Context getContext() { 
		return mContext; 
	}

	@Override
	public SettingModel getModel() {
		return mModel;
	}
	
	public static class SettingModel extends BaseModel {

		private SettingModel(Application app) { 
			super(app); 
		}

		@Override
		protected void runWorkOnThread(Controller controller, Callback callback) {
		}
	}
	
}
