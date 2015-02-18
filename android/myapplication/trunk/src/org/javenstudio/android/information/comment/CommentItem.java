package org.javenstudio.android.information.comment;

import java.util.Map;

import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.TagTree;

public class CommentItem extends CommentItemBase {

	public CommentItem(SourceBinder binder, String location) { 
		super(binder, location); 
	}
	
	public CommentItem(SourceBinder binder, String location, Map<String, Object> attrs) { 
		super(binder, location, attrs); 
	}
	
	@Override 
	protected void onInitAnalyzer(HTMLHandler a) { 
		if (a == null) return; 
		
		//a.newSubContentTag("html"); 
	}
	
	@Override 
	protected TagTree newDocumentTitleTagTree(CommentTable item) { 
		TagTree root = item.newTagTree("html"); 
		root.getRootTag().newChild("head").newChild("title"); 
		
		return root; 
	}
	
	@Override 
	protected TagTree newDocumentSubjectTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newDocumentSummaryTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newDocumentAuthorTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newDocumentDateTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newDocumentContentTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newDocumentImageTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newSubContentTitleTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newSubContentSubjectTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newSubContentSummaryTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newSubContentAuthorTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newSubContentDateTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newSubContentContentTagTree(CommentTable item) { 
		return null; 
	}
	
	@Override 
	protected TagTree newSubContentImageTagTree(CommentTable item) { 
		return null; 
	}
	
}
