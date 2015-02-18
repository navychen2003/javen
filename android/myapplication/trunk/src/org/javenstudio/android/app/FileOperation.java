package org.javenstudio.android.app;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class FileOperation {
	private static final Logger LOG = Logger.getLogger(FileOperation.class);

	public static enum Operation {
		SELECT, MOVE, DELETE, RENAME, MODIFY, SHARE, OPEN, DETAILS, 
		DOWNLOAD, CHANGEPOSTER
	}
	
	private static final Operation[] sDefault = new Operation[] {
		Operation.MOVE, Operation.DELETE, Operation.RENAME, 
		Operation.MODIFY, Operation.SHARE, Operation.OPEN, 
		Operation.DETAILS, Operation.SELECT
	};
	
	static class OperationItem implements AlertDialogHelper.IChoiceItem {
		public final Operation mOperation;
		public final int mTextRes;
		public final int mIconRes;
		
		public OperationItem(Operation op, int textRes, int iconRes) { 
			mOperation = op;
			mTextRes = textRes; 
			mIconRes = iconRes;
		}
		
		public int getTextRes() { return mTextRes; }
		public int getIconRes() { return mIconRes; }
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	
	private final OperationItem[] mItems;
	private final Object mLock = new Object();
	private final Operation[] mSupports;
	
	public FileOperation() {
		this(sDefault);
	}
	
	public FileOperation(Operation... supports) { 
		mSupports = supports;
		mItems = new OperationItem[] { 
				new OperationItem(Operation.SELECT, R.string.label_operation_select, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_select)), 
				new OperationItem(Operation.OPEN, R.string.label_operation_open, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_open)), 
				new OperationItem(Operation.MOVE, R.string.label_operation_move, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_move)), 
				new OperationItem(Operation.DELETE, R.string.label_operation_delete, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_delete)), 
				new OperationItem(Operation.RENAME, R.string.label_operation_rename, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_rename)), 
				new OperationItem(Operation.MODIFY, R.string.label_operation_modify, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_modify)), 
				new OperationItem(Operation.SHARE, R.string.label_operation_share, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_share)), 
				new OperationItem(Operation.DOWNLOAD, R.string.label_operation_download, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_download)), 
				new OperationItem(Operation.DETAILS, R.string.label_operation_details, 
						AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_menu_operation_details)), 
			};
	}
	
	protected FileOperation(Operation[] supports, OperationItem[] items) { 
		mSupports = supports;
		mItems = items;
	}
	
	public OperationItem[] getItems() { 
		synchronized (mLock) { 
			List<OperationItem> items = new ArrayList<OperationItem>();
			for (OperationItem item : mItems) { 
				if (mSupports != null) { 
					for (Operation op : mSupports) { 
						if (op == item.mOperation) { 
							items.add(item);
							break;
						}
					}
				}
			}
			return items.toArray(new OperationItem[items.size()]);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + mIdentity + "{}";
	}
	
	public static interface OnSelectListener {
		public void onOperationSelected(Operation op);
	}
	
	public static interface OnShowListener {
		public void onDialogShow(AlertDialogBuilder builder, DialogInterface dialog);
	}
	
	public static boolean showOperationDialog(final Activity activity, 
			final FileOperation ops, final CharSequence title, Drawable icon, 
			final OnSelectListener selectListener, final OnShowListener showListener) {
		if (ops == null || activity == null || activity.isDestroyed()) 
			return false;
		
		final OperationItem[] items = ops.getItems();
		if (LOG.isDebugEnabled()) {
			LOG.debug("showOperationDialog: activity=" + activity + " ops=" + ops 
					+ " itemCount=" + (items != null ? items.length : 0));
		}
		
		int titleRes = AppResources.getInstance().getStringRes(AppResources.string.section_sortby_dialog_title);
		if (titleRes == 0) titleRes = R.string.dialog_sortby_title;
		
		final AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		builder.setTitle(title);
		builder.setIcon(icon);
		builder.setAdapter(
			AlertDialogHelper.createAdapter(activity, items), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Operation op = getSelectedOperation(ops, items, which);
					dialog.dismiss();
					if (selectListener != null) selectListener.onOperationSelected(op);
				}
			});
		
		builder.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					if (showListener != null) showListener.onDialogShow(builder, dialog);
				}
			});
		
		builder.show(activity);
		
		return true; 
	}
	
	public static Operation getSelectedOperation(FileOperation ops, 
			OperationItem[] items, int which) { 
		if (ops != null && items != null) { 
			for (int i=0; i < items.length; i++) { 
				OperationItem item = items[i];
				if (which == i) { 
					return item.mOperation;
				}
			}
		}
		return null;
	}
	
}
