package org.javenstudio.android.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.setting.GeneralSetting;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.SocketStatistic;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.Logger;

public class MetricsNotification implements SocketStatistic.SocketListener {
	private static final Logger LOG = Logger.getLogger(MetricsNotification.class);

	private static final long READWRITE_THRESHOLD = 1024 * 1024;
	
	private final DataApp mApplication;
	private final NotificationManager mNotificationManager;
	private final Intent mReceiverIntent;
	private final String mAppName;
	private final int mIconRes;
	private final int mNotificationId;
	private long mReadWriteBytes = 0;
	//private long mLastUpdate = 0;
	
	public MetricsNotification(DataApp app, 
			int notificationId, Intent receiverIntent, 
			String appName, int iconRes) { 
		mApplication = app;
		mReceiverIntent = receiverIntent != null ? receiverIntent : new Intent();
		mNotificationId = notificationId;
		mAppName = appName;
		mIconRes = iconRes;
		mNotificationManager = (NotificationManager)getContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	public DataApp getApplication() { return mApplication; }
	public Context getContext() { return getApplication().getContext(); }
	
	@Override
	public synchronized void onSocketRead(int type, int bytes) { 
		if (type != ConnectivityManager.TYPE_WIFI) 
			onMobileReadWrite(bytes);
	}
	
	@Override
	public synchronized void onSocketWrite(int type, int bytes) { 
		if (type != ConnectivityManager.TYPE_WIFI) 
			onMobileReadWrite(bytes);
	}
	
	private void onMobileReadWrite(int bytes) { 
		mReadWriteBytes += bytes;
		if (mReadWriteBytes > READWRITE_THRESHOLD) { 
			mReadWriteBytes = bytes;
			
			ResourceHelper.getHandler().post(new Runnable() {
					@Override
					public void run() {
						updateNotification();
					}
				});
		}
	}
	
	/**
     * Update the notification ui.
     */
    public synchronized void updateNotification() { 
    	notificationForSocket(mNotificationId, SocketStatistic.queryStatistic());
    	//mLastUpdate = System.currentTimeMillis();
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
	
    private void notificationForSocket(int id, SocketStatistic.Statistic statistic) { 
    	if (statistic == null) return;
    	if (!GeneralSetting.getNotifyTraffic()) return;
    	
    	long mobileBytes = statistic.mobileReadBytes + statistic.mobileWriteBytes;
    	if (mobileBytes <= 0) return;
    	
    	String fromText = Utilities.formatTimeOnly(statistic.firstTime);
    	String dataText = Utilities.formatSize(mobileBytes);
    	String desc = String.format(getContext().getString(R.string.mobile_traffic_notification), 
    			fromText, dataText);
    	
    	// Build the notification object
        final Notification.Builder builder = new Notification.Builder(getContext());

        builder.setSmallIcon(mIconRes != 0 ? mIconRes : android.R.drawable.stat_sys_warning);
        //builder.setOngoing(true);
        builder.setAutoCancel(true);

        StringBuilder title = new StringBuilder(mAppName);
        builder.setContentTitle(title);
        builder.setContentText(desc);
        builder.setContentInfo(null);

        Intent intent = new Intent(mReceiverIntent);
        //intent.setData(ContentUris.withAppendedId(UploadHelper.ALL_UPLOADS_CONTENT_URI, -1));
        //intent.putExtra("multiple", item.mTitleCount > 1);

        builder.setContentIntent(getPendingIntent(intent));
        @SuppressWarnings("deprecation")
		Notification notification = builder.getNotification();
        
        if (LOG.isDebugEnabled())
        	LOG.debug("notificationForSocket: notification=" + notification + " intent=" + intent);
        
        mNotificationManager.notify(id, notification);
    }
    
    protected PendingIntent getPendingIntent(Intent intent) { 
    	return PendingIntent.getActivity(getContext(), 0, intent, 0);
    }
    
}
