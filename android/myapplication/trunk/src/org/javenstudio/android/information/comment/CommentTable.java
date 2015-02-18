package org.javenstudio.android.information.comment;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.parser.ParseException;
import org.javenstudio.common.parser.html.ContentHandler;
import org.javenstudio.common.parser.html.ContentTable;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationOne;
import org.javenstudio.android.information.InformationSource;

public class CommentTable extends ContentTable {

	public final static String FIELD_TITLE = "title"; 
	public final static String FIELD_SUBJECT = "subject"; 
	public final static String FIELD_SUMMARY = "summary"; 
	public final static String FIELD_AUTHOR = "author"; 
	public final static String FIELD_DATE = "date"; 
	public final static String FIELD_LINK = "link"; 
	public final static String FIELD_CONTENT = "content"; 
	public final static String FIELD_IMAGE = "image"; 
	
	public final static String FIELD_EXTENDIMAGE = "extend-image"; 
	
	public final static String FIELD_ADMINCOMMENT = "admincomment"; 
	public final static String FIELD_LOCATIONS = "locations"; 
	
	private final Map<String, Object> mFields; 
	private final String mLocation;
	
	public CommentTable(ContentHandler handler, String location) 
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
		if (name != null && value != null) 
			mFields.put(name, value); 
	}
	
	public static InformationOne createInformation(InformationSource source) { 
		InformationOne info = new InformationOne(source, source.getLocation()); 
		
		//Object summaryMode = source.getAttribute(Information.ATTR_SUMMARYMODE);
		//if (summaryMode != null)
		//	info.setField(Information.ATTR_SUMMARYMODE, summaryMode);
		
		return info;
	}
	
	public static InformationOne toInformation(InformationSource source, CommentTable table) { 
		InformationOne info = createInformation(source); 
		
		String subject = (String)table.getTableValue(FIELD_SUBJECT);
		String title = (String)table.getTableValue(FIELD_TITLE);
		info.setTitle(title != null && title.length() > 0 ? title : subject);
		
		info.setSummary((String)table.getTableValue(FIELD_SUMMARY)); 
		info.setDate((String)table.getTableValue(FIELD_DATE)); 
		info.setAuthor((String)table.getTableValue(FIELD_AUTHOR)); 
		info.setLink((String)table.getTableValue(FIELD_LINK)); 
		info.setContent((String)table.getTableValue(FIELD_CONTENT)); 
		info.setImage((String)table.getTableValue(FIELD_IMAGE)); 
		
		return info; 
	}
	
	public static String getInformationField(Information data, String fieldName) { 
		if (data != null && fieldName != null && data instanceof InformationOne) { 
			InformationOne info = (InformationOne)data;
			
			if (fieldName.equals(FIELD_TITLE))
				return info.getTitle();
			
			if (fieldName.equals(FIELD_SUBJECT))
				return info.getTitle();
			
			if (fieldName.equals(FIELD_SUMMARY))
				return info.getSummary();
			
			if (fieldName.equals(FIELD_DATE))
				return info.getDate();
			
			if (fieldName.equals(FIELD_AUTHOR))
				return info.getAuthor();
			
			if (fieldName.equals(FIELD_LINK))
				return info.getLink();
			
			if (fieldName.equals(FIELD_CONTENT))
				return info.getContent();
			
			if (fieldName.equals(FIELD_IMAGE))
				return info.getImage();
		}
		
		return null;
	}
	
}
