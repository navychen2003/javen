package org.javenstudio.android.information.comment;

import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.common.parser.ParseException;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.ContentField;
import org.javenstudio.common.parser.html.ContentHandler;
import org.javenstudio.common.parser.html.ContentHandlerFactory;
import org.javenstudio.common.parser.html.ContentTable;
import org.javenstudio.common.parser.html.ContentTableFactory;
import org.javenstudio.common.parser.html.ContentTableHandler;
import org.javenstudio.common.parser.html.TagElement;
import org.javenstudio.common.parser.html.TagTree;
import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.android.information.InformationNavItem;
import org.javenstudio.android.information.InformationOne;

public abstract class CommentListBase {
	//private static final Logger LOG = Logger.getLogger(CommentListBase.class);

	private final InformationNavItem.NavBinder mBinder;
	private DocumentTable mDocumentTable = null;
	
	public CommentListBase(InformationNavItem.NavBinder res) { 
		mBinder = res;
	}
	
	protected abstract void onInitAnalyzer(HTMLHandler a); 
	
	protected TagTree newTitleTagTree(CommentListTable item) { 
		return null;
	}
	
	protected TagTree newSubjectTagTree(CommentListTable item) { 
		return null;
	}
	
	protected TagTree newSummaryTagTree(CommentListTable item) { 
		return null;
	}
	
	protected TagTree newContentTagTree(CommentListTable item) { 
		return null;
	}
	
	protected TagTree newImageTagTree(CommentListTable item) { 
		return null;
	}
	
	protected TagTree newAuthorTagTree(CommentListTable item) { 
		return null;
	}
	
	protected TagTree newDateTagTree(CommentListTable item) { 
		return null;
	}
	
	protected TagTree newLinkTagTree(CommentListTable item) { 
		return newSubjectTagTree(item);
	}
	
	protected TagTree newAdminCommentTagTree(CommentListTable item) { 
		return null; 
	}
	
	protected TagTree newLocationsTagTree(CommentListTable item) { 
		return null; 
	}
	
	protected ContentField newContentField(CommentListTable item, String fieldName, TagTree root) { 
		return root != null ? new ContentField(item, fieldName, root) : null; 
	}
	
	protected InformationOne newInformation(NavItem item, CommentListTable table) { 
		return item.toInformationDefault(table);
	}
	
	public NavItem newNavItem(NavigationInfo info) { 
		return new NavItem(mBinder, info); 
	}
	
	protected class NavItem extends CommentNavItem { 
		public NavItem(NavBinder res, NavigationInfo info) { 
			super(res, info); 
		}
		
		@Override 
		protected void onInitAnalyzer(HTMLHandler a) { 
			CommentListBase.this.onInitAnalyzer(a); 
		}
		
		@Override 
		protected ContentHandlerFactory getContentFactory() { 
			return getHandlerFactory(this); 
		}
		
		@Override 
		protected InformationOne toInformation(CommentListTable table) { 
			return newInformation(this, table);
		}
		
		protected InformationOne toInformationDefault(CommentListTable table) { 
			return super.toInformation(table);
		}
		
		@Override 
		protected void onInformationParsed(String location, boolean first) { 
			super.onInformationParsed(location, first);
			
			DocumentTable table = getDocumentTable();
			if (table != null && first) { 
				addPages(table.getTableValue(CommentTable.FIELD_LOCATIONS));
			}
		}
		
		private ContentHandlerFactory getHandlerFactory(ContentTableHandler tableHandler) { 
			final ContentTableFactory docFactory = new ContentTableFactory() { 
					public ContentTable createContentTable(ContentHandler handler) { 
						try { 
							return newDocumentTable(handler, getLocation()); 
						} catch (Exception e) { 
							throw new RuntimeException(e); 
						}
					}
				};
			
			final ContentTableFactory tableFactory = new ContentTableFactory() { 
					public ContentTable createContentTable(ContentHandler handler) { 
						try { 
							return newContentTable(handler, getLocation()); 
						} catch (Exception e) { 
							throw new RuntimeException(e); 
						}
					}
				};
			
			return new ContentHandlerFactory(docFactory, tableFactory, tableHandler); 
		}
	}
	
