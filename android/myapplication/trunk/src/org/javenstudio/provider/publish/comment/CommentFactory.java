package org.javenstudio.provider.publish.comment;

public class CommentFactory {

	public CommentDataSets createCommentDataSets(CommentProvider p) { 
		return new CommentDataSets(new CommentCursorFactory(p));
	}
	
	public CommentBinder createCommentBinder(CommentProvider p) { 
		return new CommentBinder(p);
	}
	
}
