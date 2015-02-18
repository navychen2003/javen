package org.javenstudio.cocoka.data;

import java.util.List;

import org.javenstudio.cocoka.util.ContentListener;

public interface IMediaSet extends IMediaObject {

	public int getIndexHint();
	public void setIndexHint(int index);
	
	public int getItemCount();
	public int getTotalItemCount();
	
	public IMediaItem findItem(int index);
	public List<IMediaItem> getItemList(int start, int count);
	
	public int getSubSetCount();
	public IMediaSet getSubSetAt(int index);
	
	public int getIndexOf(IMediaItem path, int hint);
	
	public void addContentListener(ContentListener listener);
	public void removeContentListener(ContentListener listener);
	
	public void notifyContentChanged();
	public long reloadData();
	
}
