package org.javenstudio.android.app;

import android.content.DialogInterface;

import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataException;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.job.Future;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

public class ActionExecutor {
	private static final Logger LOG = Logger.getLogger(ActionExecutor.class);

	public static interface ActionJob extends Job<Void> { 
		public Void run(final JobContext jc);
	}
	
    public static interface ProgressListener { 
    	public boolean isCancelled();
    	public void increaseProgress(int progress, int success);
    }
	
	public static interface ActionProcessor { 
    	public int getSelectItemCount(SelectManager.SelectData[] items);
    	
    	public void executeAction(DataAction action, SelectManager.SelectData item, 
    			ProgressListener listener) throws DataException;
    	
    	public void onActionDone(DataAction action, int progress, int success);
    }
	
	private final Object mActionLock = new Object();
	private final ActionHelper mHelper;
	
    private Future<?> mActionTask = null;
    // wait the operation to finish when we want to stop it.
    private boolean mWaitOnStop = false;
	
    public ActionExecutor(ActionHelper helper) {
    	if (helper == null) throw new NullPointerException();
    	mHelper = helper;
    }
    
    public ActionHelper getHelper() { return mHelper; }
    
    public void executeAction(ActionJob job) { 
    	executeAction(job, false);
    }
    
    public void executeAction(ActionJob job, boolean waitOnStop) {
		if (job == null) return;
		stopTaskAndDismissDialog();
		
		if (LOG.isDebugEnabled())
			LOG.debug("executeAction: job=" + job + " waitOnStop=" + waitOnStop);
		
		synchronized (mActionLock) { 
			IActivity activity = getHelper().getIActivity();
			if (activity != null) 
				activity.postShowProgress(false);
			
			mActionTask = JobSubmit.submit(job, null);
			mWaitOnStop = waitOnStop;
		}
	}
    
    private synchronized void stopTaskAndDismissDialog() {
    	synchronized (mActionLock) { 
	        if (mActionTask != null) {
	        	if (LOG.isDebugEnabled())
	        		LOG.debug("stopTaskAndDismissDialog: actionTask=" + mActionTask);
	        	
	            if (!mWaitOnStop) mActionTask.cancel();
	            mActionTask.waitDone();
	            mActionTask = null;
	        }
    	}
        
    	getHelper().postHideProgressDialog();
    }

    public void pauseAction() {
    	if (!isActionProcessing()) return;
    	
    	if (LOG.isDebugEnabled())
    		LOG.debug("pauseAction: actionTask=" + mActionTask);
    	
        stopTaskAndDismissDialog();
    }
    
    public boolean isActionProcessing() { 
    	return mActionTask != null;
    }
    
    //public synchronized void waitActionDone() { 
    //	if (mActionTask != null) 
    //		mActionTask.waitDone();
    //}
	
    public void onActionProgressUpdate(DataAction action, 
			int max, int progress, int success) { 
		String text = "Processing";
		if (action == DataAction.DELETE) {
			text = getHelper().getActivity().getString(
					org.javenstudio.cocoka.app.R.string.delete_processing_message);
		}
		
		if (max > 0) { 
			int current = progress + 1;
			if (current < 0) current = 0;
			if (current > max) current = max;
			
			text += " (" + current + "/" + max + ")...";
		}
		
		getHelper().showProgressDialog(text);
	}
	
