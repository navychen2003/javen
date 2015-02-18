package org.javenstudio.android.app;

import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.util.Logger;

public class SelectManager {
	private static final Logger LOG = Logger.getLogger(SelectManager.class);

	public static interface SelectData {
		public String getName();
		public boolean supportOperation(FileOperation.Operation op);
	}
	
	public static interface ChangeListener { 
		public void onSelectChanged(SelectManager manager);
	}
	
	private final Set<SelectData> mSelectedItems;
	private ChangeListener mListener;
	
	public SelectManager() { 
		this(null);
	}
	
	public SelectManager(ChangeListener listener) { 
		mSelectedItems = new HashSet<SelectData>();
		mListener = listener;
	}
	
	public synchronized void setChangeListener(ChangeListener listener) { 
		mListener = listener;
	}
	
	public synchronized boolean isSelectedItem(SelectData data) { 
		if (data == null) return false;
		
		synchronized (mSelectedItems) { 
			return mSelectedItems.contains(data);
		}
	}
	
	public synchronized void setSelectedItem(SelectData data, 
			boolean selected) { 
		if (data == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("setSelectedItem: data=" + data + " selected=" + selected);
		
		synchronized (mSelectedItems) { 
			if (selected) 
				mSelectedItems.add(data);
			else
				mSelectedItems.remove(data);
		}
		
		ChangeListener listener = mListener;
		if (listener != null) 
			listener.onSelectChanged(this);
	}
	
	public synchronized SelectData[] getSelectedItems() { 
		synchronized (mSelectedItems) { 
			return mSelectedItems.toArray(new SelectData[mSelectedItems.size()]);
		}
	}
	
	public synchronized int getSelectedCount() { 
		synchronized (mSelectedItems) { 
			return mSelectedItems.size();
		}
	}
	
	public synchronized void clearSelectedItems() { 
		if (LOG.isDebugEnabled())
			LOG.debug("clearSelectedItems: count=" + getSelectedCount());
		
		synchronized (mSelectedItems) { 
			mSelectedItems.clear();
		}
		
		ChangeListener listener = mListener;
		if (listener != null) 
			listener.onSelectChanged(this);
	}
	
}
