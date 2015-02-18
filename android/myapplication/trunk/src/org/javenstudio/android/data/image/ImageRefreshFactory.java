package org.javenstudio.android.data.image;

import android.view.View;

import org.javenstudio.cocoka.util.ImageRefreshable;

public interface ImageRefreshFactory {

	public ImageRefreshable createRefresher(View view);
	
}
