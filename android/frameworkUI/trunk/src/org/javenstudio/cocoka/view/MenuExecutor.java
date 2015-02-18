package org.javenstudio.cocoka.view;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import org.javenstudio.cocoka.app.BaseResources;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.R;
import org.javenstudio.cocoka.data.IMediaItem;
import org.javenstudio.cocoka.data.IMediaObject;
import org.javenstudio.cocoka.data.MediaHelper;
import org.javenstudio.cocoka.opengl.GLActivity;
import org.javenstudio.cocoka.opengl.SynchronizedHandler;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.cocoka.worker.job.Future;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.cocoka.worker.job.JobSubmit;
import org.javenstudio.common.util.Logger;

final class MenuExecutor {
	private static final Logger LOG = Logger.getLogger(MenuExecutor.class);

    private static final int MSG_TASK_COMPLETE = 1;
    private static final int MSG_TASK_UPDATE = 2;
    private static final int MSG_TASK_START = 3;
    private static final int MSG_DO_SHARE = 4;
    private static final int MSG_ITEM_DELETE = 5;

    public static final int EXECUTION_RESULT_SUCCESS = 1;
    public static final int EXECUTION_RESULT_FAIL = 2;
    public static final int EXECUTION_RESULT_CANCEL = 3;

    private ProgressDialog mDialog;
    private Future<?> mTask;
    // wait the operation to finish when we want to stop it.
    private boolean mWaitOnStop;

    private final GLActivity mActivity;
    private final PhotoPage mPhotoPage;
    private final SelectionManager mSelectionManager;
    private final Handler mHandler;

	private static ProgressDialog createProgressDialog(Context context, 
			int titleId, int progressMax) {
        ProgressDialog dialog = new ProgressDialog(context);
        //dialog.setTitle(titleId);
        dialog.setMessage(context.getText(titleId));
        dialog.setMax(progressMax);
        dialog.setCancelable(false);
        dialog.setIndeterminate(false);
        
        if (progressMax > 1) 
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        
        return dialog;
    }

    public interface ProgressListener {
        public void onConfirmDialogShown();
        public void onConfirmDialogDismissed(boolean confirmed);
        public void onProgressStart();
        public void onProgressUpdate(int index);
        public void onProgressComplete(int result);
        public void onExceptionCatched(Throwable e);
    }

