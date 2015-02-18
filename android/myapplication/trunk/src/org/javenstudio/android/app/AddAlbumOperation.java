package org.javenstudio.android.app;

import android.app.Activity;

import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.common.util.Logger;

public class AddAlbumOperation extends MenuOperation.Operation {
	private static final Logger LOG = Logger.getLogger(AddAlbumOperation.class);
	
	public static interface ClickListener { 
		public void onAddAlbumClick(Activity activity);
	}
	
	private final ClickListener mListener;
	
	public AddAlbumOperation(ClickListener listener, int itemId) { 
		super(itemId);
		mListener = listener;
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
		
		ClickListener listener = mListener;
		if (listener != null) 
			listener.onAddAlbumClick(activity);
		
		return true;
	}
	
}
