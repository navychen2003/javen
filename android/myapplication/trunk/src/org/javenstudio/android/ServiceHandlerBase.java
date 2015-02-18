package org.javenstudio.android;

import android.content.Context;
import android.content.Intent;

public abstract class ServiceHandlerBase implements ServiceHandler {

	@Override
	public void onServiceCreate(ServiceCallback callback) {
	}

	@Override
	public void onServiceDestroy(ServiceCallback callback) {
	}

	@Override
	public void onServiceLowMemory(ServiceCallback callback) {
	}

	@Override
	public boolean handleCommand(ServiceCallback callback, Intent intent,
			int flags, int startId) {
		return false;
	}

	@Override
	public boolean checkServiceEnabled(Context context) {
		return true;
	}

	@Override
	public boolean canStopSelf() {
		return true;
	}

	@Override
	public void actionServiceStart(Context context) {
	}

}
