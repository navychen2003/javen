package org.javenstudio.android.information.comment;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.parser.ParseException;
import org.javenstudio.common.parser.html.ContentHandler;
import org.javenstudio.common.parser.html.ContentTable;
import org.javenstudio.android.information.InformationNavItem;
import org.javenstudio.android.information.InformationOne;

public class CommentListTable extends ContentTable {
	//private static final Logger LOG = Logger.getLogger(CommentListTable.class);
	
	//public final static String FIELD_ADMINCOMMENT = "admincomment"; 
	//public final static String FIELD_SUBJECT = "subject"; 
	//public final static String FIELD_SUMMARY = "summary"; 
	//public final static String FIELD_AUTHOR = "author"; 
	//public final static String FIELD_DATE = "date"; 
	//public final static String FIELD_LINK = "link"; 
	
	private final Map<String, Object> mFields; 
	private final String mLocation;
	private boolean mAdminComment = false; 
	
	public CommentListTable(ContentHandler handler, String location) 
			throws ParseException { 
		super(handler); 
		mFields = new HashMap<String, Object>(); 
		mLocation = location;
	}
	
	public final String getLocation() { return mLocation; }
	
	@Override 
	public void onInitFields() { 
		// do nothing
	}
	
	public synchronized String[] getTableNames() { 
		return mFields.keySet().toArray(new String[0]); 
	}
	
	public synchronized Object getTableValue(String name) { 
		return name != null ? mFields.get(name) : null; 
	}
	
	protected synchronized void setTableValue(String name, Object value) { 
		if (name != null && value != null) {
			mFields.put(name, value); 
			
			//if (LOG.isDebugEnabled())
			//	LOG.debug("setTableValue: name=" + name + " value=" + value);
		}
	}
	
	public boolean isAdminComment() { return mAdminComment; } 
	protected void setAdminComment(boolean b) { mAdminComment = b; } 
	
	public static InformationOne createInformation(InformationNavItem navItem, 
			CommentListTable table) { 
		InformationOne info = new InformationOne(navItem, table.getLocation()); 
		
		return info;
	}
	
	public static InformationOne toInformation(InformationNavItem navItem, 
			CommentListTable table) { 
		InformationOne info = createInformation(navItem, table); 
		
		String subject = (String)table.getTableValue(CommentTable.FIELD_SUBJECT);
		String title = (String)table.getTableValue(CommentTable.FIELD_TITLE);
		info.setTitle(title != null && title.length() > 0 ? title : subject);
		
		info.setSummary((String)table.getTableValue(CommentTable.FIELD_SUMMARY)); 
		info.setAuthor((String)table.getTableValue(CommentTable.FIELD_AUTHOR)); 
		info.setDate((String)table.getTableValue(CommentTable.FIELD_DATE)); 
		info.setLink((String)table.getTableValue(CommentTable.FIELD_LINK)); 
		info.setContent((String)table.getTableValue(CommentTable.FIELD_CONTENT)); 
		info.setImage((String)table.getTableValue(CommentTable.FIELD_IMAGE)); 
		
		//Object summaryMode = navItem.getInfo().getAttribute(Information.ATTR_SUMMARYMODE);
		//if (summaryMode != null)
		//	info.setField(Information.ATTR_SUMMARYMODE, summaryMode);
		
		return info; 
	}
	
}