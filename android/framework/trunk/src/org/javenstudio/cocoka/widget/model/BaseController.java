package org.javenstudio.cocoka.widget.model;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class BaseController extends Controller {
	private static final Logger LOG = Logger.getLogger(BaseController.class);
	
	public BaseController() {}
	
	public void refreshContent(final Model.Callback callback, final boolean force) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					doRefreshContent(callback, force);
				}
			});
	}
	
	private void doRefreshContent(Model.Callback callback, boolean force) { 
		Model model = getModel();
		if (model != null && model instanceof BaseModel/* && !model.isLoaderRunning()*/) {
			if (LOG.isDebugEnabled())
				LOG.debug("refreshContent: force=" + force + " running=" + model.isLoaderRunning());
			
			((BaseModel)model).startLoad(callback, force); 
		}
	}
	
	public void abortRefresh() { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					doAbortRefresh();
				}
			});
	}
	
	private void doAbortRefresh() { 
		Model model = getModel();
		if (model != null && model instanceof BaseModel) {
			if (LOG.isDebugEnabled())
				LOG.debug("abortRefresh");
			
			((BaseModel)model).stopLoad(); 
		}
	}
	
}
