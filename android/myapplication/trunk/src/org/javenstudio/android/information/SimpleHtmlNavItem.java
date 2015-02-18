package org.javenstudio.android.information;

import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.parser.ParseException;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.ContentHandler;
import org.javenstudio.common.parser.html.ContentTable;
import org.javenstudio.common.parser.util.ParseUtils;

public abstract class SimpleHtmlNavItem extends BaseHtmlNavItem {

	public SimpleHtmlNavItem(NavBinder res, NavigationInfo info) { 
		this(res, info, false); 
	}
	
	public SimpleHtmlNavItem(NavBinder res, NavigationInfo info, boolean selected) { 
		super(res, info, selected); 
	}
	
	@Override 
	protected Information onNewInformation(ContentTable content) { 
		InformationOne info = null;
		
		if (content != null && content instanceof DefaultTable) { 
			DefaultTable table = (DefaultTable)content; 
			
			info = new InformationOne(this, getLocation()); 
			info.setTitle(table.getTitle()); 
			info.setSummary(table.getDescription()); 
			info.setDate(table.getDate()); 
		}
		
		return info; 
	}
	
	public static class DefaultTable extends ContentTable {
		private String mTitle = null; 
		private String mDesc = null; 
		private String mDate = null; 
		
		public DefaultTable(ContentHandler handler) throws ParseException { 
			super(handler); 
		}
		
		@Override 
		public void onInitFields() { 
			// do nothing
		}
		
		public String getTitle() { return mTitle; } 
		public String getDescription() { return mDesc; } 
		public String getDate() { return mDate; } 
		
		protected void setTitle(String s) { mTitle = s; } 
		protected void setDescription(String s) { mDesc = s; } 
		protected void setDate(String s) { mDate = s; } 
		
		protected String getHtmlText(HTMLHandler a, int startLength, int endLength) { 
			if (a != null) 
				return ParseUtils.trim(ParseUtils.removeWhiteSpaces(a.getString(startLength, endLength))); 
			else 
				return null; 
		}
	}
	
}
