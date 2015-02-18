package org.javenstudio.android.information.comment;

import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.parser.html.ContentHandlerFactory;
import org.javenstudio.common.parser.html.ContentTable;
import org.javenstudio.common.parser.html.HTMLCharacterBuilder;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.android.information.BaseHtmlNavItem;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationOne;

public abstract class CommentNavItem extends BaseHtmlNavItem {

	public CommentNavItem(NavBinder res, NavigationInfo info) { 
		this(res, info, false); 
	}
	
	public CommentNavItem(NavBinder res, NavigationInfo info, boolean selected) { 
		super(res, info, selected); 
	}
	
	@Override 
	protected HTMLHandler createHTMLHandler(ContentHandlerFactory factory) { 
		return new HTMLHandler(factory, new HTMLCharacterBuilder(true)); 
	}
	
	@Override 
	protected Information onNewInformation(ContentTable content) { 
		if (content != null && content instanceof CommentListTable) { 
			CommentListTable table = (CommentListTable)content; 
			InformationOne doc = toInformation(table); 
			if (shouldInformationAdd(table, doc)) 
				return doc; 
		}
		
		return null; 
	}
	
	protected InformationOne toInformation(CommentListTable table) { 
		return CommentListTable.toInformation(this, table); 
	}
	
	protected boolean shouldInformationAdd(CommentListTable table, InformationOne doc) { 
		if (table != null && doc != null) { 
			String subject = doc.getTitle(); //table.getTableValue(CommentTable.FIELD_SUBJECT); 
			if (subject != null && subject.length() > 0 && !table.isAdminComment()) 
				return true; 
		}
		return false; 
	}
	
}
