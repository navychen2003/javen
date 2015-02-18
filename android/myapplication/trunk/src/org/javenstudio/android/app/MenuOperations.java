package org.javenstudio.android.app;

import android.app.Activity;

import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;

public class MenuOperations implements IMenuOperation {

	private final IMenuOperation[] mOperations;
	
	public MenuOperations() { 
		mOperations = null;
	}
	
	public MenuOperations(IMenuOperation... ops) { 
		mOperations = ops;
	}
	
	public IMenuOperation[] getOperations() { 
		return mOperations;
	}
	
	@Override
	public IMenuExecutor getMenuExecutor() {
		IMenuOperation[] mops = getOperations();
		for (int i=0; mops != null && i < mops.length; i++) { 
			IMenuOperation mo = mops[i];
			if (mo == null) continue;
			
			IMenuExecutor me = mo.getMenuExecutor();
			if (me != null) return me;
		}
		return null;
	}

	@Override
	public void addOperation(IOperation op) {
		IMenuOperation[] mops = getOperations();
		for (int i=0; mops != null && i < mops.length; i++) { 
			IMenuOperation mo = mops[i];
			if (mo == null) continue;
			
			mo.addOperation(op);
			break;
		}
	}

	@Override
	public void addOperation(IOperation... ops) {
		IMenuOperation[] mops = getOperations();
		for (int i=0; mops != null && i < mops.length; i++) { 
			IMenuOperation mo = mops[i];
			if (mo == null) continue;
			
			mo.addOperation(ops);
			break;
		}
	}

	@Override
	public void onCreateOptionsMenu(Activity activity, IMenu menu) {
		IMenuOperation[] mops = getOperations();
		for (int i=0; mops != null && i < mops.length; i++) { 
			IMenuOperation mo = mops[i];
			if (mo == null) continue;
			
			mo.onCreateOptionsMenu(activity, menu);
		}
	}

	@Override
	public void onUpdateContent(Activity activity, IMenu menu) {
		IMenuOperation[] mops = getOperations();
		for (int i=0; mops != null && i < mops.length; i++) { 
			IMenuOperation mo = mops[i];
			if (mo == null) continue;
			
			mo.onUpdateContent(activity, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(Activity activity, IMenuItem item) {
		IMenuOperation[] mops = getOperations();
		for (int i=0; mops != null && i < mops.length; i++) { 
			IMenuOperation mo = mops[i];
			if (mo == null) continue;
			
			if (mo.onOptionsItemSelected(activity, item))
				return true;
		}
		
		return false;
	}

}
