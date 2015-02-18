package org.javenstudio.android.data.comment;

import org.javenstudio.cocoka.data.ChangeNotifier;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface IMediaComment {

	public CharSequence getTitle();
	public CharSequence getContent();
	public CharSequence getAuthor();
	
	public View.OnClickListener getAuthorClickListener();
	public Drawable getAuthorDrawable(int width, int height);
	
	public Drawable getProviderIcon();
	public long getPostTime();
	
	public void addNotifier(ChangeNotifier notifier);
	
}
