package org.javenstudio.android.data.comment;

import org.javenstudio.cocoka.data.ChangeNotifier;
import org.javenstudio.cocoka.data.LoadCallback;

public interface IMediaComments {

	public int getCommentCount();
	public IMediaComment getCommentAt(int index);
	
	public void addNotifier(ChangeNotifier notifier);
	public void removeNotifier(ChangeNotifier notifier);
	public void notifyChanged(boolean selfChange);
	
	public void onViewBinded(LoadCallback callback, boolean reclick);
	
}
