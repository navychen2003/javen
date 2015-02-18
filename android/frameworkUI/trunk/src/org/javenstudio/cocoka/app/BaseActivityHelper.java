package org.javenstudio.cocoka.app;

import java.lang.reflect.Method;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.view.View;
import android.widget.TextView;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class BaseActivityHelper {
	private static final Logger LOG = Logger.getLogger(BaseActivityHelper.class);

	private IActionBar mActionBar = null;
	
	public abstract Activity getActivity();
	
	public SupportActionBar getSupportActionBarOrNull() { 
		IActionBar actionBar = getActionBarInternal(false);
		if (actionBar != null && actionBar instanceof SupportActionBar) 
			return (SupportActionBar)actionBar;
		
		return null;
	}
	
	public IActionBar getActionBarAdapter() { 
		return getActionBarInternal(true);
	}
	
	private synchronized IActionBar getActionBarInternal(boolean throwIfNull) { 
		if (mActionBar == null) { 
			Activity activity = getActivity();
			try {
				Method method = activity.getClass().getMethod("getActionBar");
				ActionBar actionBar = (ActionBar)method.invoke(activity, (Object[])null);
				if (actionBar == null) {
					if (LOG.isWarnEnabled()) 
						LOG.warn("getActionBarInternal: Activity.getActionBar() return null");
					
					if (throwIfNull)
						throw new NullPointerException("getActionBar return null");
				}
				mActionBar = new AndroidActionBar(getActivity(), actionBar);
			} catch (NoSuchMethodException e) { 
				if (LOG.isDebugEnabled()) 
					LOG.debug("getActionBarInternal: error: " + e, e);
				
				TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.styleable.SherlockTheme);
				if (!a.hasValue(R.styleable.SherlockTheme_windowActionBar)) {
					if (LOG.isWarnEnabled())
						LOG.warn("getActionBarInternal: no value of R.styleable.SherlockTheme_windowActionBar");
					
					//return null;
				}
				
				mActionBar = new SupportActionBar(getActivity());
			} catch (Throwable e) { 
				if (LOG.isWarnEnabled()) 
					LOG.warn("getActionBarInternal: error: " + e, e);
				
				mActionBar = new SupportActionBar(getActivity());
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("getActionBarInternal: actionBar=" + mActionBar);
		}
		
		return mActionBar;
	}
	
	public void onShowProgressView() { 
	}
	
	public void onHideProgressView() { 
		//showEventText(null);
	}
	
	public void showProgressActionView(IMenuItem menuItem) { 
		if (LOG.isDebugEnabled()) LOG.debug("showProgressActionView: item=" + menuItem);
		if (menuItem != null) {
			menuItem.setActionView(R.layout.actionview_progressbar);
			menuItem.setVisible(true);
		}
	}
	
	public void hideProgressActionView(IMenuItem menuItem) {
		if (LOG.isDebugEnabled()) LOG.debug("hideProgressActionView: item=" + menuItem);
		if (menuItem != null) {
			menuItem.setVisible(false);
		}
	}
	
	public void postShowProgressView(final boolean show) {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					showProgressView(show);
				}
			});
	}
	
	public void showProgressView(boolean show) { 
		View view = getActivity().findViewById(R.id.slidingmenu_content_progressbar);
		if (view != null) 
			view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
	}
	
	public void postShowContentMessage(final CharSequence message, 
			final int backgroundRes, final View.OnClickListener listener) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					showContentMessage(message, backgroundRes, listener);
				}
			});
	}
	
	public void showContentMessage(CharSequence message, int backgroundRes, 
			View.OnClickListener listener) { 
		TextView view = (TextView)getActivity().findViewById(R.id.slidingmenu_content_message);
		if (message != null && message.length() > 0) { 
			view.setText(message);
			if (backgroundRes != 0) view.setBackgroundResource(backgroundRes);
			view.setOnClickListener(listener);
			view.setVisibility(View.VISIBLE);
		} else { 
			view.setVisibility(View.GONE);
		}
	}
	
	//public void onNetworkAvailable(boolean available) { 
	//	View view = getActivity().findViewById(R.id.slidingmenu_content_network);
	//	if (view != null) 
	//		view.setVisibility(available ? View.INVISIBLE : View.VISIBLE);
	//}
	
	private ProgressDialog mProgressDialog = null;
	
	public void postShowProgressAlert(int titleRes) {
		postShowProgressAlert(getActivity().getText(titleRes));
	}
	
	public void postShowProgressAlert(final CharSequence title) {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					showProgressAlert(title);
				}
			});
	}
	
	public void showProgressAlert(CharSequence title) {
		if (LOG.isDebugEnabled()) LOG.debug("showProgressAlert: title=" + title);
		
		mProgressDialog = ProgressDialog.show(getActivity(), null, title, true, true, 
			new DialogInterface.OnCancelListener() {
				@Override
	            public void onCancel(DialogInterface dialog) {
					mProgressDialog = null;
					if (LOG.isDebugEnabled())
	                	LOG.debug("onCancel: user cancelling");
	            }
	        });
    }

	public void postHideProgressAlert() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					hideProgressAlert();
				}
			});
	}
	
	public void hideProgressAlert() {
		if (LOG.isDebugEnabled()) LOG.debug("hideProgressAlert");
		
		ProgressDialog progressDialog = mProgressDialog;
		mProgressDialog = null;
        if (progressDialog != null) 
        	progressDialog.dismiss();
    }
	
}
