package org.javenstudio.android.information.comment;

import java.util.Map;

import org.javenstudio.common.parser.ParseException;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.ContentField;
import org.javenstudio.common.parser.html.ContentHandler;
import org.javenstudio.common.parser.html.ContentHandlerFactory;
import org.javenstudio.common.parser.html.ContentTable;
import org.javenstudio.common.parser.html.ContentTableFactory;
import org.javenstudio.common.parser.html.ContentTableHandler;
import org.javenstudio.common.parser.html.HTMLCharacterBuilder;
import org.javenstudio.common.parser.html.TagElement;
import org.javenstudio.common.parser.html.TagTree;
import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.android.information.BaseInformationSource;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationModel;

public abstract class CommentItemBase extends BaseInformationSource 
		implements ContentTableHandler {
	static final boolean DEBUG = false;
	
	private boolean mDocumentContentAdded = false; 
	private DocumentTable mDocumentTable = null; 
	
	public CommentItemBase(SourceBinder binder, String location) { 
		super(binder, location); 
	}
	
	public CommentItemBase(SourceBinder binder, String location, 
			Map<String, Object> attrs) { 
		super(binder, location, attrs); 
	}
	
	public final DocumentTable getDocumentTable() { 
		return mDocumentTable; 
	}
	
	protected abstract void onInitAnalyzer(HTMLHandler a); 
	
	protected abstract TagTree newDocumentTitleTagTree(CommentTable item); 
	protected abstract TagTree newDocumentSubjectTagTree(CommentTable item); 
	protected abstract TagTree newDocumentSummaryTagTree(CommentTable item); 
	protected abstract TagTree newDocumentAuthorTagTree(CommentTable item); 
	protected abstract TagTree newDocumentDateTagTree(CommentTable item); 
	protected abstract TagTree newDocumentContentTagTree(CommentTable item); 
	protected abstract TagTree newDocumentImageTagTree(CommentTable item); 
	
	protected abstract TagTree newSubContentTitleTagTree(CommentTable item); 
	protected abstract TagTree newSubContentSubjectTagTree(CommentTable item); 
	protected abstract TagTree newSubContentSummaryTagTree(CommentTable item); 
	protected abstract TagTree newSubContentAuthorTagTree(CommentTable item); 
	protected abstract TagTree newSubContentDateTagTree(CommentTable item); 
	protected abstract TagTree newSubContentContentTagTree(CommentTable item); 
	protected abstract TagTree newSubContentImageTagTree(CommentTable item); 
	
	protected TagTree newLocationsTagTree(CommentTable item) { 
		return null; 
	}
	
	protected String parseContentPath(String fieldName, String content) { 
		return null;
	}
	
	@Override
	public String getDefaultCharset(String location) { 
		return super.getDefaultCharset(location);
	}
	
	@Override
	public String getSubContentPath(Information data, String content) { 
		if (content == null) 
			content = CommentTable.getInformationField(data, CommentTable.FIELD_SUMMARY);
		
		return parseContentPath(CommentTable.FIELD_SUMMARY, content);
	}
	
	protected ContentField newContentField(CommentTable item, String fieldName, TagTree root) { 
		return root != null ? new ContentField(item, fieldName, root) : null; 
	}
	
	@Override 
	protected HTMLHandler createHTMLHandler(ContentHandlerFactory factory) { 
		return new HTMLHandler(factory, new HTMLCharacterBuilder(true)); 
	}
	
	@Override 
	protected ContentHandlerFactory getContentFactory() { 
		final ContentTableFactory docTableFactory = new ContentTableFactory() { 
				@Override
				public ContentTable createContentTable(ContentHandler handler) { 
					try { 
						return newDocumentContentTable(handler, getLocation()); 
					} catch (Exception e) { 
						throw new RuntimeException(e); 
					}
				}
			};
		
		final ContentTableFactory subTableFactory = new ContentTableFactory() { 
				@Override
				public ContentTable createContentTable(ContentHandler handler) { 
					try { 
						return newSubContentTable(handler, getLocation()); 
					} catch (Exception e) { 
						throw new RuntimeException(e); 
					}
				}
			};
	
		return new ContentHandlerFactory(docTableFactory, subTableFactory, 
				this, DEBUG); 
	}
	
	@Override 
	public void handleTableCreate(ContentTable table) { 
		// do nothing
	}
	
	@Override 
	public synchronized void handleTableFinish(ContentTable table) { 
		DocumentTable doc = mDocumentTable; 
		if (!mDocumentContentAdded) { 
			if (doc != null && doc != table) { 
				addDocumentInformation(doc); 
				mDocumentContentAdded = true; 
			}
		} else if (doc == table) 
			return; 
		
		addInformation(table); 
	}
	
	@Override 
	public synchronized void clearDataSets() { 
		super.clearDataSets(); 
		mDocumentContentAdded = false; 
	}
	
	protected void addDocumentInformation(DocumentTable content) { 
		addInformation(content); 
	}
	
	protected void addInformation(ContentTable content) { 
		InformationModel model = getModel(); 
		if (model != null && content != null) { 
			Information info = onNewInformation(content); 
			if (info != null) 
				postAddInformation(model, info); 
		}
	}
	
	protected Information onNewInformation(ContentTable content) { 
		if (content != null && content instanceof CommentTable) { 
			CommentTable table = (CommentTable)content; 
			
			if (shouldInformationAdd(table)) 
				return toInformation(table); 
		}
		return null; 
	}
	
	protected boolean shouldInformationAdd(CommentTable table) { 
		if (table != null) { 
			String subject = (String)table.getTableValue(CommentTable.FIELD_SUBJECT); 
			if (subject != null && subject.length() > 0) 
				return true; 
			
			String summary = (String)table.getTableValue(CommentTable.FIELD_SUMMARY); 
			if (summary != null && summary.length() > 0) 
				return true; 
		}
		return false; 
	}
	
	protected Information toInformation(CommentTable table) { 
		return CommentTable.toInformation(this, table); 
	}
	
	protected ContentTable newDocumentContentTable(ContentHandler handler, 
			String location) throws ParseException { 
		DocumentTable impl = new DocumentTable(handler, location); 
		mDocumentTable = impl; 
		return impl; 
	}
	
	protected ContentTable newSubContentTable(ContentHandler handler, 
			String location) throws ParseException { 
		return new DocumentCommentTable(handler, location); 
	}
	
	@Override
	protected void onInformationParsed(String location, boolean first) {
		super.onInformationParsed(location, first);
		
		DocumentTable table = getDocumentTable();
		if (table != null && first) { 
			addPages(table.getTableValue(CommentTable.FIELD_LOCATIONS));
		}
	}
	
	public class DocumentTable extends DocumentCommentTable {
		public DocumentTable(ContentHandler handler, String location) 
				throws ParseException { 
			super(handler, location); 
		}
		
		@Override 
		public void onInitFields() { 
			addField(newContentField(this, CommentTable.FIELD_LOCATIONS, newLocationsTagTree(this))); 
			
			addField(newContentField(this, FIELD_TITLE, newDocumentTitleTagTree(this))); 
			addField(newContentField(this, FIELD_SUBJECT, newDocumentSubjectTagTree(this))); 
			addField(newContentField(this, FIELD_SUMMARY, newDocumentSummaryTagTree(this))); 
			addField(newContentField(this, FIELD_AUTHOR, newDocumentAuthorTagTree(this))); 
			addField(newContentField(this, FIELD_DATE, newDocumentDateTagTree(this))); 
			addField(newContentField(this, FIELD_CONTENT, newDocumentContentTagTree(this))); 
			addField(newContentField(this, FIELD_IMAGE, newDocumentImageTagTree(this))); 
		}
		
		public boolean isDebug() { return false; }
	}
	
	public class DocumentCommentTable extends CommentTable {
		public DocumentCommentTable(ContentHandler handler, String location) 
				throws ParseException { 
			super(handler, location); 
		}
		
		@Override 
		public void onInitFields() { 
			addField(newContentField(this, FIELD_TITLE, newSubContentTitleTagTree(this))); 
			addField(newContentField(this, FIELD_SUBJECT, newSubContentSubjectTagTree(this))); 
			addField(newContentField(this, FIELD_SUMMARY, newSubContentSummaryTagTree(this))); 
			addField(newContentField(this, FIELD_AUTHOR, newSubContentAuthorTagTree(this))); 
			addField(newContentField(this, FIELD_DATE, newSubContentDateTagTree(this))); 
			addField(newContentField(this, FIELD_CONTENT, newSubContentContentTagTree(this))); 
			addField(newContentField(this, FIELD_IMAGE, newSubContentImageTagTree(this))); 
		}
		
		@Override 
		public void onFoundField(ContentField field, TagElement e, int endLength) { 
			if (field == null || e == null) 
				return; 
			
			setCommentTableValue(this, field.getName(), e, endLength); 
		}
		
		public boolean isDebug() { return DEBUG; }
	}
	
	protected void setCommentTableValue(CommentTable table, String fieldName, TagElement e, int endLength) { 
		if (CommentTable.FIELD_LOCATIONS.equals(fieldName)) { 
			table.setTableValue(CommentTable.FIELD_LOCATIONS, 
					parseLocations(table.getLocation(), e, endLength)); 
			return; 
		}
		
		table.setTableValue(fieldName, parseField(fieldName, e, endLength)); 
	}
	
	protected String[] parseLocations(String location, TagElement e, int endLength) { 
		return null; 
	}
	
	protected String parseField(String fieldName, TagElement e, int endLength) { 
		return parseFieldDefault(fieldName, e, endLength);
	}
	
	public String parseFieldDefault(String fieldName, TagElement e, int endLength) { 
		if (CommentTable.FIELD_SUMMARY.equals(fieldName) || 
			CommentTable.FIELD_CONTENT.equals(fieldName) || 
			CommentTable.FIELD_IMAGE.equals(fieldName)) { 
	        return getHtmlText(e.getAnalyzer(), e.getStartLength(), endLength); 
	    }
		
		return getSimpleText(e.getAnalyzer(), e.getStartLength(), endLength); 
	}
	
	public String parseFieldHtml(TagElement e, int endLength) {
	    return getHtmlText(e.getAnalyzer(), e.getStartLength(), endLength); 
	}
	
	public String parseFieldText(TagElement e, int endLength) {
	    return getSimpleText(e.getAnalyzer(), e.getStartLength(), endLength); 
	}
	
	protected String getSimpleText(HTMLHandler a, int startLength, int endLength) { 
		if (a != null) { 
			String text = a.getString(startLength, endLength); 
			text = ParseUtils.extractContentFromHtml(text); 
			text = ParseUtils.trim(ParseUtils.removeWhiteSpaces(text)); 
			return text; 
		} else 
			return null; 
	}
	
	protected String getHtmlText(HTMLHandler a, int startLength, int endLength) { 
		if (a != null) { 
			String text = a.getString(startLength, endLength); 
			text = ParseUtils.trim(ParseUtils.removeWhiteSpaces(text)); 
			return text; 
		} else 
			return null; 
	}
	
}