    public void postActionProgressUpdate(final DataAction action, 
			final int max, final int progress, final int success) { 
    	ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					onActionProgressUpdate(action, max, progress, success);
				}
	    	});
    }
	
    public void onActionProgressComplete(DataAction action, 
    		boolean forceRefresh, boolean cancelled) { 
		stopTaskAndDismissDialog();
		
		IActivity activity = getHelper().getIActivity();
		if (activity != null) { 
			activity.postHideProgress(false);
			
			activity.setContentFragment();
			activity.refreshContent(forceRefresh);
		}
	}
	
    public void postActionProgressComplete(final DataAction action, 
			final boolean forceRefresh, final boolean cancelled) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					onActionProgressComplete(action, forceRefresh, cancelled);
				}
	    	});
	}
	
    public void postActionProgressComplete(DataAction action, 
			int max, int progress, int success, boolean cancelled) { 
		postActionProgressComplete(action, success > 0, cancelled);
	}
    
    public void onActionProgressException(DataAction action, Throwable e) { 
		if (e == null) return;
		
		IActivity activity = getHelper().getIActivity();
		if (activity != null) 
			activity.getActivityHelper().showWarningMessage(e);
	}
    
    public synchronized void actionShareConfirm(final SelectMode mode, 
			final SelectMode.Callback callback) { 
    	if (mode == null || callback == null) return;
		
		final SelectManager manager = callback.getSelectManager();
		final SelectManager.SelectData[] items = manager != null ? 
				manager.getSelectedItems() : null;
		
		if (items == null || items.length == 0) {
			IActivity activity = getHelper().getIActivity();
			if (activity != null) 
				activity.getActivityHelper().showWarningMessage(R.string.select_empty_message);
			return;
		}
		
		for (SelectManager.SelectData data : items) {
			if (data == null) continue;
			if (data.supportOperation(FileOperation.Operation.SHARE) == false) {
				IActivity activity = getHelper().getIActivity();
				if (activity != null) {
					String text = activity.getResources().getString(R.string.share_notsupported_message);
					activity.getActivityHelper().showWarningMessage(
							String.format(text, "\""+data.getName()+"\""));
				}
				return;
			}
		}
		
		if (callback.handleAction(mode, DataAction.SHARE, items))
			return;
    }
    
    public synchronized void actionDownloadConfirm(final SelectMode mode, 
			final SelectMode.Callback callback) { 
    	if (mode == null || callback == null) return;
		
		final SelectManager manager = callback.getSelectManager();
		final SelectManager.SelectData[] items = manager != null ? 
				manager.getSelectedItems() : null;
		
		if (items == null || items.length == 0) {
			IActivity activity = getHelper().getIActivity();
			if (activity != null) 
				activity.getActivityHelper().showWarningMessage(R.string.select_empty_message);
			return;
		}
		
		for (SelectManager.SelectData data : items) {
			if (data == null) continue;
			if (data.supportOperation(FileOperation.Operation.DOWNLOAD) == false) {
				IActivity activity = getHelper().getIActivity();
				if (activity != null) {
					String text = activity.getResources().getString(R.string.download_notsupported_message);
					activity.getActivityHelper().showWarningMessage(
							String.format(text, "\""+data.getName()+"\""));
				}
				return;
			}
		}
		
		if (callback.handleAction(mode, DataAction.DOWNLOAD, items))
			return;
    }
    
    public synchronized void actionMoveConfirm(final SelectMode mode, 
			final SelectMode.Callback callback) { 
    	if (mode == null || callback == null) return;
		
		final SelectManager manager = callback.getSelectManager();
		final SelectManager.SelectData[] items = manager != null ? 
				manager.getSelectedItems() : null;
		
		if (items == null || items.length == 0) {
			IActivity activity = getHelper().getIActivity();
			if (activity != null) 
				activity.getActivityHelper().showWarningMessage(R.string.select_empty_message);
			return;
		}
		
		for (SelectManager.SelectData data : items) {
			if (data == null) continue;
			if (data.supportOperation(FileOperation.Operation.MOVE) == false) {
				IActivity activity = getHelper().getIActivity();
				if (activity != null) {
					String text = activity.getResources().getString(R.string.move_notsupported_message);
					activity.getActivityHelper().showWarningMessage(
							String.format(text, "\""+data.getName()+"\""));
				}
				return;
			}
		}
		
		if (callback.handleAction(mode, DataAction.MOVE, items))
			return;
    }
    
    public synchronized void actionDeleteConfirm(final SelectMode mode, 
			final SelectMode.Callback callback) { 
		if (mode == null || callback == null) return;
		
		final SelectManager manager = callback.getSelectManager();
		final SelectManager.SelectData[] items = manager != null ? 
				manager.getSelectedItems() : null;
		
		if (items == null || items.length == 0) {
			IActivity activity = getHelper().getIActivity();
			if (activity != null) 
				activity.getActivityHelper().showWarningMessage(R.string.select_empty_message);
			return;
		}
		
		for (SelectManager.SelectData data : items) {
			if (data == null) continue;
			if (data.supportOperation(FileOperation.Operation.DELETE) == false) {
				IActivity activity = getHelper().getIActivity();
				if (activity != null) {
					String text = activity.getResources().getString(R.string.delete_notsupported_message);
					activity.getActivityHelper().showWarningMessage(
							String.format(text, "\""+data.getName()+"\""));
				}
				return;
			}
		}
		
		if (callback.handleAction(mode, DataAction.DELETE, items))
			return;
		
		CharSequence title = callback.getActionConfirmTitle(DataAction.DELETE);
		if (title == null) {
			title = AppResources.getInstance().getResources().getString(
					R.string.label_action_delete);
		}
		
		CharSequence message = callback.getActionConfirmMessage(DataAction.DELETE);
		if (message == null) {
			message = AppResources.getInstance().getResources().getString(
					R.string.delete_confirm_message);
		}
		
		DialogInterface.OnClickListener oklistener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					manager.clearSelectedItems();
					mode.finishActionMode();
					actionDelete(callback, items);
				}
			};
		
		AlertDialogBuilder builder = AppResources.getInstance().createDialogBuilder(getHelper().getActivity())
	        .setPositiveButton(R.string.dialog_ok_button, oklistener)
	        .setNegativeButton(R.string.dialog_cancel_button, null)
	        .setTitle(title)
	        .setMessage(message)
	        .setCancelable(true)
	        .setOnCancelListener(null);
		
		builder.show(getHelper().getActivity());
	}
	
	protected void actionDelete(ActionProcessor processor, 
			SelectManager.SelectData[] items) { 
		if (processor == null || items == null) 
			return;
		
		executeAction(new DeleteActionJob(getHelper().getIActivity(), 
				processor, items), false);
	}
    
	public static abstract class SimpleActionJob implements ActionJob { 
		private final IActivity mActivity;
		
		public SimpleActionJob(IActivity activity) { 
			mActivity = activity;
		}
		
		public final IActivity getActivity() { return mActivity; }
		
		public ActionHelper getHelper() {
			IActivity activity = getActivity();
			return activity != null ? activity.getActionHelper() : null;
		}
		
		public ActionExecutor getExecutor() {
			ActionHelper helper = getHelper();
			if (helper != null) return helper.getActionExecutor();
			return null;
		}
		
		public abstract Void run(final JobContext jc);
		
		public final void postActionProgressUpdate(DataAction action, 
				CharSequence message) { 
			ActionHelper helper = getHelper();
			if (helper != null) helper.postShowProgressDialog(message);
		}
		
		public final void postActionProgressUpdate(DataAction action, 
				int max, int progress, int success) { 
			ActionExecutor executor = getExecutor();
			if (executor != null) 
				executor.postActionProgressUpdate(action, max, progress, success);
		}
		
		public final void postActionProgressComplete(DataAction action, 
				boolean forceRefresh, boolean cancelled) { 
			ActionExecutor executor = getExecutor();
			if (executor != null) 
				executor.postActionProgressComplete(action, forceRefresh, cancelled);
		}
		
		public void postActionProgressComplete(DataAction action, 
				int max, int progress, int success, boolean cancelled) {
			ActionExecutor executor = getExecutor();
			if (executor != null) {
				executor.postActionProgressComplete(action, max, progress, 
						success, cancelled);
			}
		}
		
		public final void postActionException(DataAction action, Throwable ex) { 
			ActionExecutor executor = getExecutor();
			if (executor != null) executor.onActionProgressException(action, ex);
		}
		
		public final void postShowMessage(String message) { 
			IActivity activity = getActivity();
			if (activity != null) 
				activity.getActivityHelper().showWarningMessage(message);
		}
	}
	
	public static class DeleteActionJob extends SimpleActionJob {
		private final ActionProcessor mExecutor;
		private final DataAction mAction;
		private final SelectManager.SelectData[] mItems;
		private final int mProgressMax;
		private int mProgress = 0;
		private int mProgressSuccess = 0;
		
		public DeleteActionJob(IActivity activity, ActionProcessor executor, 
				SelectManager.SelectData[] items) { 
			super(activity);
			if (executor == null || items == null) throw new NullPointerException();
			
			mExecutor = executor;
			mAction = DataAction.DELETE;
			mItems = items;
			mProgressMax = executor.getSelectItemCount(items);
		}
		
		@Override
		public Void run(final JobContext jc) {
			ProgressListener listener = new ProgressListener() {
					@Override
					public boolean isCancelled() {
						return jc.isCancelled();
					}
					@Override
					public void increaseProgress(int progress, int success) {
						mProgress += progress;
						mProgressSuccess += success;
						
						postActionProgressUpdate(mAction, 
								mProgressMax, mProgress, mProgressSuccess);
					}
				};
				
			try { 
				listener.increaseProgress(0, 0);
				
				for (SelectManager.SelectData item : mItems) { 
					if (jc.isCancelled()) break;
                    mExecutor.executeAction(mAction, item, listener);
				}
				
			} catch (final Throwable ex) { 
				if (LOG.isWarnEnabled()) 
                	LOG.warn("ActionJob: failed to execute operation: " + ex, ex);
				
				postActionException(mAction, ex);
				
			} finally { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("ActionJob: action=" + mAction + " max=" + mProgressMax 
							+ " progress=" + mProgress + " success=" + mProgressSuccess 
							+ " cancelled=" + jc.isCancelled());
				}
				
				try { 
					mExecutor.onActionDone(mAction, mProgress, mProgressSuccess);
				} catch (Throwable e) { 
					if (LOG.isWarnEnabled()) 
	                	LOG.warn("ActionJob: failed: " + e, e);
				}
				
				postActionProgressComplete(mAction, 
						mProgressMax, mProgress, mProgressSuccess, 
						jc.isCancelled());
			}
			
			return null;
		}
	}
	
}
