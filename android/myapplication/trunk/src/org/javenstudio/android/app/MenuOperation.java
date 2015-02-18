package org.javenstudio.android.app;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;

public class MenuOperation implements IMenuOperation {
	//private static final Logger LOG = Logger.getLogger(MenuOperation.class);

	public static class Operation implements IMenuOperation.IOperation { 
		private final int mItemId;
		private WeakReference<Activity> mActivityRef = null;
		private WeakReference<IMenuItem> mMenuItemRef = null;
		
		private final long mIdentity = ResourceHelper.getIdentity();
		public final long getIdentity() { return mIdentity; }
		
		public Operation(int itemId) { mItemId = itemId; }
		
		public int getItemId() { return mItemId; }
		public boolean isEnabled() { return false; }
		
		private synchronized IMenuItem findMenuItem(Activity activity, IMenu menu) { 
			if (activity == null || menu == null) 
				return null;
			
			if (mActivityRef != null) { 
				if (activity == mActivityRef.get()) { 
					if (mMenuItemRef != null) { 
						IMenuItem item = mMenuItemRef.get();
						if (item != null) 
							return item;
					}
				}
			}
			
			mActivityRef = new WeakReference<Activity>(activity);
			mMenuItemRef = null;
			
			IMenuItem item = menu.findItem(getItemId());
			if (item != null) 
				mMenuItemRef = new WeakReference<IMenuItem>(item);
			
			//if (LOG.isDebugEnabled()) {
			//	LOG.debug("findMenuItem: id=" + getIdentity() + " itemId=" + getItemId() 
			//			+ " activity=" + activity + " menuItem=" + item);
			//}
			
			return item;
		}
		
		@Override
		public final void onCreateOptionsMenu(Activity activity, IMenu menu) { 
			if (activity == null || menu == null) return;
			IMenuItem item = findMenuItem(activity, menu);
			if (item != null) item.setVisible(isEnabled());
		}
		
		@Override
		public boolean onOptionsItemSelected(Activity activity, IMenuItem item) { 
			return false; 
		}
		
		@Override
		public final void onUpdateContent(Activity activity, IMenu menu) { 
			if (activity == null || menu == null) return;
			IMenuItem item = findMenuItem(activity, menu);
			if (item != null) item.setVisible(isEnabled());
		}
	}
	
	private Map<Integer, IMenuOperation.IOperation> mOperations = null;
	private IMenuExecutor mMenuExecutor = null;
	
	public MenuOperation() {}
	
	public void setMenuExecutor(IMenuExecutor me) { mMenuExecutor = me; }
	public IMenuExecutor getMenuExecutor() { return mMenuExecutor; }
	
	@Override
	public void addOperation(IMenuOperation.IOperation... ops) { 
		if (ops == null) return;
		for (IMenuOperation.IOperation op : ops) { 
			addOperation(op);
		}
	}
	
	@Override
	public synchronized void addOperation(IMenuOperation.IOperation op) { 
		if (op == null) return;
		if (mOperations == null) 
			mOperations = new HashMap<Integer, IMenuOperation.IOperation>();
		
		mOperations.put(op.getItemId(), op);
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("addMenuItem: itemId=" + op.getItemId() + " op=" + op);
	}
	
	@Override
	public synchronized void onCreateOptionsMenu(Activity activity, IMenu menu) {
		if (activity == null || menu == null || mOperations == null) 
			return;
		
		for (IMenuOperation.IOperation op : mOperations.values()) { 
			if (op == null) continue;
			op.onCreateOptionsMenu(activity, menu);
		}
	}
	
	@Override
	public synchronized boolean onOptionsItemSelected(Activity activity, IMenuItem item) {
		if (activity == null || item == null || mOperations == null) 
			return false;
		
		IMenuOperation.IOperation op = mOperations.get(item.getItemId());
		if (op != null) 
			return op.onOptionsItemSelected(activity, item);
		
		return false;
	}
	
	@Override
	public synchronized void onUpdateContent(Activity activity, IMenu menu) {
		if (activity == null || menu == null || mOperations == null) 
			return;
		
		for (IMenuOperation.IOperation op : mOperations.values()) { 
			if (op == null) continue;
			op.onUpdateContent(activity, menu);
		}
	}
	
}
