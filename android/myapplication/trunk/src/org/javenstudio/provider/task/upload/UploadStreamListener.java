package org.javenstudio.provider.task.upload;

public interface UploadStreamListener {

	public void onUploadRead(long uploadId, long readSize, long totalSize);
	public void onUploadPending(long uploadId);
	public void onUploadRemoved(long uploadId);
	
}
