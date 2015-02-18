package org.javenstudio.provider.account.host;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AlertDialogHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class BaseDialogHelper {
	private static final Logger LOG = Logger.getLogger(BaseDialogHelper.class);

	protected AlertDialogBuilder mBuilder = null;
	protected AlertDialog mDialog = null;
	
	public AlertDialogBuilder getBuilder() { return mBuilder; }
	public AlertDialog getDialog() { return mDialog; }
	public boolean isDialogShown() { return mDialog != null; }
	
	public void dismissDialog() { 
		AlertDialog dialog = mDialog;
		if (dialog != null) {
			AlertDialogHelper.keepDialog(dialog, false);
			dialog.dismiss();
		}
	}
	
	public void postDismissDialog() {
		if (isDialogShown() == false) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					dismissDialog();
				}
			});
	}
	
	protected void onDialogDismiss(AlertDialogBuilder builder, 
			DialogInterface dialog) {}
	
	protected void onDialogShow(AlertDialogBuilder builder, 
			DialogInterface dialog) {}
	
	protected void onDialogRefresh(AlertDialogBuilder builder) {
		//refreshDataSets();
	}
	
	public void setDialogTitle(CharSequence title, Drawable icon, 
			View.OnClickListener iconListener, View.OnClickListener refreshListener) {
		AlertDialogBuilder builder = mBuilder;
		AlertDialog dialog = mDialog;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogTitle: dialog=" + dialog + " title=" + title 
					+ " iconListener=" + iconListener + " refreshListener=" + refreshListener);
		}
		
		if (dialog != null && builder != null) {
			builder.getHelper().setDialogTitle(dialog, title);
			builder.getHelper().setDialogIcon(dialog, icon);
			builder.getHelper().setDialogIconListener(dialog, iconListener);
			builder.getHelper().setDialogIndicator(dialog, iconListener != null ? 
					AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_dialog_back_indicator) :
						0);
			builder.getHelper().setDialogRefreshListener(dialog, 
					refreshListener, refreshListener != null);
			onHostDialogTitleChanged(builder, dialog);
		}
	}
	
	protected void onHostDialogTitleChanged(AlertDialogBuilder builder, 
			AlertDialog dialog) {}
	
	public void postSetHostDialogTitle(final CharSequence title, 
			final Drawable icon, final View.OnClickListener iconListener, 
			final View.OnClickListener refreshListener) {
		if (isDialogShown() == false) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					setDialogTitle(title, icon, iconListener, refreshListener);
				}
			});
	}
	
	public void setDialogTitle(CharSequence title) {
		AlertDialogBuilder builder = mBuilder;
		AlertDialog dialog = mDialog;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("setDialogTitle: dialog=" + dialog + " title=" + title);
		}
		
		if (dialog != null && builder != null) {
			builder.getHelper().setDialogTitle(dialog, title);
		}
	}
	
	public void postSetDialogTitle(final CharSequence title) {
		if (isDialogShown() == false) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					setDialogTitle(title);
				}
			});
	}
	
	public void showDialogProgress(boolean show, boolean showRefresh) {
		AlertDialogBuilder builder = mBuilder;
		AlertDialog dialog = mDialog;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("showDialogProgress: dialog=" + dialog 
					+ " show=" + show);
		}
		
		if (dialog != null && builder != null) {
			builder.getHelper().showDialogProgressBar(dialog, show, 
					!show && showRefresh);
		}
	}
	
	public void postShowDialogProgress(final boolean show, 
			final boolean showRefresh) {
		if (isDialogShown() == false) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					showDialogProgress(show, showRefresh);
				}
			});
	}
	
}
