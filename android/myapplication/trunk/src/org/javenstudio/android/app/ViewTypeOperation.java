package org.javenstudio.android.app;

import android.app.Activity;
import android.content.DialogInterface;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.common.util.Logger;

public class ViewTypeOperation extends MenuOperation.Operation {
	private static final Logger LOG = Logger.getLogger(ViewTypeOperation.class);
	
	private final ViewType mTypes;
	
	public ViewTypeOperation(ViewType types, int itemId) { 
		super(itemId);
		mTypes = types;
	}
	
	@Override
	public boolean isEnabled() { return true; }
	
	public ViewType getViewType() { return mTypes; }
	
	public static int getCheckedItem(ViewType types, 
			ViewType.TypeItem[] items) { 
		if (types != null && items != null) { 
			for (int i=0; i < items.length; i++) { 
				ViewType.TypeItem item = items[i];
				if (item.mType == types.getSelectType()) { 
					return i;
				}
			}
		}
		return 0;
	}
	
	public static void setCheckedItem(ViewType types, 
			ViewType.TypeItem[] items, int which) { 
		if (types != null && items != null) { 
			for (int i=0; i < items.length; i++) { 
				ViewType.TypeItem item = items[i];
				if (which == i) { 
					types.setSelectType(item.mType);
					return;
				}
			}
		}
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
		
		return showViewTypeDialog(activity, mTypes);
	}
	
	public static boolean showViewTypeDialog(final Activity activity, 
			final ViewType types) {
		if (types == null || activity == null || activity.isDestroyed()) 
			return false;
		
		final ViewType.TypeItem[] items = types.getItems();
		if (LOG.isDebugEnabled()) {
			LOG.debug("showViewTypeDialog: activity=" + activity + " types=" + types
					+ " itemCount=" + (items != null ? items.length : 0));
		}
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(true);
		builder.setTitle(R.string.label_action_viewtype);
		builder.setSingleChoiceItems(
			AlertDialogHelper.createChoiceAdapter(activity, items), 
			getCheckedItem(types, items), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setCheckedItem(types, items, which);
					dialog.dismiss();
					//if (activity != null && activity instanceof IActivity) { 
					//	((IActivity)activity).setContentFragment();
					//}
				}
			});
		
		builder.show(activity);
		
		return true; 
	}
	
}
