package org.anybox.android.library.app;

import android.view.View;
import android.widget.TextView;

import org.anybox.android.library.R;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.task.TaskListBinder;
import org.javenstudio.provider.task.TaskListProvider;
import org.javenstudio.provider.task.download.DownloadBinder;
import org.javenstudio.provider.task.download.DownloadItem;
import org.javenstudio.provider.task.download.DownloadProvider;
import org.javenstudio.provider.task.upload.UploadBinder;
import org.javenstudio.provider.task.upload.UploadItem;
import org.javenstudio.provider.task.upload.UploadProvider;

public class MyTaskListBinder extends TaskListBinder {
	//private static final Logger LOG = Logger.getLogger(MyTaskListBinder.class);

	public MyTaskListBinder(TaskListProvider provider) {
		super(provider);
	}
	
	static class UploadEmptyItem extends UploadItem {
		public UploadEmptyItem(UploadProvider p) {
			super(p);
		}

		@Override
		public int getViewRes() { return R.layout.task_empty; }

		@Override
		public void bindView(IActivity activity, UploadBinder binder, View view) {
			if (activity == null || binder == null || view == null)
				return;
			
			TextView titleView = (TextView)view.findViewById(R.id.task_empty_title);
			if (titleView != null) {
				titleView.setText(R.string.upload_empty_title);
				titleView.setVisibility(View.VISIBLE);
			}
			
			TextView subtitleView = (TextView)view.findViewById(R.id.task_empty_subtitle);
			if (subtitleView != null) {
				subtitleView.setText(R.string.upload_empty_subtitle);
				subtitleView.setVisibility(View.VISIBLE);
			}
		}
	}
	
	static class DownloadEmptyItem extends DownloadItem {
		public DownloadEmptyItem(DownloadProvider p) {
			super(p);
		}

		@Override
		public int getViewRes() { return R.layout.task_empty; }

		@Override
		public void bindView(IActivity activity, DownloadBinder binder, View view) {
			if (activity == null || binder == null || view == null)
				return;
			
			TextView titleView = (TextView)view.findViewById(R.id.task_empty_title);
			if (titleView != null) {
				titleView.setText(R.string.download_empty_title);
				titleView.setVisibility(View.VISIBLE);
			}
			
			TextView subtitleView = (TextView)view.findViewById(R.id.task_empty_subtitle);
			if (subtitleView != null) {
				subtitleView.setText(R.string.download_empty_subtitle);
				subtitleView.setVisibility(View.VISIBLE);
			}
		}
	}
	
}
