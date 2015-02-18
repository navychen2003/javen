package org.javenstudio.android.app;

import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataException;

public interface SelectAction {

	public int getSelectItemCount(SelectManager.SelectData[] items);
	
	public void onSelectChanged(SelectMode mode, SelectManager manager);
	
	public void executeAction(DataAction action, SelectManager.SelectData item, 
			ActionExecutor.ProgressListener listener) throws DataException;
	
	public CharSequence getActionConfirmTitle(DataAction action, 
			SelectManager manager);
	
	public CharSequence getActionConfirmMessage(DataAction action, 
			SelectManager manager);
	
}
