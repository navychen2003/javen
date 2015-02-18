package org.javenstudio.provider.publish;

import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.provider.publish.comment.CommentFactory;
import org.javenstudio.provider.publish.comment.CommentProvider;

public abstract class BaseCommentProvider extends CommentProvider {

	public BaseCommentProvider(String name, int iconRes, 
			IMediaComments comments, CommentFactory factory) { 
		super(name, iconRes, comments, factory);
	}
	
}
