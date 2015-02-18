package org.javenstudio.android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ListAdapter;

public final class AlertDialogBuilder extends AlertDialog.Builder {

	private final AlertDialogHelper mHelper;
	private DialogInterface.OnShowListener mOnShowListener = null;
	private View.OnClickListener mRefreshListener = null;
	
	private CharSequence mTitle = null;
	private CharSequence mMessage = null;
	private Drawable mIcon = null;
	private int mIconRes = 0;
	private int mIconAttributeRes = 0;
	
	public AlertDialogBuilder(Activity activity, AlertDialogHelper helper) {
		super(activity);
		if (helper == null) throw new NullPointerException();
		mHelper = helper;
	}
	
	public AlertDialogBuilder(Activity activity, AlertDialogHelper helper, int theme) {
		super(activity, theme);
		if (helper == null) throw new NullPointerException();
		mHelper = helper;
	}
	
	public AlertDialogHelper getHelper() { return mHelper; }
	public int getIconRes() { return mIconRes; }
	public Drawable getIcon() { return mIcon; }
	public int getIconAttributeRes() { return mIconAttributeRes; }
	public CharSequence getTitle() { return mTitle; }
	public CharSequence getMessage() { return mMessage; }
	
	public DialogInterface.OnShowListener getOnShowListener() {
		return mOnShowListener;
	}
	
	public void setOnShowListener(DialogInterface.OnShowListener listener) {
		mOnShowListener = listener;
	}
	
	public View.OnClickListener getRefreshListener() {
		return mRefreshListener;
	}
	
	public void setRefreshListener(View.OnClickListener listener) {
		mRefreshListener = listener;
	}
	
	@Override
	public AlertDialogBuilder setIcon(int iconId) {
		super.setIcon(iconId);
		mIconRes = iconId;
		return this;
	}
	
	@Override
	public AlertDialogBuilder setIcon(Drawable icon) {
		super.setIcon(icon);
		mIcon = icon;
		return this;
	}
	
	@Override
	public AlertDialogBuilder setIconAttribute(int attrId) {
		super.setIconAttribute(attrId);
		mIconAttributeRes = attrId;
		return this;
	}
	
	@Override
	public AlertDialogBuilder setTitle(int titleId) {
		super.setTitle(titleId);
		mTitle = getContext().getText(titleId);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setTitle(CharSequence title) {
		super.setTitle(title);
		mTitle = title;
		return this;
	}
	
	@Override
	public AlertDialogBuilder setMessage(int messageId) {
		super.setMessage(messageId);
		mMessage = getContext().getText(messageId);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setMessage(CharSequence message) {
		super.setMessage(message);
		mMessage = message;
		return this;
	}
	
	@Override
	public AlertDialogBuilder setView(View view) {
		super.setView(view);
		return this;
	}
	
	//@Override
	//public AlertDialogBuilder setView(View view, int viewSpacingLeft, int viewSpacingTop,
    //        int viewSpacingRight, int viewSpacingBottom) {
	//	super.setView(view, viewSpacingLeft, viewSpacingTop, viewSpacingRight, viewSpacingBottom);
	//	return this;
	//}
	
	@Override
	public AlertDialogBuilder setAdapter(final ListAdapter adapter, 
			final DialogInterface.OnClickListener listener) {
		super.setAdapter(adapter, listener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setSingleChoiceItems(ListAdapter adapter, 
			int checkedItem, final DialogInterface.OnClickListener listener) {
		super.setSingleChoiceItems(adapter, checkedItem, listener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setCancelable(boolean cancelable) {
		super.setCancelable(cancelable);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setOnCancelListener(
			DialogInterface.OnCancelListener onCancelListener) {
		super.setOnCancelListener(onCancelListener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setOnDismissListener(
			DialogInterface.OnDismissListener onDismissListener) {
		super.setOnDismissListener(onDismissListener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setOnKeyListener(
			DialogInterface.OnKeyListener onKeyListener) {
		super.setOnKeyListener(onKeyListener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setPositiveButton(int textId, 
			final DialogInterface.OnClickListener listener) {
		super.setPositiveButton(textId, listener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setPositiveButton(CharSequence text, 
			final DialogInterface.OnClickListener listener) {
		super.setPositiveButton(text, listener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setNegativeButton(int textId, 
			final DialogInterface.OnClickListener listener) {
		super.setNegativeButton(textId, listener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setNegativeButton(CharSequence text, 
			final DialogInterface.OnClickListener listener) {
		super.setNegativeButton(text, listener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setNeutralButton(int textId, 
			final DialogInterface.OnClickListener listener) {
		super.setNeutralButton(textId, listener);
		return this;
	}
	
	@Override
	public AlertDialogBuilder setNeutralButton(CharSequence text, 
			final DialogInterface.OnClickListener listener) {
		super.setNeutralButton(text, listener);
		return this;
	}
	
	@Override
	public AlertDialog show() {
		AlertDialog dialog = create();
		if (dialog != null) {
			dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						mHelper.onAlertDialogShow((AlertDialog)dialog);
						DialogInterface.OnShowListener listener = mOnShowListener;
						if (listener != null) listener.onShow(dialog);
					}
				});
			mHelper.onAlertDialogPreShow(dialog);
			dialog.show();
		}
		return dialog;
	}
	
	@Override
	public AlertDialog create() {
		mHelper.onAlertDialogPreCreate(this);
		return super.create();
	}
	
	public AlertDialog show(Activity activity) {
		final IActivity iactivity = (activity != null && activity instanceof IActivity) ? 
				(IActivity) activity : null;
		if (iactivity != null) 
			iactivity.getActivityHelper().lockOrientationOnDialogShowing();
		
		AlertDialog dialog = show();
		if (dialog != null) { 
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						if (iactivity != null)
							iactivity.getActivityHelper().unlockOrientationOnDialogDismiss();
					}
				});
		}
		
		return dialog;
	}
	
}
