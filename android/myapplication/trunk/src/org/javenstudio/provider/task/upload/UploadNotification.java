package org.javenstudio.provider.task.upload;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.app.R;
import org.javenstudio.common.util.Logger;

public class UploadNotification {
	private static final Logger LOG = Logger.getLogger(UploadNotification.class);
	
    /**
     * This inner class is used to collate uploads that are owned by
     * the same application. This is so that only one notification line
     * item is used for all uploads of a given application.
     *
     */
    static class NotificationItem {
        int mId;  // This first db _id for the upload for the app
        long mTotalCurrent = 0;
        long mTotalTotal = 0;
        int mTitleCount = 0;
        String mAccountName; 
        String mDescription;
        String[] mTitles = new String[2]; // upload titles.
        String mPausedText = null;

        /**
         * Add a second upload to this notification item.
         */
        void addItem(String title, long currentBytes, long totalBytes) {
            mTotalCurrent += currentBytes;
            if (totalBytes <= 0 || mTotalTotal == -1) {
                mTotalTotal = -1;
            } else {
                mTotalTotal += totalBytes;
            }
            if (mTitleCount < 2) {
                mTitles[mTitleCount] = title;
            }
            mTitleCount++;
        }
    }
	
    static class NotificationInfo { 
    	public int id = 0;
    	public int totalCount = 0; 
    	public int finishCount = 0; 
		public int progress = 0; 
		public String description = null;
		
		public boolean isChanged(NotificationInfo info) { 
			if (info == null) return true;
			
			return this.id != info.id || 
					this.totalCount != info.totalCount || 
					this.finishCount != info.finishCount || 
					this.progress != info.progress || 
					isDescChanged(info.description);
		}
		
		private boolean isDescChanged(String text) { 
			if (this.description != null || text != null) { 
				if (this.description == null || text == null) 
					return true;
				return this.description.equals(text);
			}
			return false;
		}
    }
    
	private final AccountApp mApp;
	private final NotificationManager mNotificationManager;
	private final Map<String, NotificationItem> mNotifications;
	private final ComponentName mReceiverName;
	private final String mAppName;
	private final int mIconRes;
	private final int mNotificationId;
	private NotificationInfo mLastInfo = null;
	private long mLastUpdate = 0;
	
	public UploadNotification(AccountApp app, 
			ComponentName receiverName, int notificationId, 
			String appName, int iconRes) { 
		if (app == null || receiverName == null) throw new NullPointerException();
		mApp = app;
		mReceiverName = receiverName;
		mNotificationId = notificationId;
		mAppName = appName;
		mIconRes = iconRes;
		mNotifications = new HashMap<String, NotificationItem>();
		mNotificationManager = (NotificationManager)getContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	public AccountApp getAccountApp() { return mApp; }
	public Context getContext() { return getAccountApp().getContext(); }
	
    /**
     * Update the notification ui.
     */
    public synchronized void updateNotification(Collection<UploadDataInfo> uploads) {
        updateActiveNotification(uploads);
        //updateMultiNotification(uploads);
        //updateCompletedNotification(uploads);
    }
	
    public synchronized void cancelNotification(long id) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("cancelNotification: id=" + id);
    	
        mNotificationManager.cancel((int) id);
    }

    public synchronized void cancelAllNotifications() {
    	if (LOG.isDebugEnabled())
    		LOG.debug("cancelAllNotifications");
    	
        mNotificationManager.cancelAll();
    }
    