	protected DocumentTable getDocumentTable() { 
		return mDocumentTable;
	}
	
	protected ContentTable newDocumentTable(ContentHandler handler, String location) 
			throws ParseException { 
		DocumentTable impl = new DocumentTable(handler, location); 
		mDocumentTable = impl;
		return impl;
	}
	
	protected ContentTable newContentTable(ContentHandler handler, String location) 
			throws ParseException { 
		return new CommentListTableImpl(handler, location); 
	}
	
	public class DocumentTable extends CommentListTableImpl {
		public DocumentTable(ContentHandler handler, String location) 
				throws ParseException { 
			super(handler, location); 
		}
		
		@Override 
		public void onInitFields() { 
			addField(newContentField(this, CommentTable.FIELD_LOCATIONS, newLocationsTagTree(this))); 
		}
	}
	
	protected class CommentListTableImpl extends CommentListTable {
		public CommentListTableImpl(ContentHandler handler, String location) 
				throws ParseException { 
			super(handler, location); 
		}
		
		@Override 
		public void onInitFields() { 
			addField(newContentField(this, CommentTable.FIELD_ADMINCOMMENT, newAdminCommentTagTree(this)));
			
			addField(newContentField(this, CommentTable.FIELD_AUTHOR, newAuthorTagTree(this))); 
			addField(newContentField(this, CommentTable.FIELD_TITLE, newTitleTagTree(this))); 
			addField(newContentField(this, CommentTable.FIELD_SUBJECT, newSubjectTagTree(this))); 
			addField(newContentField(this, CommentTable.FIELD_SUMMARY, newSummaryTagTree(this))); 
			addField(newContentField(this, CommentTable.FIELD_CONTENT, newContentTagTree(this))); 
			addField(newContentField(this, CommentTable.FIELD_IMAGE, newImageTagTree(this))); 
			addField(newContentField(this, CommentTable.FIELD_LINK, newLinkTagTree(this))); 
			addField(newContentField(this, CommentTable.FIELD_DATE, newDateTagTree(this))); 
		}
		
		@Override 
		public void onFoundField(ContentField field, TagElement e, int endLength) { 
			if (field == null || e == null) 
				return; 
			
			setCommentListTableValue(this, field.getName(), e, endLength); 
		}
	}
	
	protected void setCommentListTableValue(CommentListTable table, 
			String fieldName, TagElement e, int endLength) { 
		if (CommentTable.FIELD_ADMINCOMMENT.equals(fieldName)) { 
			table.setAdminComment(parseAdminComment(e, endLength)); 
			return; 
			
		} else if (CommentTable.FIELD_LOCATIONS.equals(fieldName)) { 
			table.setTableValue(CommentTable.FIELD_LOCATIONS, 
					parseLocations(table.getLocation(), e, endLength)); 
			return; 
			
		} else if (CommentTable.FIELD_LINK.equals(fieldName)) { 
			table.setTableValue(CommentTable.FIELD_LINK, parseLink(e, endLength)); 
			return;
		}
		
		table.setTableValue(fieldName, parseField(fieldName, e, endLength)); 
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
	
	protected String[] parseLocations(String location, TagElement e, int endLength) { 
		return null; 
	}
	
	protected boolean parseAdminComment(TagElement e, int endLength) { 
		return parseAdminCommentDefault(e, endLength); 
	}
	
	public boolean parseAdminCommentDefault(TagElement e, int endLength) { 
		return false; 
	}
	
	protected String parseLink(TagElement e, int endLength) { 
		return parseLinkDefault(e, endLength); 
	}
	
	public String parseLinkDefault(TagElement e, int endLength) { 
		return e.getAttribute("href"); 
	}
	
	public String parseAttribute(TagElement e, String name) { 
		return e.getAttribute(name);
	}
	
	protected String getSimpleText(HTMLHandler a, int startLength, int endLength) { 
		if (a != null) { 
			String text = ParseUtils.extractContentFromHtml(a.getString(startLength, endLength)); 
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
