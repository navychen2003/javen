package org.javenstudio.provider.app.picasa;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.app.ActionExecutor;
import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AlertDialogHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.app.BaseAlbumProvider;
import org.javenstudio.provider.media.MediaAlbumFactory;
import org.javenstudio.provider.media.MediaAlbumSource;
import org.javenstudio.provider.media.album.AlbumBinder;
import org.javenstudio.provider.media.album.AlbumSource;
import org.javenstudio.util.StringUtils;

public class PicasaAlbumProvider extends BaseAlbumProvider {
	private static final Logger LOG = Logger.getLogger(PicasaAlbumProvider.class);
	
	public PicasaAlbumProvider(String name, int iconRes, 
			AlbumSet set, PicasaAlbumFactory factory) { 
		super(name, iconRes, set, factory);
	}
	
	public static AlbumSource newAlbumSource(Provider provider, 
			AlbumSet set, PicasaAlbumFactory factory) { 
		return factory.createAlbumSource(provider, set);
	}
	
	public static abstract class PicasaAlbumFactory extends MediaAlbumFactory {
		@Override
		public boolean showAlbum(PhotoSet photoSet) { 
			return photoSet != null && photoSet instanceof PicasaAlbum;
		}
		
		@Override
		public AlbumBinder createAlbumBinder(AlbumSource source, ViewType.Type type) { 
			if (type == ViewType.Type.LIST)
				return new PicasaAlbumBinderList((MediaAlbumSource)source);
			else if (type == ViewType.Type.SMALL)
				return new PicasaAlbumBinderSmall((MediaAlbumSource)source);
			
			return new PicasaAlbumBinderLarge((MediaAlbumSource)source);
		}
		
		@Override
		public void onCreateAlbum(final Activity activity, final AlbumSet albumSet) {
			if (activity == null || albumSet == null) return;
			
			if (LOG.isDebugEnabled())
				LOG.debug("onCreateAlbum: activity=" + activity + " albumSet=" + albumSet);
			
			final SystemUser account;
			if (albumSet instanceof PicasaAlbumSet) { 
				PicasaAlbumSet pas = (PicasaAlbumSet)albumSet;
				account = pas.getAccount();
			} else
				account = null;
			
			if (account == null) { 
				if (activity != null && activity instanceof IActivity) {
					((IActivity)activity).getActivityHelper()
						.showWarningMessage(R.string.album_create_unsupported);
				}
				return;
			}
			
			final LayoutInflater inflater = LayoutInflater.from(activity);
			final View view = inflater.inflate(R.layout.album_input, null);
			
			final AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity)
				.setTitle(R.string.create_album_title)
				.setView(view)
				.setCancelable(false);
			
			builder.setNeutralButton(R.string.dialog_cancel_button, 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dismissDialog(dialog);
						}
					});
			
			builder.setPositiveButton(R.string.dialog_create_button, 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							AlertDialogHelper.keepDialog(dialog, true);
							onCreateAlbumClick(activity, dialog, account, albumSet, view);
						}
					});
			
			builder.show(activity);
		}
	}
	
	static void dismissDialog(DialogInterface dialog) { 
		if (dialog == null) return;
		AlertDialogHelper.keepDialog(dialog, false);
		dialog.dismiss();
	}
	
	static String getStringValue(Object value) { 
		return value != null ? StringUtils.trim(value.toString()) : null;
	}
	
	static void onCreateAlbumClick(final Activity activity, DialogInterface dialog, 
			final SystemUser account, AlbumSet albumSet, View view) { 
		if (activity == null || dialog == null || account == null || 
			albumSet == null || view == null) 
			return;
		
		final TextView nameLabel = (TextView)view.findViewById(R.id.album_input_name);
		final EditText nameField = (EditText)view.findViewById(R.id.album_input_name_field);
		final EditText locationField = (EditText)view.findViewById(R.id.album_input_location_field);
		final EditText keywordsField = (EditText)view.findViewById(R.id.album_input_keywords_field);
		final EditText summaryField = (EditText)view.findViewById(R.id.album_input_summary_field);
		final RadioGroup accessField = (RadioGroup)view.findViewById(R.id.album_input_access_field);
		
		final String name = getStringValue(nameField.getText());
		final String location = getStringValue(locationField.getText());
		final String keywords = getStringValue(keywordsField.getText());
		final String summary = getStringValue(summaryField.getText());
		
		String accessText = "public";
		if (accessField.getCheckedRadioButtonId() == R.id.album_input_access_private)
			accessText = "private";
		
		final String access = accessText;
		
		if (name == null || name.length() == 0) { 
			nameLabel.setTextColor(Color.RED);
			nameField.setHint(R.string.label_album_name_empty);
			nameField.requestFocus();
			return;
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onCreateAlbumClick: name=" + name + " location=" + location 
					+ " keywords=" + keywords + " access=" + access);
		}
		
		final IActivity iactivity = (activity instanceof IActivity) 
				? (IActivity)activity : null;
		
		final ActionHelper helper = iactivity != null ? iactivity.getActionHelper() : null;
		if (helper != null) { 
			helper.getActionExecutor().executeAction(
				new ActionExecutor.SimpleActionJob(iactivity) {
					@Override
					public Void run(JobContext jc) {
						boolean result = false;
						boolean catched = false;
						try { 
							String message = String.format(activity.getString(R.string.album_create_processing), name);
							postActionProgressUpdate(DataAction.CREATE, message);
							
							result = PicasaCreater.createAlbum(activity, 
									account, name, location, keywords, summary, access);
							
							if (result) { 
								ContentHelper.getInstance().updateFetchDirtyWithAccount(account.getAccountName());
							}
						} catch (Throwable ex) { 
							result = false;
							catched = true;
							postActionException(DataAction.CREATE, ex);
							
							if (LOG.isDebugEnabled())
								LOG.debug("run: createActionJob error: " + ex, ex);
							
						} finally { 
							postActionProgressComplete(DataAction.CREATE, result, jc.isCancelled());
							
							if (!result && !catched) { 
								String message = String.format(activity.getString(R.string.album_create_failed), name);
								postShowMessage(message);
							}
						}
						
						return null;
					}
				});
		}
		
		dismissDialog(dialog);
	}
	
}