    private void updateActiveNotification(Collection<UploadDataInfo> uploads) {
        if (uploads == null) return;
        
        int totalCount = 0, finishCount = 0;
        int pendingCount = 0, runningCount = 0;
        long totalBytes = 0, currentBytes = 0;
        
        StringBuilder desc = new StringBuilder();
        
        for (UploadDataInfo upload : uploads) {
        	if (upload == null) continue;
        	
        	totalCount ++;
        	if (upload.isUploadFinished())
        		finishCount ++;
        	
            if (!isActiveAndVisible(upload)) 
                continue;
            
            if (upload.isUploadRunning()) {
            	String name = upload.getContentName();
                if (name != null && name.length() > 0) { 
                	if (desc.length() > 0) desc.append(", ");
                	desc.append(name);
                }
                
                totalBytes += upload.getTotalBytes();
                currentBytes += upload.getCurrentBytes();
                
            	runningCount ++;
            } else
            	pendingCount ++;
        }
        
        if (pendingCount > 0 || runningCount > 0) {
        	String text = getContext().getString(R.string.upload_notification_running);
        	if (desc.length() > 0) text += " " + desc.toString();
        	
        	long current = System.currentTimeMillis();
        	boolean ignore = (current - mLastUpdate) < 100;
        	
        	finishCount += runningCount;
        	if (finishCount > totalCount) finishCount = totalCount;
        	
        	int progress = 0; 
            if (totalBytes > 0 && currentBytes >= 0) 
            	progress = (int)(100.0f * ((float)currentBytes/(float)totalBytes));
        	
        	NotificationInfo lastInfo = mLastInfo;
        	NotificationInfo info = new NotificationInfo();
        	info.id = mNotificationId;
        	info.totalCount = totalCount;
        	info.finishCount = finishCount;
        	info.progress = progress;
        	info.description = text;
        	
        	if (lastInfo == null || lastInfo.isChanged(info))
        		ignore = false;
        	
        	if (LOG.isDebugEnabled()) {
        		LOG.debug("updateActiveNotification: ignore=" + ignore 
        				+ " total=" + totalCount + " finish=" + finishCount 
        				+ " bytes=" + totalBytes + " sent=" + currentBytes);
        	}
        	
        	if (ignore) return;
        	mLastUpdate = current;
        	mLastInfo = info;
        	
	        notificationForActive(mNotificationId, totalCount, finishCount, progress, text);
	        
        } else{ 
        	cancelNotification(mNotificationId);
        }
    }
    
    private void notificationForActive(int id, int totalCount, 
    		int finishCount, int progress, String desc) { 
    	// Build the notification object
        final Notification.Builder builder = new Notification.Builder(getContext());

        builder.setSmallIcon(mIconRes != 0 ? mIconRes : android.R.drawable.stat_sys_upload_done);
        builder.setOngoing(true);
        builder.setAutoCancel(false);

        StringBuilder title = new StringBuilder(mAppName);
        if (totalCount > 0 && finishCount >= 0) {
            title.append(" (")
            	.append(Integer.toString(finishCount)).append("/")
            	.append(Integer.toString(totalCount))
            	.append(")");
        }
        builder.setContentTitle(title);
        builder.setContentText(desc);

        builder.setProgress(100, progress, progress <= 0);
        builder.setContentInfo(buildPercentageLabel(progress));

        Intent intent = new Intent(UploadHelper.ACTION_LIST);
        intent.setComponent(mReceiverName);
        intent.setData(ContentUris.withAppendedId(UploadHelper.ALL_UPLOADS_CONTENT_URI, -1));
        //intent.putExtra("multiple", item.mTitleCount > 1);

        builder.setContentIntent(PendingIntent.getBroadcast(getContext(), 0, intent, 0));
        @SuppressWarnings("deprecation")
		Notification notification = builder.getNotification();
        
        if (LOG.isDebugEnabled())
        	LOG.debug("notificationForActive: notification=" + notification + " intent=" + intent);
        
        mNotificationManager.notify(id, notification);
    }
    
	@SuppressWarnings("unused")
	private void updateMultiNotification(Collection<UploadDataInfo> uploads) {
        // Collate the notifications
        mNotifications.clear();
        if (uploads == null) return;
        
        for (UploadDataInfo upload : uploads) {
            if (!isActiveAndVisible(upload)) 
                continue;
            
            String accountName = upload.getAccountName();
            long max = upload.getTotalBytes();
            long progress = upload.getCurrentBytes();
            long id = upload.getUploadId();
            String title = upload.getTitle();
            if (title == null || title.length() == 0) 
                title = getContext().getString(R.string.upload_unknown_title);
            
            NotificationItem item;
            if (mNotifications.containsKey(accountName)) {
                item = mNotifications.get(accountName);
                item.addItem(title, progress, max);
                
            } else {
                item = new NotificationItem();
                item.mId = (int) id;
                item.mAccountName = accountName;
                item.mDescription = upload.getContentName();
                item.addItem(title, progress, max);
                mNotifications.put(accountName, item);
            }
            
            //if (upload.mStatus == Uploads.Impl.STATUS_QUEUED_FOR_WIFI
            //        && item.mPausedText == null) {
            //    item.mPausedText = mContext.getResources().getString(
            //            R.string.notification_need_wifi_for_size);
            //}
        }

        // Add the notifications
        for (NotificationItem item : mNotifications.values()) {
            // Build the notification object
            final Notification.Builder builder = new Notification.Builder(getContext());

            boolean hasPausedText = (item.mPausedText != null);
            int iconResource = android.R.drawable.stat_sys_upload_done;
            if (hasPausedText) 
                iconResource = android.R.drawable.stat_sys_warning;
            
            builder.setSmallIcon(iconResource);
            builder.setOngoing(true);

            boolean hasContentText = false;
            StringBuilder title = new StringBuilder(item.mTitles[0]);
            if (item.mTitleCount > 1) {
                title.append(getContext().getString(R.string.upload_notification_filename_separator));
                title.append(item.mTitles[1]);
                if (item.mTitleCount > 2) {
                    title.append(getContext().getString(R.string.upload_notification_filename_extras,
                            new Object[] { Integer.valueOf(item.mTitleCount - 2) }));
                }
            } else if (!TextUtils.isEmpty(item.mDescription)) {
                builder.setContentText(item.mDescription);
                hasContentText = true;
            }
            builder.setContentTitle(title);

            if (hasPausedText) {
                builder.setContentText(item.mPausedText);
                
            } else {
                builder.setProgress((int) item.mTotalTotal, (int) item.mTotalCurrent, item.mTotalTotal == -1);
                if (hasContentText) 
                    builder.setContentInfo(buildPercentageLabel(item.mTotalTotal, item.mTotalCurrent));
            }

            Intent intent = new Intent(UploadHelper.ACTION_LIST);
            intent.setComponent(mReceiverName);
            intent.setData(ContentUris.withAppendedId(UploadHelper.ALL_UPLOADS_CONTENT_URI, item.mId));
            intent.putExtra("multiple", item.mTitleCount > 1);

            builder.setContentIntent(PendingIntent.getBroadcast(getContext(), 0, intent, 0));
            @SuppressWarnings("deprecation")
			Notification notification = builder.getNotification();
            
            if (LOG.isDebugEnabled())
            	LOG.debug("updateActiveNotification: notification=" + notification + " intent=" + intent);
            
            mNotificationManager.notify(item.mId, notification);
        }
    }
	
