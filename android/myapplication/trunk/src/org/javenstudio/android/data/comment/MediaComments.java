package org.javenstudio.android.data.comment;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.cocoka.data.ChangeNotifier;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.data.MediaHelper;

public class MediaComments extends MediaHelper 
		implements IMediaComments {

	private final List<ChangeNotifier> mNotifiers = 
			new ArrayList<ChangeNotifier>();
	
	@Override
	public int getCommentCount() {
		return 0;
	}

	@Override
	public IMediaComment getCommentAt(int index) {
		return null;
	}

	@Override
	public void onViewBinded(LoadCallback callback, boolean refetch) { 
	}
	
	@Override
	public void addNotifier(ChangeNotifier notifier) {
		if (notifier == null) return;
		
		synchronized (mNotifiers) { 
			for (ChangeNotifier cn : mNotifiers) { 
				if (notifier == cn) return;
			}
			mNotifiers.add(notifier);
		}
	}

	@Override
	public void removeNotifier(ChangeNotifier notifier) {
		if (notifier == null) return;
		
		synchronized (mNotifiers) { 
			for (int i=0; i < mNotifiers.size(); ) { 
				ChangeNotifier ch = mNotifiers.get(i);
				if (ch == notifier) { 
					mNotifiers.remove(i);
					continue;
				}
				i ++;
			}
		}
	}

	@Override
	public void notifyChanged(boolean selfChange) {
		synchronized (mNotifiers) { 
			for (ChangeNotifier cn : mNotifiers) { 
				if (cn != null) 
					cn.onChange(selfChange);
			}
		}
	}

}
