package org.javenstudio.android.app;

import android.app.Activity;
import android.content.DialogInterface;

import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.common.util.Logger;

public class SortTypeOperation extends MenuOperation.Operation {
	private static final Logger LOG = Logger.getLogger(SortTypeOperation.class);
	
	private final SortType mTypes;
	
	public SortTypeOperation(SortType types, int itemId) { 
		super(itemId);
		mTypes = types;
	}
	
	@Override
	public boolean isEnabled() { return true; }
	
	public SortType getSortType() { return mTypes; }
	
	public static int getCheckedItem(SortType types, 
			SortType.TypeItem[] items) { 
		if (types != null && items != null) { 
			for (int i=0; i < items.length; i++) { 
				SortType.TypeItem item = items[i];
				if (item.mType == types.getSelectType()) { 
					return i;
				}
			}
		}
		return 0;
	}
	
	public static SortType.Type setCheckedItem(SortType types, 
			SortType.TypeItem[] items, int which) { 
		if (types != null && items != null) { 
			for (int i=0; i < items.length; i++) { 
				SortType.TypeItem item = items[i];
				if (which == i) { 
					types.setSelectType(item.mType);
					return item.mType;
				}
			}
		}
		return null;
	}
	
	@Override
	public boolean onOptionsItemSelected(final Activity activity, 
			IMenuItem item) { 
		if (activity == null || item == null) 
			return false;
		
		if (item.getItemId() != getItemId()) { 
			//if (LOG.isDebugEnabled()) { 
			//	LOG.debug("onOptionsItemSelected: activity=" + activity + " item=" + item 
			//			+ ", itemId=" + item.getItemId() + " not equals to operationItemId=" + getItemId());
			//}
			
			return false;
		}
		
		return showSortTypeDialog(activity, mTypes, null);
	}
	
	public static boolean showSortTypeDialog(final Activity activity, 
			final SortType types, final SortType.OnChangeListener listener) {
		if (types == null || activity == null || activity.isDestroyed()) 
			return false;
		
		final SortType.TypeItem[] items = types.getItems();
		if (LOG.isDebugEnabled()) {
			LOG.debug("showSortTypeDialog: activity=" + activity + " types=" + types 
					+ " itemCount=" + (items != null ? items.length : 0));
		}
		
		int titleRes = AppResources.getInstance().getStringRes(AppResources.string.section_sortby_dialog_title);
		if (titleRes == 0) titleRes = R.string.dialog_sortby_title;
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		builder.setTitle(titleRes);
		builder.setSingleChoiceItems(
			AlertDialogHelper.createChoiceAdapter(activity, items), 
			getCheckedItem(types, items), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SortType.Type type = setCheckedItem(types, items, which);
					dialog.dismiss();
					if (listener != null) listener.onChangeSortType(type);
				}
			});
		
		builder.show(activity);
		
		return true; 
	}
	
}
