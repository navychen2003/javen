package org.javenstudio.cocoka.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class PopupDialog implements DialogInterface.OnClickListener,
			DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

	private final Activity mActivity; 
	private final PopupDialogAdapter mAdapter; 
	private final int mDialogId; 
	private final int mTitleId; 
	
	public PopupDialog(Activity activity, int dialogId, int itemLayoutId, int titleId) { 
		this(activity, dialogId, itemLayoutId, titleId, null);
	}
	
	public PopupDialog(Activity activity, int dialogId, int itemLayoutId, int titleId, 
			PopupDialogAdapter.ListItem[] items) {
		mActivity = activity; 
		mDialogId = dialogId; 
		mTitleId = titleId; 
		mAdapter = newPopupMenuAdapter(activity, dialogId, itemLayoutId, items);
	}
	
	public final Activity getActivity() { 
		return mActivity;
	}
	
	public int getScreenWidth() { 
		return getActivity().getResources().getDisplayMetrics().widthPixels; 
	}
	
	public int getScreenHeight() { 
		return getActivity().getResources().getDisplayMetrics().heightPixels; 
	}
	
	protected PopupDialogAdapter newPopupMenuAdapter(Context context, int dialogId, int itemLayoutId, PopupDialogAdapter.ListItem[] items) { 
		return new PopupDialogAdapter(context, dialogId, itemLayoutId, items); 
	}
	
	public void addMenuItem(PopupDialogAdapter.ListItem item) { 
		mAdapter.addItem(item);
	}
	
	public void setMenuViewBinder(PopupDialogAdapter.ViewBinder binder) { 
		mAdapter.setViewBinder(binder);
	}
	
	protected class MenuDialog extends SimpleDialog { 
		protected MenuDialog(Context context, int theme, int contentView) {
			super(context, theme, contentView);
		}
		
		@Override
	    public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState); 
			
			onSetTitle(mTitleId);
		}
		
		protected void onSetTitle(int titleId) { 
			// do nothing
		}
	}
	
	protected MenuDialog newMenuDialog(int theme, int contentViewId) { 
		return new MenuDialog(getActivity(), theme, contentViewId); 
	}
	
	public final Dialog createDialog(int theme, int contentViewId, final int listViewId) { 
		final MenuDialog dialog = newMenuDialog(theme, contentViewId); 
		
		dialog.setOnCreatedListener(new SimpleDialog.OnCreatedListener() {
				@Override
				public void onDialogCreated(SimpleDialog d) {
					ListView listView = (ListView)dialog.findViewById(listViewId);
					if (listView != null) { 
						listView.setAdapter(mAdapter);
						listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
								onClick(dialog, position);
							}
						});
					}
				}
			});
		
		dialog.setOnCancelListener(this);
        dialog.setOnDismissListener(this);
        //dialog.setOnShowListener(this);
		
		return dialog;
	}
	
	public final AlertDialog createAlertDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mTitleId);
        builder.setAdapter(mAdapter, this);
        builder.setInverseBackgroundForced(true);
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnCancelListener(this);
        dialog.setOnDismissListener(this);
        //dialog.setOnShowListener(this);

        return dialog;
	}
	
	@Override
    public void onCancel(DialogInterface dialog) {
        cleanup();
    }

	@Override
    public void onDismiss(DialogInterface dialog) {
    }

    private void cleanup() {
        try {
        	mActivity.dismissDialog(mDialogId);
        	mActivity.removeDialog(mDialogId);
        } catch (Exception e) {
            // An exception is thrown if the dialog is not visible, which is fine
        	//Log.w(TAG, "cleanup dialog error", e); 
        }
    }
    
    public void onShow(DialogInterface dialog) {
    }
    
    /**
     * Handle the action clicked in the "Add to home" dialog.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
    	dialog.dismiss();
        cleanup();

        PopupDialogAdapter.Callback callback = mAdapter.getItemCallback(which); 
        if (callback != null) 
        	callback.onItemClick(); 
    }
}
