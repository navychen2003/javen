package org.javenstudio.android.app;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupWindow;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.MessageHelper;
import org.javenstudio.android.NetworkHelper;
import org.javenstudio.android.setting.GeneralSetting;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.BaseActivityHelper;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.IRefreshView;
import org.javenstudio.cocoka.app.RefreshGridView;
import org.javenstudio.cocoka.app.RefreshListView;
import org.javenstudio.cocoka.util.OutOfMemoryListener;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.cocoka.widget.ConfirmCallback;
import org.javenstudio.cocoka.widget.PopupMenu;
import org.javenstudio.cocoka.widget.PopupMenuListener;
import org.javenstudio.cocoka.widget.SearchView;
import org.javenstudio.common.util.Logger;

public abstract class ActivityHelper extends BaseActivityHelper 
		implements OutOfMemoryListener {
	private static final Logger LOG = Logger.getLogger(ActivityHelper.class);

	public static interface HelperApp {
		public ActivityHelper getActivityHelper();
	}
	
	private final AtomicInteger mProgressCounter = new AtomicInteger(0);
	private final Activity mActivity;
	
	public ActivityHelper(Activity activity) { 
		mActivity = activity;
		Utilities.addOOMListener(this);
	}
	
	protected abstract boolean isStarted();
	protected abstract boolean isDestroyed();
	protected abstract boolean isProgressRunning();
	
	protected boolean isLockOrientationDisabled(int orientation) { return false; }
	protected boolean isUnlockOrientationDisabled() { return false; }
	
	@Override
	public final Activity getActivity() { 
		return mActivity; 
	}
	
	public boolean incrementProgress(boolean force) { 
		if (mProgressCounter.get() < 0) 
			mProgressCounter.set(0);
		
		int count = mProgressCounter.incrementAndGet();
		if (LOG.isDebugEnabled())
			LOG.debug("incrementProgress: count=" + count + " force=" + force);
		
		return true;
	}
	
	public boolean decrementProgress(boolean force) { 
		int count = mProgressCounter.decrementAndGet();
		if (LOG.isDebugEnabled())
			LOG.debug("decrementProgress: count=" + count + " force=" + force);
		
		if (count > 0 && !force) 
			return false;
		
		if (mProgressCounter.get() < 0) 
			mProgressCounter.set(0);
		
		return true;
	}
	
	private int mRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
	private boolean mDialogShowing = false;
	
	public boolean isDialogShowing() { return mDialogShowing; }
	
	@Override
	public void onShowProgressView() { 
		if (LOG.isDebugEnabled()) LOG.debug("onShowProgressView");
		super.onShowProgressView();
		lockOrientationOnProgressRunning();
		
		IRefreshView refreshView = getRefreshView();
		boolean refreshing = false;
		if (refreshView != null) {
			if (refreshView.isRefreshing()) {
				refreshing = true;
			} else {
				//refreshView.setRefreshing(true, false);
			}
		}
		
		if (!refreshing && getIActivity().isContentProgressEnabled())
			showProgressView(true);
	}
	
	@Override
	public void onHideProgressView() { 
		if (LOG.isDebugEnabled()) LOG.debug("onHideProgressView");
		super.onHideProgressView();
		unlockOrientationOnProgressStopped();
		
		//if (getIActivity().isContentProgressEnabled())
			showProgressView(false);
		
		setLastUpdatedLabel(getIActivity().getLastUpdatedLabel());
		
		IRefreshView refreshView = getRefreshView();
		if (refreshView != null && refreshView.isRefreshing())
			refreshView.onRefreshComplete();
	}
	
	@Override
	public void showProgressAlert(CharSequence title) {
		lockOrientation();
		super.showProgressAlert(title);
	}
	
	@Override
	public void hideProgressAlert() {
		super.hideProgressAlert();
		unlockOrientation();
	}
	
	public void lockOrientation() { 
		lockOrientation(getActivity().getResources().getConfiguration().orientation);
	}
	
	public void lockOrientation(int orientation) { 
		mRequestedOrientation = getActivity().getRequestedOrientation();
		if (LOG.isDebugEnabled()) {
			LOG.debug("lockOrientation: requestedOrientation=" + mRequestedOrientation 
					+ " orientation=" + orientation);
		}
		
		if (isLockOrientationDisabled(orientation)) { 
			if (LOG.isDebugEnabled())
				LOG.debug("lockOrientation: isLockOrientationDisabled return true");
			return;
		}
		
		//int orientation = getActivity().getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	public void unlockOrientation() { 
		if (LOG.isDebugEnabled())
			LOG.debug("unlockOrientation: requestedOrientation=" + mRequestedOrientation);
		
		if (isUnlockOrientationDisabled()) { 
			if (LOG.isDebugEnabled())
				LOG.debug("unlockOrientation: isUnlockOrientationDisabled return true");
			return;
		}
		
		if (isOrientationSensorEnable()) {
			//getActivity().setRequestedOrientation(mRequestedOrientation);
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		} else { 
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}
	
	public boolean isOrientationSensorEnable() { 
		return GeneralSetting.getOrientationSensor();
	}
	
	public void unlockOrientationIfCan() { 
		if (isDialogShowing() || isProgressRunning() || isActionMode()) 
			return;
		
		unlockOrientation();
	}
	
	public void lockOrientationOnProgressRunning() { 
		lockOrientation();
	}
	
	public void unlockOrientationOnProgressStopped() { 
		unlockOrientationIfCan();
	}
	
	public void lockOrientationOnDialogShowing() { 
		mDialogShowing = true;
		lockOrientation();
	}
	
	public void unlockOrientationOnDialogDismiss() { 
		mDialogShowing = false;
		unlockOrientationIfCan();
	}
	
	public void lockOrientationOnActionStarting() { 
		lockOrientation();
	}
	
	public void unlockOrientationOnActionFinished() { 
		unlockOrientationIfCan();
	}
	
	private IRefreshView mRefreshView = null;
	public IRefreshView getRefreshView() { return mRefreshView; }
	
	public void setRefreshView(IRefreshView view) { 
		if (LOG.isDebugEnabled()) LOG.debug("setRefreshView: view=" + view);
		mRefreshView = view; 
		if (view != null) {
			view.setLastUpdatedLabel(getIActivity().getLastUpdatedLabel());
		}
	}
	
	public void setLastUpdatedLabel(CharSequence label) {
		IRefreshView refreshView = mRefreshView;
		if (refreshView != null) refreshView.setLastUpdatedLabel(label);
	}
	
	public RefreshListView.OnRefreshListener createListRefreshListener(IActivity activity, 
			final IRefreshView.RefreshListener listener) {
		if (activity == null || activity != getIActivity()) 
			return null;
		
		return new RefreshListView.OnRefreshListener() {
				@Override
				public void onRefresh(RefreshListView refreshView, boolean... params) {
					if (refreshView != null && params != null && params.length == 1) {
						setRefreshView(refreshView);
						getIActivity().refreshContent(true);
					}
				}
				@Override
				public void onTouchEvent(RefreshListView refreshView, MotionEvent event) {
					if (listener != null)
						listener.onTouchEvent(refreshView, event);
				}
			};
	}
	
	public RefreshListView.OnPullEventListener createListPullListener(IActivity activity, 
			final IRefreshView.RefreshListener listener) {
		if (activity == null || activity != getIActivity()) 
			return null;
		
		return new RefreshListView.OnPullEventListener() {
				@Override
				public void onPullToRefresh(RefreshListView refreshView) {
					if (refreshView != null)
						refreshView.setLastUpdatedLabel(getIActivity().getLastUpdatedLabel());
					if (listener != null)
						listener.onPullToRefresh(refreshView);
				}
				@Override
				public void onReleaseToRefresh(RefreshListView refreshView) {
					if (listener != null)
						listener.onReleaseToRefresh(refreshView);
				}
				@Override
				public void onPullReset(RefreshListView refreshView) {
					if (listener != null)
						listener.onPullReset(refreshView);
				}
			};
	}
	
	public RefreshGridView.OnRefreshListener createGridRefreshListener(IActivity activity,
			final IRefreshView.RefreshListener listener) {
		if (activity == null || activity != getIActivity()) 
			return null;
		
		return new RefreshGridView.OnRefreshListener() {
				@Override
				public void onRefresh(RefreshGridView refreshView, boolean... params) {
					if (refreshView != null && params != null && params.length == 1) {
						setRefreshView(refreshView);
						getIActivity().refreshContent(true);
					}
				}
				@Override
				public void onTouchEvent(RefreshGridView refreshView, MotionEvent event) {
					if (listener != null)
						listener.onTouchEvent(refreshView, event);
				}
			};
	}
	
	public RefreshGridView.OnPullEventListener createGridPullListener(IActivity activity,
			final IRefreshView.RefreshListener listener) {
		if (activity == null || activity != getIActivity()) 
			return null;
		
		return new RefreshGridView.OnPullEventListener() {
				@Override
				public void onPullToRefresh(RefreshGridView refreshView) {
					if (refreshView != null)
						refreshView.setLastUpdatedLabel(getIActivity().getLastUpdatedLabel());
					if (listener != null)
						listener.onPullToRefresh(refreshView);
				}
				@Override
				public void onReleaseToRefresh(RefreshGridView refreshView) {
					if (listener != null)
						listener.onReleaseToRefresh(refreshView);
				}
				@Override
				public void onPullReset(RefreshGridView refreshView) {
					if (listener != null)
						listener.onPullReset(refreshView);
				}
			};
	}
	
	public void onActionModeCreate() { 
		IActivity activity = getIActivity();
		if (activity != null) { 
			IRefreshView refreshView = getRefreshView();
			if (refreshView != null)
				refreshView.setPullToRefreshEnabled(false);
		}
	}
	
	public void onActionModeDestroy() { 
		IActivity activity = getIActivity();
		if (activity != null) { 
			IRefreshView refreshView = getRefreshView();
			if (refreshView != null)
				refreshView.setPullToRefreshEnabled(true);
		}
	}
	
	private boolean isActionMode() { 
		IActivity activity = getIActivity();
		if (activity != null) { 
			ActionHelper helper = activity.getActionHelper();
			if (helper != null) 
				return helper.isActionMode();
		}
		
		return false;
	}
	
	public void onActivityDestroy() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onActivityDestroy: activity=" + getActivity());
		
		removePopupMenus();
		hideWarningDialog();
		hideFetchDialog();
		
		IActivity activity = getIActivity();
		if (activity != null) { 
			ActionHelper helper = activity.getActionHelper();
			if (helper != null) 
				helper.getActionExecutor().pauseAction();
		}
	}
	
	public void onActivityCreate() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onActivityCreate: activity=" + getActivity());
		
		mProgressCounter.set(0);
		unlockOrientation();
	}
	
	public void onActivityConfigurationChanged(Configuration newConfig) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onActivityConfigurationChanged: activity=" + getActivity());
	}
	
	public void onActivityStart() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onActivityStart: activity=" + getActivity());
		
		NetworkHelper.getInstance().addNetworkListener(getIActivity());
		showWarningMessage();
	}
	
	public void onActivityResume() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onActivityResume: activity=" + getActivity());
		
		unlockOrientationIfCan();
	}
	
	public void onActivityStop() { 
		if (LOG.isDebugEnabled())
			LOG.debug("onActivityStop: activity=" + getActivity());
	}
	
	public void onActivityDispatchTouchEvent(MotionEvent event) { 
		if (event == null) return;
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("onActivityDispatchTouchEvent: activity=" + getActivity() + " event=" + event);
		
		switch (event.getAction()) { 
		case MotionEvent.ACTION_DOWN: 
			TouchHelper.onMotionDown(getIActivity(), event);
			break;
		case MotionEvent.ACTION_UP: 
			TouchHelper.onMotionUp(getIActivity(), event);
			break;
		case MotionEvent.ACTION_MOVE: 
			TouchHelper.onMotionMove(getIActivity(), event);
			break;
		}
	}
	
	public IActivity getIActivity() { 
		Activity activity = getActivity();
		if (activity != null && activity instanceof IActivity) 
			return (IActivity)activity;
		
		return null;
	}
	
	public static interface SearchViewListener {
		public void onSearchViewOpen(IActivity activity, View view);
		public void onSearchViewClose(IActivity activity, View view);
		public boolean onSearchTextSubmit(IActivity activity, String query);
		public boolean onSearchTextChange(IActivity activity, String newText);
	}
	
	public void initSearchMenuItem(IMenuItem item, 
			SearchViewListener listener, boolean visible) { 
		if (item != null && listener != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("initSearchMenuItem: menuItem: " + item);
			
			initSearchView(getIActivity(), item.getActionView(), listener);
			item.setVisible(visible);
		}
	}
	
	static final int SEARCH_IMEOPTIONS = 
			EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_SEARCH;
	
	private View mSearchView = null;
	private SearchViewListener mSearchListener = null;
	
	private void initSearchView(final IActivity activity, final View view, 
			final SearchViewListener listener) { 
		if (activity == null || view == null || listener == null) 
			return;
		
		SearchManager manager = (SearchManager)
				activity.getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchable = manager.getSearchableInfo(
				activity.getComponentName());
		
		if (view instanceof SearchView) {
			if (LOG.isDebugEnabled())
				LOG.debug("initSearchView: searchView: " + view);
			
			final SearchView searchView = (SearchView)view;
			searchView.setSearchableInfo(searchable);
			searchView.setImeOptions(SEARCH_IMEOPTIONS);
			searchView.setSearchIcon(AppResources.getInstance().getDrawableRes(
					AppResources.drawable.icon_menu_search));
			
			searchView.setOnSearchClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mSearchView = view;
						mSearchListener = listener;
						//searchView.setQuery(activity.getQueryText(), false);
						listener.onSearchViewOpen(activity, view);
					}
				});
			
			searchView.setOnCloseListener(new SearchView.OnCloseListener() {
					@Override
					public boolean onClose() {
						mSearchView = null;
						mSearchListener = null;
						listener.onSearchViewClose(activity, view);
						return false;
					}
				});
			
			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
					@Override
					public boolean onQueryTextSubmit(String query) {
						if (LOG.isDebugEnabled()) 
							LOG.debug("onQueryTextSubmit: " + query);
						
						return listener.onSearchTextSubmit(activity, query);
					}
					@Override
					public boolean onQueryTextChange(String newText) {
						return listener.onSearchTextChange(activity, newText);
					}
				});
			
			searchView.setOnStartActivityListener(new SearchView.OnStartActivityListener() {
					@Override
					public boolean onStartActivity(Intent intent, int requestCode) {
						startActivityForResultSafely(intent, requestCode);
						return true;
					}
				});
		} else { 
			if (LOG.isDebugEnabled())
				LOG.debug("initSearchView: unknown searchView: " + view);
		}
	}
	
	private void onVoiceSearchResult(int resultCode, Intent data) { 
		if (resultCode == Activity.RESULT_OK && data != null) { 
			List<String> results = data.getStringArrayListExtra(
					RecognizerIntent.EXTRA_RESULTS);

			for (int i=0; results != null; i++) { 
				String result = results.get(i);
				if (result != null && result.length() > 0) { 
					IActivity activity = getIActivity();
					SearchViewListener listener = mSearchListener;
					if (activity != null && listener != null) { 
						if (LOG.isDebugEnabled()) 
							LOG.debug("onVoiceSearchResult: query=" + result);
						
						listener.onSearchTextSubmit(activity, result);
					}
					
					return;
				}
			}
		}
	}
	
	public static interface ActivityResultListener { 
		public boolean onActivityResult(ActivityHelper helper, 
				int requestCode, int resultCode, Intent data);
	}
	
	private ActivityResultListener mListener = null;
	
	public void setActivityResultListener(ActivityResultListener listener) { 
		mListener = listener;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) { 
		case SearchView.REQUESTCODE_VOICAPPSEARCH:
		case SearchView.REQUESTCODE_VOICWEBSEARCH: 
			onVoiceSearchResult(resultCode, data);
			break;
		}
		
		ActivityResultListener listener = mListener;
		if (listener != null) 
			listener.onActivityResult(this, requestCode, resultCode, data);
	}
	
	public boolean hideSearchView() { 
		return hideSearchView(mSearchView);
	}
	
	public boolean hideSearchView(View view) { 
		if (view == null) return false;
		
		if (view instanceof SearchView) {
			SearchView searchView = (SearchView)view;
			searchView.setIconified(true);
			searchView.onActionViewCollapsed();
			searchView.setImeOptions(SEARCH_IMEOPTIONS);
		}
		
		mSearchView = null;
		mSearchListener = null;
		return true;
	}
	
	public boolean onActionHome() { 
		return hideSearchView();
	}
	
	public boolean onBackPressed() { 
		return hideSearchView();
	}
	
	public void showWarningMessage(final String msg) {
		showWarningMessage(msg, null);
	}
	
	public void showWarningMessage(final Throwable e) {
		if (e == null) return;
		showWarningMessage(MessageHelper.formatExceptionObject(e), e);
	}
	
	public void showWarningMessage(final ActionError error) {
		if (error == null) return;
		showWarningMessage(MessageHelper.formatActionError(error), error);
	}
	
	public void showWarningMessage(final String msg, final Object params) {
    	if (msg == null || msg.length() == 0) 
    		return; 
    	
    	ResourceHelper.getHandler().post(new Runnable() { 
	    		public void run() { 
	    			displayWarningMessage(msg, params);
	    		}
	    	});
    }
    
	public void showWarningMessage(final int msgid) {
		showWarningMessage(msgid, null);
	}
	
    public void showWarningMessage(final int msgid, final Object params) {
    	ResourceHelper.getHandler().post(new Runnable() { 
	    		public void run() { 
	    			displayWarningMessage(msgid, params);
	    		}
	    	});
    }
    
    protected void displayWarningMessage(int messageId) {
    	displayWarningMessage(messageId, null);
    }
    
    protected void displayWarningMessage(int messageId, Object params) { 
    	displayWarningMessage(getActivity().getString(messageId), params);
    }
	
	private AlertDialog mWarningDialog = null;
	private String mWarningMessage = null;
	private Object mWarningException = null;
	private boolean mWarningShown = false;
    
	private synchronized void hideWarningDialog() { 
		AlertDialog dialog = mWarningDialog;
		if (dialog != null) dialog.dismiss();
		mWarningDialog = null;
	}
    
	public String getWarningMessage() { return mWarningMessage; }
	public Object getWarningException() { return mWarningException; }
	
	private synchronized void showWarningMessage() { 
		if (mWarningShown) return;
		
		final String message = mWarningMessage;
		final Object params = mWarningException;
		
		if (message == null) 
			return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("showAlertMessage: message=" + message + " params=" + params);
		
		showWarningMessage(message, params);
	}
	
    protected synchronized void displayWarningMessage(String message, 
    		final Object params) { 
    	if (message != null || params != null) { 
    		mWarningMessage = message;
    		mWarningException = params;
    		mWarningShown = false;
    	}
    	
    	//if (!GeneralSetting.getShowWarning()) return;
		if (!isStarted() || message == null) { 
			//Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			return;
		}
		
		AlertDialog d = mWarningDialog;
		if (d != null && d.isShowing())
			return;
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(getActivity())
			.setTitle(AppResources.getInstance().getStringRes(AppResources.string.dialog_warning_title))
			.setIcon(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_dialog_warning))
			.setMessage(message != null ? Html.fromHtml(message) : "")
			.setCancelable(false);
		
		if (params != null) {
			builder.setNegativeButton(R.string.dialog_show_trace, 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							MessageHelper.showMessageDetails(getActivity(), params);
							//GeneralSetting.setShowWarning(false);
							//onOutOfMemoryErrorExit(params);
						}
					});
		}
		
		builder.setPositiveButton(R.string.dialog_ok_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						onOutOfMemoryErrorExit(params);
					}
				});
		
		if (isDestroyed()) return;
		
		lockOrientationOnDialogShowing();
		AlertDialog dialog = builder.show();
		if (dialog != null) { 
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						unlockOrientationOnDialogDismiss();
						mWarningShown = true;
					}
				});
		}
		
		mWarningDialog = dialog;
		mWarningShown = true;
    }
	
	private Object mException = null;
	private ActionError mError = null;
	
	public Object getCatchedException() { return mException; }
	
	public void onActionError(final ActionError error) {
		onActionError(error, null);
	}
	
	public void onActionError(final ActionError error, final Object data) {
		if (error == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onActionError: error=" + error, error.getException());
		
		if (error != null && mError != error) {
			mError = error;
			if (AppResources.getInstance().handleActionError(getActivity(), error, data)) return;
			showWarningMessage(error);
		}
	}
	
	private void onExceptionCatched(final Object params) { 
		if (params == null) return;
		
		if (LOG.isDebugEnabled())
			LOG.debug("onExceptionCatched: exception=" + params);
		
		if (params != null && mException != params) {
			mException = params;
			showWarningMessage(MessageHelper.formatExceptionObject(params), params);
		}
	}
	
	private AlertDialog mAutoFetchDialog = null;
	private static boolean mAutoFetch = false;
	private static boolean mAutoFetchConfirm = false;
	
	private synchronized void hideFetchDialog() { 
		AlertDialog dialog = mAutoFetchDialog;
		if (dialog != null) dialog.dismiss();
		mAutoFetchDialog = null;
	}
	
	public synchronized boolean confirmAutoFetch(final boolean refreshOnYes) { 
		return confirmAutoFetch(new ConfirmCallback() {
				@Override
				public void onYesClick() {
					if (refreshOnYes) {
						IActivity activity = getIActivity();
						if (activity != null) {
							activity.refreshContent(false);
							activity.setContentFragment();
						}
					}
				}
				@Override
				public void onNoClick() {
				}
			});
	}
	
	public synchronized boolean confirmAutoFetch(final ConfirmCallback callback) { 
		final int autoFetchType = GeneralSetting.getAutoFetchType();
		final boolean autoFetch = mAutoFetchConfirm ? mAutoFetch : 
			autoFetchType == GeneralSetting.AUTOFETCH_ALLOW;
		
		if (autoFetch || mAutoFetchConfirm || autoFetchType != GeneralSetting.AUTOFETCH_CONFIRM) 
			return autoFetch;
		
		AlertDialog d = mAutoFetchDialog;
		if (d != null && d.isShowing())
			return autoFetch;
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(getActivity())
			.setTitle(AppResources.getInstance().getStringRes(AppResources.string.dialog_warning_title))
			.setIcon(AppResources.getInstance().getDrawableRes(AppResources.drawable.icon_dialog_warning))
			.setMessage(Html.fromHtml(getActivity().getString(R.string.dialog_auto_fetch_message)))
			.setCancelable(false);
		
		builder.setNegativeButton(R.string.dialog_yes_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mAutoFetch = true;
						mAutoFetchConfirm = true;
						dialog.dismiss();
						if (callback != null) callback.onYesClick();
					}
				});
		
		builder.setPositiveButton(R.string.dialog_no_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mAutoFetch = false;
						mAutoFetchConfirm = true;
						dialog.dismiss();
						if (callback != null) callback.onNoClick();
					}
				});
		
		if (isDestroyed()) return autoFetch;
		
		AlertDialog dialog = builder.show(getActivity());
		
		mAutoFetchDialog = dialog;
		return autoFetch;
	}
	
	@Override
	public void onOutOfMemoryError(OutOfMemoryError e) { 
		onExceptionCatched(e);
	}
	
	private void onOutOfMemoryErrorExit(Object params) { 
    	if (params != null && params instanceof OutOfMemoryError) { 
    		//android.os.Process.killProcess(android.os.Process.myPid());
    		exitProcess();
    	}
    }
    
    public void exitProcess() { 
    	ResourceHelper.exitProcess();
    }
	
    public void startActivitySafely(Intent intent) {
    	startActivitySafely(intent, null); 
    }
    
    public void startActivitySafely(Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getActivity().startActivity(intent);
        } catch (Throwable e) {
        	onActionError(new ActionError(ActionError.Action.START_ACTIVITY, e));
        }
        //} catch (ActivityNotFoundException e) {
        //    Toast.makeText(getActivity(), "Activity not found", Toast.LENGTH_SHORT).show();
        //    
        //    if (LOG.isErrorEnabled()) 
        //    	LOG.error("Unable to launch. tag=" + tag + " intent=" + intent, e);
        //    
        //} catch (SecurityException e) {
        //    Toast.makeText(getActivity(), "Activity not found", Toast.LENGTH_SHORT).show();
        //    
        //    if (LOG.isErrorEnabled()) {
        //    	LOG.error("I does not have the permission to launch " + intent +
        //            ". Make sure to create a MAIN intent-filter for the corresponding activity " +
        //            "or use the exported attribute for this activity. "
        //            + "tag="+ tag + " intent=" + intent, e);
        //    }
        //}
    }
    
    public void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
        	getActivity().startActivityForResult(intent, requestCode);
        } catch (Throwable e) {
        	onActionError(new ActionError(ActionError.Action.START_ACTIVITY, e));
        }
        //} catch (ActivityNotFoundException e) {
        //    Toast.makeText(getActivity(), "Activity not found", Toast.LENGTH_SHORT).show();
        //    
        //} catch (SecurityException e) {
        //    Toast.makeText(getActivity(), "Activity not found", Toast.LENGTH_SHORT).show();
        //    
        //    if (LOG.isErrorEnabled()) {
        //    	LOG.error("I does not have the permission to launch " + intent +
        //            ". Make sure to create a MAIN intent-filter for the corresponding activity " +
        //            "or use the exported attribute for this activity.", e);
        //    }
        //}
    }
    
    public View newActionView(String className) { 
    	return (View)newInstance(className, 
    			new Class[] { Context.class }, 
    			new Object[] { getActivity().getApplicationContext() });
    }
    
    @SuppressWarnings("unchecked")
    private <T> T newInstance(String className, Class<?>[] constructorSignature,
            Object[] arguments) {
        try {
            Class<?> clazz = getActivity().getClassLoader().loadClass(className);
            Constructor<?> constructor = clazz.getConstructor(constructorSignature);
            return (T) constructor.newInstance(arguments);
        } catch (Throwable e) {
        	if (LOG.isErrorEnabled())
            	LOG.error("Cannot instantiate class: " + className, e);
        }
        return null;
    }
    
    private Map<Integer, PopupMenu> mPopupMenus = null; 
    
    private synchronized PopupMenu getPopupMenu(int id) { 
    	if (mPopupMenus == null) 
    		mPopupMenus = new HashMap<Integer, PopupMenu>(); 
    	return mPopupMenus.get(id); 
    }
    
    private synchronized void setPopupMenu(int id, PopupMenu menu) { 
    	PopupMenu old = getPopupMenu(id); 
    	if (old != null) { 
    		if (old == menu) return; 
    		old.dismiss(); 
    	}
    	if (mPopupMenus != null) { 
    		if (menu != null) 
    			mPopupMenus.put(id, menu); 
    		else 
    			mPopupMenus.remove(id); 
    	}
    }
    
    private synchronized void removePopupMenus() {
    	if (mPopupMenus != null) { 
    		for (int id : mPopupMenus.keySet()) { 
    			PopupMenu menu = mPopupMenus.get(id); 
    			if (menu != null && menu.isShowing()) 
    				menu.dismiss(); 
    			
    			onPopupMenuRemoved(id, menu); 
    		}
    		
    		mPopupMenus.clear(); 
    	}
    }
    
    public final void showPopupMenu(final int id, final View view) { 
    	showPopupMenu(id, view, null); 
    }
    
    public final void showPopupMenu(final int id, final View view, 
    		final PopupMenuListener listener) { 
		PopupMenu menu = getPopupMenu(id); 
		if (menu == null) { 
			final PopupMenu newmenu = listener != null ? 
					listener.createPopupMenu(id, view) : createPopupMenu(id, view); 
			if (newmenu == null) 
				return; 
			
			newmenu.setOnDismissListener(new PopupWindow.OnDismissListener() {
					@Override
					public void onDismiss() {
						if (listener != null) 
							listener.onPopupMenuDismiss(id, newmenu, view); 
						else 
							onPopupMenuDismiss(id, newmenu, view); 
					}
				});
			
			if (listener != null) 
				listener.onPopupMenuCreated(id, newmenu, view); 
			else 
				onPopupMenuCreated(id, newmenu, view); 
			
			menu = newmenu;
			setPopupMenu(id, newmenu); 
		}
		
		if (menu != null && !menu.isShowing()) {
			if (listener != null) 
				listener.onPopupMenuShow(id, menu, view); 
			else 
				onPopupMenuShow(id, menu, view); 
			
			if (listener != null) 
				listener.showPopupMenuAt(id, menu, view); 
			else 
				showPopupMenuAt(id, menu, view); 
		}
	}
	
    public void showPopupMenuAt(int id, PopupMenu menu, final View view) { 
    	IActivity activity = getIActivity();
    	if (activity != null) 
    		menu.showAtBottom(activity.getContentView()); 
    }
    
    public PopupMenu createPopupMenu(int id, final View view) { 
		return null; 
	}
	
    public void onPopupMenuCreated(int id, PopupMenu menu, final View view) {}
    public void onPopupMenuShow(int id, PopupMenu menu, final View view) {}
    public void onPopupMenuDismiss(int id, PopupMenu menu, final View view) {}
    public void onPopupMenuRemoved(int id, PopupMenu menu) {}
    
	public void showAboutDialog() { 
		if (LOG.isDebugEnabled()) { 
			LOG.debug("showAboutDialog: activity=" + getActivity() 
					+ " destroyed=" + isDestroyed());
		}
		if (isDestroyed()) return;
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(getActivity())
			.setTitle(AppResources.getInstance().getStringRes(AppResources.string.about_title))
			.setMessage(AppResources.getInstance().getStringText(AppResources.string.about_message))
			.setCancelable(true);
		
		builder.setNegativeButton(R.string.dialog_exit_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						exitProcess();
					}
				});
		
		builder.setPositiveButton(R.string.dialog_cancel_button, 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		
		builder.show(getActivity());
	}
    
}
