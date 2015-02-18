package org.javenstudio.provider.task.upload;

import java.io.IOException;

import android.graphics.drawable.Drawable;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionReleaseTrigger;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.multipart.FilePart;
import org.javenstudio.cocoka.net.http.multipart.MultipartEntity;
import org.javenstudio.cocoka.net.http.multipart.Part;
import org.javenstudio.cocoka.net.http.multipart.StringPart;

public abstract class UploaderBase implements IUploader {

	private final DataApp mApplication;
	private final UploadHandler mHandler;
	private final int mIconRes;
	
	public UploaderBase(DataApp app, UploadHandler handler, int iconRes) { 
		mApplication = app;
		mHandler = handler;
		mIconRes = iconRes;
	}
	
	public DataApp getApplication() { return mApplication; }
	public UploadHandler getUploadHandler() { return mHandler; }
	
	@Override
	public Drawable getProviderIcon() { 
		if (mIconRes != 0) 
			return ResourceHelper.getResources().getDrawable(mIconRes);
		return null;
	}
	
	@Override
	public boolean startUploadThread(UploadDataInfo info) { 
		return false;
	}
	
	protected HttpPost createHttpPost(final UploadDataInfo info, 
			String location) throws IOException { 
		final HttpPost post = new HttpPost(location);
		//post.setEntity(entity);
		
		post.setReleaseTrigger(new ConnectionReleaseTrigger() {
				@Override
				public void releaseConnection() throws IOException {
				}
				@Override
				public void abortConnection() throws IOException {
					onUploadAborted(info, 0, null);
				}
			});
		
		return post;
	}
	
	protected HttpEntity createFileEntity(final UploadDataInfo info, 
			String filePath, String contentType) throws IOException { 
		return new UploadFileEntity(getUploadHandler(), info.getUploadId(), 
				filePath, contentType);
	}
	
	protected HttpEntity createMultipartEntity(String contentType, Part... parts) { 
		MultipartEntity entity = new MultipartEntity(parts);
		
		if (contentType != null) 
			entity.setContentType(contentType);
		
		return entity;
	}
	
	protected FilePart createFilePart(UploadDataInfo info, 
			String filePath, String contentType, String fileName) throws IOException { 
		FilePart part = new FilePart(fileName, 
				new UploadFileSource(getUploadHandler(), info.getUploadId(), 
						filePath, fileName));
		
		if (contentType != null) 
			part.setContentType(contentType);
		
		return part;
	}
	
	protected StringPart createStringPart(String name, String value, String contentType) { 
		StringPart part = new StringPart(name, value, "utf-8");
		
		if (contentType != null) 
			part.setContentType(contentType);
		
		return part;
	}
	
	protected final void onUploadStarting(final UploadDataInfo info, final HttpPost post) { 
		if (info == null) return;
		info.setHttpPost(post);
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					info.onStarting(post);
					getUploadHandler().updateNotification();
				}
			});
	}
	
	protected final void onUploadFetching(final UploadDataInfo info) { 
		if (info == null) return;
	}
	
	protected final void onUploadFinished(final UploadDataInfo info, 
			final int code, final String message) { 
		if (info == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					info.onFinished(code, message);
					getUploadHandler().dequeueUpload(info.getUploadId());
				}
			});
	}
	
	protected final void onUploadFailed(final UploadDataInfo info, 
			final int code, final String message) { 
		if (info == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					info.onFailed(code, message);
					getUploadHandler().dequeueUpload(info.getUploadId());
				}
			});
	}
	
	protected final void onUploadAborted(final UploadDataInfo info, 
			final int code, final String message) { 
		if (info == null) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() {
					info.onAborted(code, message);
					getUploadHandler().dequeueUpload(info.getUploadId());
				}
			});
	}
	
	protected void onUploadException(UploadDataInfo info, Throwable e) { 
		if (e != null) {
			if (e instanceof HttpException) {
				final HttpException he = (HttpException)e;
				final int code = he.getStatusCode();
				final String message = he.getMessage();
				
				if (code == 200 || code == 201 || code == 202) {
					onUploadFinished(info, code, message);
				} else if (info.isPostAborted() && code == -1 && message != null && 
						message.indexOf("shutdown") >= 0) {
					onUploadAborted(info, code, message);
				} else {
					onUploadFailed(info, code, message);
				}
			} else {
				onUploadFailed(info, -1, e.getMessage());
			}
		} else {
			onUploadFinished(info, 0, null);
		}
	}
	
}
