package org.javenstudio.provider.account.host;

import java.net.InetAddress;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.javenstudio.android.app.AlertDialogBuilder;
import org.javenstudio.android.app.AlertDialogHelper;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.work.Work;
import org.javenstudio.common.util.Logger;

public abstract class AddDialogHelper extends BaseDialogHelper {
	private static final Logger LOG = Logger.getLogger(AddDialogHelper.class);

	public CharSequence getHostAddTitle() {
		return ResourceHelper.getResources().getString(R.string.dialog_addhost_title);
	}
	
	public boolean showAddDialog(final Activity activity) {
		if (activity == null || activity.isDestroyed()) 
			return false;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("showAddDialog: activity=" + activity);
		
		final View view = getAddDialogView(activity);
		
		final AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(activity);
		builder.setCancelable(false);
		builder.setTitle(getHostAddTitle());
		builder.setView(view);
		
		builder.setPositiveButton(R.string.dialog_cancel_button, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AlertDialogHelper.keepDialog(dialog, false);
					dialog.dismiss();
				}
			});
		
		builder.setNegativeButton(R.string.dialog_add_button, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AlertDialogHelper.keepDialog(dialog, true);
					onActionAdd(activity, view);
				}
			});
		
		builder.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					onDialogShow(builder, dialog);
				}
			});
		
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					onDialogDismiss(builder, dialog);
					mDialog = null;
					mBuilder = null;
				}
			});
		
		AlertDialog dialog = builder.show(activity);
		mDialog = dialog;
		mBuilder = builder;
		
		return dialog != null; 
	}
	
	protected View getAddDialogView(Activity activity) {
		if (activity == null) return null;
		
		final LayoutInflater inflater = LayoutInflater.from(activity);
		final View view = inflater.inflate(R.layout.select_host_input, null);
		
		return view;
	}
	
	protected void onActionAdd(final Activity activity, final View view) {
		if (activity == null || view == null)
			return;
		
		final InputMethodManager imm = (InputMethodManager)
				activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		
		final EditText domainEdit = (EditText)view.findViewById(R.id.host_input_domain_field);
		final EditText addressEdit = (EditText)view.findViewById(R.id.host_input_address_field);
		
		if (imm != null) {
			if (domainEdit != null) 
				imm.hideSoftInputFromWindow(domainEdit.getWindowToken(), 0);
			if (addressEdit != null) 
				imm.hideSoftInputFromWindow(addressEdit.getWindowToken(), 0);
		}
		
		try {
			ResourceHelper.getScheduler().post(new Work("AddHost") {
					@Override
					public void onRun() {
						boolean success = false;
						postShowDialogProgress(true, false);
						try {
							success = actionAddOnThread(activity, view);
						} catch (Throwable e) {
							if (LOG.isWarnEnabled()) LOG.warn("actionAddOnThread: error: " + e, e);
							success = false;
						} finally {
							postShowDialogProgress(false, false);
						}
						if (success) postDismissDialog();
					}
				});
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("onActionAdd: error: " + e, e);
		}
	}
	
	protected boolean actionAddOnThread(Activity activity, View view) {
		if (activity == null || view == null)
			return false;
		
		final EditText domainEdit = (EditText)view.findViewById(R.id.host_input_domain_field);
		final EditText addressEdit = (EditText)view.findViewById(R.id.host_input_address_field);
		
		final TextView domainPrompt = (TextView)view.findViewById(R.id.host_input_domain_prompt);
		final TextView addressPrompt = (TextView)view.findViewById(R.id.host_input_address_prompt);
		
		String domainValue = null;
		String addressValue = null;
		boolean domainWrong = false;
		boolean addressWrong = false;
		
		if (domainEdit != null) {
			Editable val = domainEdit.getText();
			domainValue = val != null ? val.toString() : null;
			if (domainValue != null) domainValue = domainValue.trim();
		}
		
		if (addressEdit != null) {
			Editable val = addressEdit.getText();
			addressValue = val != null ? val.toString() : null;
			if (addressValue != null) addressValue = addressValue.trim();
		}
		
		if (addressValue == null || addressValue.length() == 0) {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						if (addressPrompt != null) {
							addressPrompt.setText(R.string.dialog_host_address_required);
							addressPrompt.setVisibility(View.VISIBLE);
						}
						if (addressEdit != null) {
							addressEdit.requestFocus();
						}
					}
				});
			addressWrong = true;
			
		} else if ((addressValue = checkInputAddress(addressValue)) == null) {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						if (addressPrompt != null) {
							addressPrompt.setText(R.string.dialog_host_address_invalid);
							addressPrompt.setVisibility(View.VISIBLE);
						}
						if (addressEdit != null) {
							addressEdit.requestFocus();
						}
					}
				});
			addressWrong = true;
		}
		
		if (addressPrompt != null && !addressWrong) {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						addressPrompt.setVisibility(View.GONE);
					}
				});
		}
		
		if (domainValue != null && domainValue.length() > 0) {
			if ((domainValue = checkInputDomain(domainValue)) == null) {
				ResourceHelper.getHandler().post(new Runnable() {
						@Override
						public void run() {
							if (domainPrompt != null) {
								domainPrompt.setText(R.string.dialog_host_domain_invalid);
								domainPrompt.setVisibility(View.VISIBLE);
							}
							if (domainEdit != null) {
								domainEdit.requestFocus();
							}
						}
					});
				domainWrong = true;
			}
		}
		
		if (domainPrompt != null && !domainWrong) {
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						domainPrompt.setVisibility(View.GONE);
					}
				});
		}
		
		if (addressWrong || domainWrong) return false;
		
		if (domainValue == null || domainValue.length() == 0) 
			domainValue = getAddressHost(addressValue);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("actionAddOnThread: domain=" + domainValue 
					+ " address=" + addressValue);
		}
		
		String address = addressValue;
		int port = 0;
		int pos = addressValue.indexOf(':');
		if (pos > 0) {
			address = addressValue.substring(0, pos);
			try {
				port = Integer.parseInt(addressValue.substring(pos+1));
			} catch (Throwable e) {
				port = 0;
			}
		}
		if (port <= 0) port = 80;
		
		return updateHostData(activity, domainValue, address, port);
	}
	
	protected abstract boolean updateHostData(Activity activity, 
			String domain, String address, int port);
	
	protected String getAddressHost(String value) {
		String text = value;
		if (text != null && text.indexOf("://") < 0)
			text = "http://" + text;
		
		try {
			Uri uri = Uri.parse(text);
			String host = uri.getHost();
			//int port = uri.getPort();
			
			if (host != null && host.length() > 0) {
				//if (port > 0) host = host + ":" + port;
				return host;
			}
		} catch (Throwable e) {
			if (LOG.isDebugEnabled())
				LOG.debug("getAddressHost: \"" + value + "\" error: " + e, e);
		}
		
		return null;
	}
	
	protected String checkInputAddress(String value) {
		String text = value;
		if (text != null && text.indexOf("://") < 0)
			text = "http://" + text;
		
		try {
			Uri uri = Uri.parse(text);
			String host = uri.getHost();
			int port = uri.getPort();
			
			InetAddress[] addrs = InetAddress.getAllByName(host);
			if (host != null && host.length() > 0 && 
				addrs != null && addrs.length > 0) {
				if (port > 0) host = host + ":" + port;
				return host;
			}
		} catch (Throwable e) {
			if (LOG.isDebugEnabled())
				LOG.debug("checkInputAddress: \"" + value + "\" error: " + e, e);
		}
		
		return null;
	}
	
	protected String checkInputDomain(String value) {
		String text = value;
		if (text != null && text.indexOf("://") < 0)
			text = "http://" + text;
		
		try {
			Uri uri = Uri.parse(value);
			String host = uri.getHost();
			//int port = uri.getPort();
			
			InetAddress[] addrs = InetAddress.getAllByName(host);
			if (host != null && host.length() > 0 && 
				addrs != null && addrs.length > 0) {
				//if (port > 0) host = host + ":" + port;
				return host;
			}
		} catch (Throwable e) {
			if (LOG.isDebugEnabled())
				LOG.debug("checkInputDomain: \"" + value + "\" error: " + e, e);
		}
		
		return null;
	}
	
}
