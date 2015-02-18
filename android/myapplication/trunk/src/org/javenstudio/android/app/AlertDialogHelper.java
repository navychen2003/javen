package org.javenstudio.android.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public abstract class AlertDialogHelper {

	public static interface IChoiceItem {
		public int getTextRes();
		public int getIconRes();
	}
	
	public void setDialogTitle(AlertDialog dialog, CharSequence title) {
		if (dialog != null) dialog.setTitle(title);
	}
	
	public void setDialogTitle(AlertDialog dialog, int titleRes) {
		if (dialog != null) dialog.setTitle(titleRes);
	}
	
	public void setDialogIcon(AlertDialog dialog, Drawable icon) {
		if (dialog != null) dialog.setIcon(icon);
	}
	
	public void setDialogIcon(AlertDialog dialog, int iconRes) {
		if (dialog != null) dialog.setIcon(iconRes);
	}
	
	public void setDialogIndicator(AlertDialog dialog, Drawable icon) {
	}
	
	public void setDialogIndicator(AlertDialog dialog, int iconRes) {
	}
	
	public void setDialogIconListener(AlertDialog dialog, 
			View.OnClickListener listener) {
	}
	
	public void setDialogTitleListener(AlertDialog dialog, 
			View.OnClickListener listener) {
	}
	
	public void setDialogRefreshListener(AlertDialog dialog, 
			View.OnClickListener listener, boolean showRefresh) {
	}
	
	public void showDialogProgressBar(AlertDialog dialog, 
			boolean showProgress, boolean showRefresh) {
	}
	
	protected void onAlertDialogPreCreate(AlertDialogBuilder builder) {}
	protected void onAlertDialogPreShow(AlertDialog dialog) {}
	protected void onAlertDialogShow(AlertDialog dialog) {}
	
	public TextView findTitleView(AlertDialog dialog) { return null; }
	public ImageView findIconView(AlertDialog dialog) { return null; }
	public ImageView findIndicatorView(AlertDialog dialog) { return null; }
	public ImageView findRefreshView(AlertDialog dialog) { return null; }
	
	public View findProgressView(AlertDialog dialog) { return null; }
	public View findIconContainer(AlertDialog dialog) { return null; }
	public View findTitleContainer(AlertDialog dialog) { return null; }
	public View findActionContainer(AlertDialog dialog) { return null; }
	
	public static ListAdapter createAdapter(Context context, IChoiceItem[] items) {
		ArrayAdapter<IChoiceItem> adapter = new ArrayAdapter<IChoiceItem>(context, 0, items) { 
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					IChoiceItem item = getItem(position);
					return getItemView(getContext(), item, convertView, parent);
				}
			};
		
		return adapter;
	}
	
	private static View getItemView(Context context, IChoiceItem item, 
			View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater.inflate(R.layout.dialog_item, null);
		
		final TextView textView = (TextView)view.findViewById(R.id.dialog_item_text);
		if (item != null && textView != null) { 
			textView.setText(item.getTextRes());
			textView.setCompoundDrawablesWithIntrinsicBounds(item.getIconRes(), 0, 0, 0);
		}
		
		return view;
	}
	
	public static ListAdapter createChoiceAdapter(Context context, IChoiceItem[] items) {
		ArrayAdapter<IChoiceItem> adapter = new ArrayAdapter<IChoiceItem>(context, 0, items) { 
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					IChoiceItem item = getItem(position);
					return getChoiceItemView(getContext(), item, convertView, parent);
				}
			};
		
		return adapter;
	}
	
	private static View getChoiceItemView(Context context, IChoiceItem item, 
			View convertView, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater.inflate(R.layout.dialog_singlechoice, null);
		
		final CheckedTextView textView = (CheckedTextView)view.findViewById(R.id.dialog_singlechoice_text);
		if (item != null && textView != null) { 
			textView.setText(item.getTextRes());
			textView.setCompoundDrawablesWithIntrinsicBounds(item.getIconRes(), 0, 0, 0);
			
			int checkmarkRes = AppResources.getInstance().getDrawableRes(AppResources.drawable.btn_dialog_checkmark);
			if (checkmarkRes != 0) textView.setCheckMarkDrawable(checkmarkRes);
		}
		
		return view;
	}
	
	public static void keepDialog(DialogInterface dialog, boolean keep) { 
		if (dialog == null) return;
		try { 
			java.lang.reflect.Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, !keep);
		} catch (Throwable e) { 
			// ignore
		}
	}
	
}
