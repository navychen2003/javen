package org.javenstudio.cocoka.data;

import android.graphics.BitmapRegionDecoder;
import android.net.Uri;

import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;

public class SnailItem extends AbstractMediaItem {

	@Override
	public Job<BitmapRef> requestThumbnail(BitmapHolder holder, 
			RequestListener listener) {
		return new Job<BitmapRef>() {
				@Override
				public BitmapRef run(JobContext jc) {
					return null;
				}
			};
	}

	@Override
	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder, 
			RequestListener listener) {
		return new Job<BitmapRegionDecoder>() {
				@Override
				public BitmapRegionDecoder run(JobContext jc) {
					return null;
				}
			};
	}

	@Override
	public String getName() {
		return "SnailItem";
	}

	@Override
	public Uri getContentUri(int op) {
		return null;
	}
	
}
