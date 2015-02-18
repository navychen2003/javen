package org.javenstudio.android.app;

import android.app.Activity;

import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.common.util.Logger;

public class TaskOperation extends MenuOperation.Operation {
	private static final Logger LOG = Logger.getLogger(TaskOperation.class);
	
	public TaskOperation(int itemId) { 
		super(itemId);
	}
	
	@Override
	public boolean isEnabled() { return true; }
	
	@Override
	public boolean onOptionsItemSelected(final Activity activity, IMenuItem item) { 
		if (activity == null || item == null) 
			return false;
		
		if (item.getItemId() != getItemId()) { 
			//if (LOG.isDebugEnabled()) { 
			//	LOG.debug("onOptionsItemSelected: activity=" + activity + " item=" + item 
			//			+ ", itemId=" + item.getItemId() + " not equals to operationItemId=" + getItemId());
			//}
			
			return false;
		}
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("onOptionsItemSelected: activity=" + activity + " item=" + item 
					+ " itemId=" + item.getItemId());
		}
		
		//if (activity instanceof IActivity) { 
		//	//IActivity a = (IActivity)activity;
		//}
		
		onActionTask(activity);
		
		return true;
	}
	
	protected void onActionTask(Activity activity) {}
	
}
