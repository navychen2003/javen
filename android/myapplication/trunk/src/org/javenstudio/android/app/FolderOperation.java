package org.javenstudio.android.app;

import org.javenstudio.android.app.R;

public class FolderOperation extends FileOperation {

	private static final Operation[] sDefault = new Operation[] {
		Operation.MOVE, Operation.DELETE, Operation.RENAME, 
		Operation.MODIFY, Operation.DETAILS, Operation.SELECT
	};
	
	public FolderOperation() {
		this(sDefault);
	}
	
	public FolderOperation(Operation... supports) { 
		super(supports, new OperationItem[] { 
				new OperationItem(Operation.SELECT, R.string.label_operation_select, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_select)),
				new OperationItem(Operation.MOVE, R.string.label_operation_move, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_move)), 
				new OperationItem(Operation.DELETE, R.string.label_operation_delete, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_delete)), 
				new OperationItem(Operation.RENAME, R.string.label_operation_rename, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_rename)), 
				new OperationItem(Operation.MODIFY, R.string.label_operation_modify, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_modify)), 
				new OperationItem(Operation.DETAILS, R.string.label_operation_details, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_details)), 
			});
	}
	
}
