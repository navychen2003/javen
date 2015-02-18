package org.javenstudio.provider;

import org.javenstudio.android.app.IActivity;

public interface IProviderActivity extends IActivity {

	public void setContentProvider(Provider provider);
	public void setContentProvider(Provider provider, boolean force);
	public Provider getCurrentProvider();
	
}
