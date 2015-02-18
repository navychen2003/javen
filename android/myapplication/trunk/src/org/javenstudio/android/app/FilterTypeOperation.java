package org.javenstudio.android.app;

import android.app.Activity;
import android.content.DialogInterface;

import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.common.util.Logger;

public class FilterTypeOperation extends MenuOperation.Operation {
	private static final Logger LOG = Logger.getLogger(FilterTypeOperation.class);
	
	private final FilterType mTypes;
	
	public FilterTypeOperation(FilterType types, int itemId) { 
		super(itemId);
		mTypes = types;
	}
	
	@Override
	public boolean isEnabled() { return true; }
	
	public FilterType getFilterType() { return mTypes; }
	
	public static int getCheckedItem(FilterType types, 
			FilterType.TypeItem[] items) { 
		if (types != null && items != null) { 
			for (int i=0; i < items.length; i++) { 
				FilterType.TypeItem item = items[i];
				if (item.mType == types.getSelectType()) { 
					return i;
				}
			}
		}
		return 0;
	}
	
	public static FilterType.Type setCheckedItem(FilterType types, 
			FilterType.TypeItem[] items, int which) { 
		if (types != null && items != null) { 
			for (int i=0; i < items.length; i++) { 
				FilterType.TypeItem item = items[i];
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
		
		return showFilterTypeDialog(activity, mTypes, null);
	}
	
	public static boolean showFilterTypeDialog(final Activity activity, 
			final FilterType types, final FilterType.OnChangeListener listener) {
		if (types == null || activity == null || activity.isDestroyed()) 
			return false;
		
		final FilterType.TypeItem[] items = types.getItems();
		if (LOG.isDebugEnabled()) {
			LOG.debug("showFilterTypeDialog: activity=" + activity + " types=" + types 
					+ " itemCount=" + (items != null ? items.length : 0));
		}
		
		int titleRes = AppResources.getInstance().getStringRes(AppResources.string.section_filterby_dialog_title);
		if (titleRes == 0) titleRes = R.string.dialog_filterby_title;
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		builder.setTitle(titleRes);
		builder.setSingleChoiceItems(
			AlertDialogHelper.createChoiceAdapter(activity, items), 
			getCheckedItem(types, items), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					FilterType.Type type = setCheckedItem(types, items, which);
					dialog.dismiss();
					if (listener != null) listener.onChangeFilterType(type);
				}
			});
		
		builder.show(activity);
		
		return true; 
	}
	
}