    public MenuExecutor(GLActivity activity, PhotoPage page, SelectionManager selectionManager) {
        mActivity = Utils.checkNotNull(activity);
        mPhotoPage = Utils.checkNotNull(page);
        mSelectionManager = Utils.checkNotNull(selectionManager);
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_TASK_START: {
                    	lockOrientation();
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressStart();
                        }
                        break;
                    }
                    case MSG_TASK_COMPLETE: {
                        stopTaskAndDismissDialog();
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressComplete(message.arg1);
                        }
                        mSelectionManager.leaveSelectionMode();
                        unlockOrientation();
                        break;
                    }
                    case MSG_TASK_UPDATE: {
                        if (mDialog != null) mDialog.setProgress(message.arg1);
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressUpdate(message.arg1);
                        }
                        break;
                    }
                    case MSG_DO_SHARE: {
                        ((Activity) mActivity).startActivity((Intent) message.obj);
                        break;
                    }
                    case MSG_ITEM_DELETE: { 
                    	if (message.obj != null) {
                    		IMediaItem item = (IMediaItem) message.obj;
                    		mPhotoPage.onDeleteItem(item);
                    	}
                    	break;
                    }
                }
            }
        };
    }

    private void stopTaskAndDismissDialog() {
        if (mTask != null) {
            if (!mWaitOnStop) mTask.cancel();
            mTask.waitDone();
            mDialog.dismiss();
            mDialog = null;
            mTask = null;
        }
    }

    public void pause() {
        stopTaskAndDismissDialog();
    }

    private GLActivity getActivity() { return mActivity; }
    
    private void lockOrientation() { 
    	int orientation = getActivity().getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    
    private void unlockOrientation() { 
    	getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
    
    private void onProgressUpdate(int index, ProgressListener listener) {
        mHandler.sendMessage(
                mHandler.obtainMessage(MSG_TASK_UPDATE, index, 0, listener));
    }

	private void onProgressStart(ProgressListener listener) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_START, listener));
    }

    private void onProgressComplete(int result, ProgressListener listener, Throwable exception) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_COMPLETE, result, 0, listener));
        
        if (listener != null && exception != null) 
        	listener.onExceptionCatched(exception);
    }
    
    private void onItemDelete(int result, IMediaItem item) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_ITEM_DELETE, result, 0, item));
    }

    public static void updateMenuOperation(IMenu menu, int supported) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("updateMenuOperation: menu=" + menu + " supported=" + supported);
    	
        boolean supportDelete = (supported & IMediaObject.SUPPORT_DELETE) != 0;
        boolean supportRotate = (supported & IMediaObject.SUPPORT_ROTATE) != 0;
        boolean supportCrop = (supported & IMediaObject.SUPPORT_CROP) != 0;
        boolean supportTrim = (supported & IMediaObject.SUPPORT_TRIM) != 0;
        boolean supportShare = (supported & IMediaObject.SUPPORT_SHARE) != 0;
        boolean supportShareTo = (supported & IMediaObject.SUPPORT_SHARETO) != 0;
        boolean supportDownload = (supported & IMediaObject.SUPPORT_DOWNLOAD) != 0;
        boolean supportSetAs = true; //(supported & IMediaObject.SUPPORT_SETAS) != 0;
        //boolean supportShowOnMap = (supported & IMediaObject.SUPPORT_SHOW_ON_MAP) != 0;
        //boolean supportCache = (supported & IMediaObject.SUPPORT_CACHE) != 0;
        boolean supportEdit = (supported & IMediaObject.SUPPORT_EDIT) != 0;
        boolean supportInfo = true; //(supported & IMediaObject.SUPPORT_INFO) != 0;
        //boolean supportImport = (supported & IMediaObject.SUPPORT_IMPORT) != 0;

        setMenuItemVisible(menu, "photo_action_delete", R.id.photo_action_delete, supportDelete);
        setMenuItemVisible(menu, "photo_action_rotate_ccw", R.id.photo_action_rotate_ccw, supportRotate);
        setMenuItemVisible(menu, "photo_action_rotate_cw", R.id.photo_action_rotate_cw, supportRotate);
        setMenuItemVisible(menu, "photo_action_crop", R.id.photo_action_crop, supportCrop);
        setMenuItemVisible(menu, "photo_action_trim", R.id.photo_action_trim, supportTrim);
        setMenuItemVisible(menu, "photo_action_share", R.id.photo_action_share, supportShare);
        setMenuItemVisible(menu, "photo_action_shareto", R.id.photo_action_shareto, supportShareTo);
        setMenuItemVisible(menu, "photo_action_download", R.id.photo_action_download, supportDownload);
        setMenuItemVisible(menu, "photo_action_setas", R.id.photo_action_setas, supportSetAs);
        //setMenuItemVisible(menu, "photo_action_show_on_map", R.id.photo_action_show_on_map, supportShowOnMap);
        setMenuItemVisible(menu, "photo_action_edit", R.id.photo_action_edit, supportEdit);
        setMenuItemVisible(menu, "photo_action_details", R.id.photo_action_details, supportInfo);
        //setMenuItemVisible(menu, "photo_action_import", R.id.photo_action_import, supportImport);
    }

    private static void setMenuItemVisible(IMenu menu, String name, 
    		int itemId, boolean visible) {
        IMenuItem item = menu.findItem(itemId);
        if (item != null) { 
        	item.setVisible(visible);
        	
        	if (visible && LOG.isDebugEnabled()) {
        		LOG.debug("setMenuItemVisible: itemId=" + itemId + " name=" + name
        				+ " item=" + item + " visible=" + visible);
        	}
        }
    }
    
    @SuppressWarnings("unused")
	private static void setMenuItemIcon(IMenu menu, int itemId, int iconRes, int iconId) {
        IMenuItem item = menu.findItem(itemId);
        if (item != null) { 
        	if (iconRes == 0) iconRes = BaseResources.getInstance().getDrawableRes(iconId);
        	if (iconRes != 0) item.setIcon(iconRes);
        }
    }

	private IMediaObject getSingleSelectedItem() {
        List<IMediaObject> ids = mSelectionManager.getSelected(true);
        Utils.assertTrue(ids.size() == 1);
        return ids.get(0);
    }

    private Intent getIntentBySingleSelectedItem(int op, String action) {
        IMediaObject obj = getSingleSelectedItem(); 
        if (obj != null && obj instanceof IMediaItem) { 
        	IMediaItem item = (IMediaItem)obj; 
        	String mimeType = MediaHelper.getMimeType(item.getMediaType());
        	Uri contentUri = item.getContentUri(op);
        	if (contentUri != null) 
        		return new Intent(action).setDataAndType(contentUri, mimeType);
        	item.onOperationError(mActivity, op, IMediaItem.ERROR_CONTENTURI_IS_NULL, action);
        }
        return null;
    }

    private void onMenuClicked(int action, ProgressListener listener) {
        onMenuClicked(action, listener, false, true);
    }

    public void onMenuClicked(int action, ProgressListener listener,
            boolean waitOnStop, boolean showDialog) {
        int title = 0;
        //switch (action) {
            //case R.id.photo_action_select_all:
            //    if (mSelectionManager.inSelectAllMode()) {
            //        mSelectionManager.deSelectAll();
            //    } else {
            //        mSelectionManager.selectAll();
            //    }
            //    return;
        
        if (action == R.id.photo_action_crop) {
            //Intent intent = getIntentBySingleSelectedPath(FilterShowActivity.CROP_ACTION)
            //        .setClass((Activity) mActivity, FilterShowActivity.class);
            //((Activity) mActivity).startActivity(intent);
            return;
            
        } else if (action == R.id.photo_action_edit) {
            Intent intent = getIntentBySingleSelectedItem(IMediaItem.SUPPORT_EDIT, Intent.ACTION_EDIT); 
            if (intent != null) { 
	            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	            ((Activity) mActivity).startActivity(Intent.createChooser(intent, null));
            }
            return;
            
        } else if (action == R.id.photo_action_setas) {
            Intent intent = getIntentBySingleSelectedItem(IMediaItem.SUPPORT_SETAS, Intent.ACTION_ATTACH_DATA);
            if (LOG.isDebugEnabled()) LOG.debug("onMenuClicked: photo_action_setas: intent=" + intent);
            if (intent != null) { 
	            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	            intent.putExtra("mimeType", intent.getType());
	            GLActivity activity = mActivity;
	            //activity.startActivity(Intent.createChooser(intent, 
	            //		activity.getString(R.string.label_set_image)));
	            activity.startChooser(intent, activity.getString(R.string.label_set_image));
            }
            return;
            
        } else if (action == R.id.photo_action_delete) {
            title = R.string.delete_processing_message; //R.string.label_delete;
            //break;
            
        } else if (action == R.id.photo_action_rotate_cw) {
            title = R.string.label_rotate_right;
            //break;
            
        } else if (action == R.id.photo_action_rotate_ccw) {
            title = R.string.label_rotate_left;
            //break;
        }
            
            //case R.id.photo_action_show_on_map:
            //    title = R.string.show_on_map;
            //    break;
            //case R.id.photo_action_import:
            //    title = R.string.Import;
            //    break;
            //default:
            //    return;
        //}
    
        startAction(action, title, listener, waitOnStop, showDialog);
    }

    private class ConfirmDialogListener implements OnClickListener, OnCancelListener {
        private final int mActionId;
        private final ProgressListener mListener;

        public ConfirmDialogListener(int actionId, ProgressListener listener) {
            mActionId = actionId;
            mListener = listener;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mListener != null) 
                    mListener.onConfirmDialogDismissed(true);
                
                onMenuClicked(mActionId, mListener);
                
            } else {
                if (mListener != null) 
                    mListener.onConfirmDialogDismissed(false);
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (mListener != null) {
                mListener.onConfirmDialogDismissed(false);
            }
        }
    }

    public void onMenuClicked(IMenuItem menuItem, String confirmMsg,
            final ProgressListener listener) {
        final int action = menuItem.getItemId();

        if (confirmMsg != null) {
            if (listener != null) listener.onConfirmDialogShown();
            ConfirmDialogListener cdl = new ConfirmDialogListener(action, listener);
            BaseResources.getInstance().createDialogBuilder(mActivity)
                    .setMessage(confirmMsg)
                    .setOnCancelListener(cdl)
                    .setPositiveButton(R.string.label_ok, cdl)
                    .setNegativeButton(R.string.label_cancel, cdl)
                    .show();
        } else {
            onMenuClicked(action, listener);
        }
    }

    public void startAction(int action, int title, ProgressListener listener) {
        startAction(action, title, listener, false, true);
    }

    public void startAction(int action, int title, ProgressListener listener,
            boolean waitOnStop, boolean showDialog) {
        List<IMediaObject> ids = mSelectionManager.getSelected(false);
        stopTaskAndDismissDialog();

        Activity activity = mActivity;
        mDialog = createProgressDialog(activity, title, ids.size());
        if (showDialog) 
            mDialog.show();
        
        MediaOperation operation = new MediaOperation(action, ids, listener);
        mTask = JobSubmit.submit(operation, null);
        mWaitOnStop = waitOnStop;
    }

    private boolean execute(JobContext jc, int cmd, IMediaObject item) throws IOException {
    	if (LOG.isDebugEnabled())
    		LOG.debug("Execute cmd: " + cmd + " for " + item);
    	
        boolean result = true;
        long startTime = System.currentTimeMillis();

        if (cmd == R.id.photo_action_delete) { 
        	if (item != null && item instanceof IMediaItem) { 
        		IMediaItem mediaItem = (IMediaItem)item;
        		boolean deleted = mediaItem.delete();
        		
        		if (LOG.isDebugEnabled())
        			LOG.debug("execute: delete item: " + mediaItem + " result=" + deleted);
        		
        		if (deleted) 
        			onItemDelete(0, mediaItem);
        	}
        }
        
        switch (cmd) {
            //case R.id.photo_action_delete:
            //    //manager.delete(path);
            //    break;
            //case R.id.photo_action_rotate_cw:
            //    //manager.rotate(path, 90);
            //    break;
            //case R.id.photo_action_rotate_ccw:
            //    //manager.rotate(path, -90);
            //    break;
            //case R.id.photo_action_toggle_full_caching: {
            //    IMediaObject obj = manager.getMediaObject(path);
            //    int cacheFlag = obj.getCacheFlag();
            //    if (cacheFlag == MediaObject.CACHE_FLAG_FULL) {
            //        cacheFlag = MediaObject.CACHE_FLAG_SCREENNAIL;
            //    } else {
            //        cacheFlag = MediaObject.CACHE_FLAG_FULL;
            //    }
            //    obj.cache(cacheFlag);
            //    break;
            //}
            //case R.id.photo_action_show_on_map: {
            //    MediaItem item = (MediaItem) manager.getMediaObject(path);
            //    double latlng[] = new double[2];
            //    item.getLatLong(latlng);
            //    if (GalleryUtils.isValidLocation(latlng[0], latlng[1])) {
            //        GalleryUtils.showOnMap(mActivity, latlng[0], latlng[1]);
            //    }
            //    break;
            //}
            //case R.id.photo_action_import: {
            //    MediaObject obj = manager.getMediaObject(path);
            //    result = obj.Import();
            //    break;
            //}
            //default:
            //    throw new AssertionError();
        }
        
        if (LOG.isDebugEnabled()) {
        	LOG.debug("It takes " + (System.currentTimeMillis() - startTime) +
                " ms to execute cmd for " + item);
        }
        
        return result;
    }

    private class MediaOperation implements Job<Void> {
        private final List<IMediaObject> mItems;
        private final int mOperation;
        private final ProgressListener mListener;

        public MediaOperation(int operation, List<IMediaObject> items,
                ProgressListener listener) {
            mOperation = operation;
            mItems = items;
            mListener = listener;
        }

        @Override
        public Void run(JobContext jc) {
            int index = 0;
            int result = EXECUTION_RESULT_SUCCESS;
            Throwable exception = null;
            
            try {
                onProgressStart(mListener);
                for (IMediaObject item : mItems) {
                    if (jc.isCancelled()) {
                        result = EXECUTION_RESULT_CANCEL;
                        break;
                    }
                    
                    if (!execute(jc, mOperation, item)) 
                        result = EXECUTION_RESULT_FAIL;
                    
                    onProgressUpdate(++index, mListener);
                }
                
            } catch (Throwable th) {
            	exception = th;
            	if (LOG.isWarnEnabled()) {
                	LOG.warn("failed to execute operation " + mOperation
                        + " : " + th, th);
            	}
            	
            } finally {
               onProgressComplete(result, mListener, exception);
            }
            
            return null;
        }
    }
	
}