    @SuppressWarnings("unused")
	private void updateCompletedNotification(Collection<UploadDataInfo> uploads) {
    	if (uploads == null) return;
    	
        for (UploadDataInfo upload : uploads) {
            if (!isCompleteAndVisible(upload)) 
                continue;
            
            notificationForCompleted(upload.getUploadId(), upload.getTitle(),
                    upload.getUploadStatus(), upload.getLastUpdate());
        }
    }
	
	private void notificationForCompleted(long id, String title, int status, long lastMod) {
        // Add the notifications
        Notification.Builder builder = new Notification.Builder(getContext());
        builder.setSmallIcon(mIconRes != 0 ? mIconRes : android.R.drawable.stat_sys_upload_done);
        
        if (title == null || title.length() == 0) 
            title = getContext().getResources().getString(R.string.upload_unknown_title);
        
        Uri contentUri = ContentUris.withAppendedId(
        		UploadHelper.ALL_UPLOADS_CONTENT_URI, id);
        
        String caption;
        Intent intent;
        
        if (UploadHelper.isStatusError(status)) {
            caption = getContext().getResources().getString(R.string.upload_notification_failed);
            intent = new Intent(UploadHelper.ACTION_LIST);
            
        } else {
            caption = getContext().getResources().getString(R.string.upload_notification_finished);
            intent = new Intent(UploadHelper.ACTION_LIST);
        }
        
        intent.setComponent(mReceiverName);
        intent.setData(contentUri);

        builder.setWhen(lastMod);
        builder.setContentTitle(title);
        builder.setContentText(caption);
        builder.setContentIntent(PendingIntent.getBroadcast(getContext(), 0, intent, 0));

        intent = new Intent(UploadHelper.ACTION_HIDE);
        intent.setComponent(mReceiverName);
        intent.setData(contentUri);
        builder.setDeleteIntent(PendingIntent.getBroadcast(getContext(), 0, intent, 0));

        @SuppressWarnings("deprecation")
		Notification notification = builder.getNotification();
        
        if (LOG.isDebugEnabled())
        	LOG.debug("notificationForCompleted: notification=" + notification + " intent=" + intent);
        
        mNotificationManager.notify((int) id, notification);
    }
	
    private boolean isActiveAndVisible(UploadDataInfo upload) {
        return upload != null && (upload.isUploadPending() || upload.isUploadRunning());
    }

	private boolean isCompleteAndVisible(UploadDataInfo upload) {
        return upload != null && upload.isUploadFinished();
    }

    private String buildPercentageLabel(long totalBytes, long currentBytes) {
        if (totalBytes <= 0) 
            return null;
        
        final int percent = (int) (100 * currentBytes / totalBytes);
        return buildPercentageLabel(percent);
    }
    
    private String buildPercentageLabel(int percent) {
    	if (percent < 0) percent = 0;
    	if (percent > 100) percent = 100;
    	
        return getContext().getString(R.string.upload_notification_percent, percent);
    }
	
}
