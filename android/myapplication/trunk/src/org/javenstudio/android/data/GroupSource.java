package org.javenstudio.android.data;

import org.javenstudio.cocoka.android.ResourceHelper;

public abstract class GroupSource {

	private final long mIdentity = ResourceHelper.getIdentity();
	protected long mDataVersion = 0;
	
	public abstract String getName();
	public abstract DataApp getApplication();
	
	public String getTitle() { return getName(); }
	public final long getIdentity() { return mIdentity; }
	public final long getVersion() { return mDataVersion; }
	
	public long reloadData(ReloadCallback callback, ReloadType type) { return 0; }
	
}
