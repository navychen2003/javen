package org.javenstudio.android.reader;

import java.util.Map;

import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationBinderFactory;
import org.javenstudio.android.information.InformationOne;
import org.javenstudio.android.information.InformationSource;
import org.javenstudio.android.information.comment.CommentItem;
import org.javenstudio.android.information.comment.CommentItemBase;
import org.javenstudio.android.information.comment.CommentTable;
import org.javenstudio.cocoka.android.MainMethods;
import org.javenstudio.cocoka.android.ModuleManager;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.TagElement;
import org.javenstudio.common.parser.html.TagTree;

public class CommentItemMethods {

	public static void registerMethods() { 
		final Class<?> clazz = CommentItemMethods.class;
		
		MainMethods.registerMethod(clazz, "newCommentItem", 
				Object.class, String.class, Map.class);
		MainMethods.registerMethod(clazz, "newPhotoItem", 
				Object.class, String.class, Map.class);
		MainMethods.registerMethod(clazz, "parseFieldDefault", 
				Object.class, String.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "parseFieldHtml", 
				Object.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "parseFieldText", 
				Object.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "parseElementAttribute", 
				Object.class, String.class);
		MainMethods.registerMethod(clazz, "getCommentTableValue", 
				Object.class, String.class);
		MainMethods.registerMethod(clazz, "getDocumentTableValue", 
				Object.class, String.class);
	}
	
	public static CommentItem newCommentItem(Object moduleClass, 
			String location, Map<String, Object> attrs) { 
		return new CommentItemImpl((ModuleManager.ModuleClass)moduleClass, 
				InformationBinderFactory.getInstance().getCommentSourceBinder(), 
				location, attrs);
	}
	
	public static CommentItem newPhotoItem(Object moduleClass, 
			String location, Map<String, Object> attrs) { 
		return new CommentItemImpl((ModuleManager.ModuleClass)moduleClass, 
				InformationBinderFactory.getInstance().getPhotoSourceBinder(), 
				location, attrs);
	}
	
	public static String parseFieldDefault(Object commentItem, 
			String fieldName, Object element, int endLength) { 
		CommentItem item = (CommentItem)commentItem;
		return item.parseFieldDefault(fieldName, (TagElement)element, endLength);
	}
	
	public static String parseFieldHtml(Object commentItem, Object element, int endLength) { 
		CommentItem item = (CommentItem)commentItem;
		return item.parseFieldHtml((TagElement)element, endLength);
	}
	
	public static String parseFieldText(Object commentItem, Object element, int endLength) { 
		CommentItem item = (CommentItem)commentItem;
		return item.parseFieldText((TagElement)element, endLength);
	}
	
	public static String parseElementAttribute(Object element, String attrName) { 
		return ((TagElement)element).getAttribute(attrName);
	}
	
	public static Object getCommentTableValue(Object commentTable, String fieldName) { 
		if (commentTable != null) { 
			CommentTable table = (CommentTable)commentTable;
			return table.getTableValue(fieldName);
		}
		return null;
	}
	
	public static Object getDocumentTableValue(Object documentTable, String fieldName) { 
		if (documentTable != null) { 
			CommentItemBase.DocumentTable table = (CommentItemBase.DocumentTable)documentTable;
			return table.getTableValue(fieldName);
		}
		return null;
	}
	
	static class CommentItemImpl extends CommentItem { 
		
		//private final ModuleManager.ModuleMethod mGetInformationURL; 
		private final ModuleManager.ModuleMethod mOnInitAnalyzer; 
		private final ModuleManager.ModuleMethod mNewLocationsTagTree; 
		
		private final ModuleManager.ModuleMethod mNewDocumentTitleTagTree; 
		private final ModuleManager.ModuleMethod mNewDocumentSubjectTagTree; 
		private final ModuleManager.ModuleMethod mNewDocumentSummaryTagTree; 
		private final ModuleManager.ModuleMethod mNewDocumentAuthorTagTree; 
		private final ModuleManager.ModuleMethod mNewDocumentDateTagTree; 
		private final ModuleManager.ModuleMethod mNewDocumentContentTagTree; 
		private final ModuleManager.ModuleMethod mNewDocumentImageTagTree; 
		
		private final ModuleManager.ModuleMethod mNewSubContentTitleTagTree; 
		private final ModuleManager.ModuleMethod mNewSubContentSubjectTagTree; 
		private final ModuleManager.ModuleMethod mNewSubContentSummaryTagTree; 
		private final ModuleManager.ModuleMethod mNewSubContentAuthorTagTree; 
		private final ModuleManager.ModuleMethod mNewSubContentDateTagTree; 
		private final ModuleManager.ModuleMethod mNewSubContentContentTagTree; 
		private final ModuleManager.ModuleMethod mNewSubContentImageTagTree; 
		
		private final ModuleManager.ModuleMethod mParseField; 
		private final ModuleManager.ModuleMethod mParseLocations; 
		private final ModuleManager.ModuleMethod mParseContentPath; 
		private final ModuleManager.ModuleMethod mGetInformationField; 
		private final ModuleManager.ModuleMethod mAddDocumentInformation; 
		private final ModuleManager.ModuleMethod mGetDefaultCharset; 
		
		private final Object mObject;
		
		public CommentItemImpl(ModuleManager.ModuleClass moduleClass, 
				InformationSource.SourceBinder binder, String location, 
				Map<String, Object> attrs) { 
			super(binder, location, attrs);
			
			//mGetInformationURL = moduleClass.getMethod("getInformationURL");
			mOnInitAnalyzer = moduleClass.getMethod("onInitAnalyzer");
			mNewLocationsTagTree = moduleClass.getMethod("newLocationsTagTree");
			
			mNewDocumentTitleTagTree = moduleClass.getMethod("newDocumentTitleTagTree");
			mNewDocumentSubjectTagTree = moduleClass.getMethod("newDocumentSubjectTagTree");
			mNewDocumentSummaryTagTree = moduleClass.getMethod("newDocumentSummaryTagTree");
			mNewDocumentAuthorTagTree = moduleClass.getMethod("newDocumentAuthorTagTree");
			mNewDocumentDateTagTree = moduleClass.getMethod("newDocumentDateTagTree");
			mNewDocumentContentTagTree = moduleClass.getMethod("newDocumentContentTagTree");
			mNewDocumentImageTagTree = moduleClass.getMethod("newDocumentImageTagTree");
			
			mNewSubContentTitleTagTree = moduleClass.getMethod("newSubContentTitleTagTree");
			mNewSubContentSubjectTagTree = moduleClass.getMethod("newSubContentSubjectTagTree");
			mNewSubContentSummaryTagTree = moduleClass.getMethod("newSubContentSummaryTagTree");
			mNewSubContentAuthorTagTree = moduleClass.getMethod("newSubContentAuthorTagTree");
			mNewSubContentDateTagTree = moduleClass.getMethod("newSubContentDateTagTree");
			mNewSubContentContentTagTree = moduleClass.getMethod("newSubContentContentTagTree");
			mNewSubContentImageTagTree = moduleClass.getMethod("newSubContentImageTagTree");
			
			mParseField = moduleClass.getMethod("parseField");
			mParseLocations = moduleClass.getMethod("parseLocations");
			mParseContentPath = moduleClass.getMethod("parseContentPath");
			mGetInformationField = moduleClass.getMethod("getInformationField");
			mAddDocumentInformation = moduleClass.getMethod("addDocumentInformation");
			mGetDefaultCharset = moduleClass.getMethod("getDefaultCharset");
			
			ModuleManager.ModuleMethod method = moduleClass.getMethod("newCommentItemObject");
			mObject = method.invoke(this, location);
		}
		
		@Override 
		protected void onInitAnalyzer(HTMLHandler a) { 
			if (mOnInitAnalyzer.existMethod())
				mOnInitAnalyzer.invoke(mObject, a); 
			else
				super.onInitAnalyzer(a);
		}
		
		@Override 
		protected TagTree newLocationsTagTree(CommentTable item) { 
			if (mNewLocationsTagTree.existMethod())
				return (TagTree)mNewLocationsTagTree.invoke(this, item);
			else
				return super.newLocationsTagTree(item);
		}
		
		@Override 
		protected TagTree newDocumentTitleTagTree(CommentTable item) { 
			if (mNewDocumentTitleTagTree.existMethod())
				return (TagTree)mNewDocumentTitleTagTree.invoke(mObject, item);
			else
				return super.newDocumentTitleTagTree(item);
		}
		
		@Override 
		protected TagTree newDocumentSubjectTagTree(CommentTable item) { 
			if (mNewDocumentSubjectTagTree.existMethod())
				return (TagTree)mNewDocumentSubjectTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newDocumentSummaryTagTree(CommentTable item) { 
			if (mNewDocumentSummaryTagTree.existMethod())
				return (TagTree)mNewDocumentSummaryTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newDocumentAuthorTagTree(CommentTable item) { 
			if (mNewDocumentAuthorTagTree.existMethod())
				return (TagTree)mNewDocumentAuthorTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newDocumentDateTagTree(CommentTable item) { 
			if (mNewDocumentDateTagTree.existMethod())
				return (TagTree)mNewDocumentDateTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newDocumentContentTagTree(CommentTable item) { 
			if (mNewDocumentContentTagTree.existMethod())
				return (TagTree)mNewDocumentContentTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newDocumentImageTagTree(CommentTable item) { 
			if (mNewDocumentImageTagTree.existMethod())
				return (TagTree)mNewDocumentImageTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newSubContentTitleTagTree(CommentTable item) { 
			if (mNewSubContentTitleTagTree.existMethod())
				return (TagTree)mNewSubContentTitleTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newSubContentSubjectTagTree(CommentTable item) { 
			if (mNewSubContentSubjectTagTree.existMethod())
				return (TagTree)mNewSubContentSubjectTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newSubContentSummaryTagTree(CommentTable item) { 
			if (mNewSubContentSummaryTagTree.existMethod())
				return (TagTree)mNewSubContentSummaryTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newSubContentAuthorTagTree(CommentTable item) { 
			if (mNewSubContentAuthorTagTree.existMethod())
				return (TagTree)mNewSubContentAuthorTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newSubContentDateTagTree(CommentTable item) { 
			if (mNewSubContentDateTagTree.existMethod())
				return (TagTree)mNewSubContentDateTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newSubContentContentTagTree(CommentTable item) { 
			if (mNewSubContentContentTagTree.existMethod())
				return (TagTree)mNewSubContentContentTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected TagTree newSubContentImageTagTree(CommentTable item) { 
			if (mNewSubContentImageTagTree.existMethod())
				return (TagTree)mNewSubContentImageTagTree.invoke(mObject, item);
			
			return null; 
		}
		
		@Override 
		protected String parseField(String fieldName, TagElement e, int endLength) {
			if (mParseField.existMethod())
				return (String)mParseField.invoke(mObject, fieldName, e, endLength);
			else 
				return super.parseField(fieldName, e, endLength); 
		}
		
		@Override 
		protected String parseContentPath(String fieldName, String content) {
			if (mParseContentPath.existMethod())
				return (String)mParseContentPath.invoke(mObject, fieldName, content);
			else 
				return super.parseContentPath(fieldName, content); 
		}
		
		@Override 
		protected String[] parseLocations(String location, TagElement e, int endLength) { 
			if (mParseLocations.existMethod())
				return (String[])mParseLocations.invoke(mObject, location, e, endLength);
			else
				return super.parseLocations(location, e, endLength);
		}
		
		@Override 
		protected Information toInformation(CommentTable table) { 
			if (mGetInformationField.existMethod()) {
				DocumentTable doc = getDocumentTable(); 
				InformationOne info = CommentTable.createInformation(this); 
				
				String subject = (String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_SUBJECT);
				String title = (String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_TITLE);
				info.setTitle(title != null && title.length() > 0 ? title : subject);
				
				info.setSummary((String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_SUMMARY));
				info.setDate((String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_DATE));
				info.setAuthor((String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_AUTHOR));
				info.setLink((String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_LINK));
				info.setContent((String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_CONTENT));
				info.setImage((String)mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_IMAGE));
				
				info.setField(Information.EXTEND_IMAGE_KEY, mGetInformationField.invoke(mObject, doc, table, 
						CommentTable.FIELD_EXTENDIMAGE));
				
				return info;
			}
			
			return super.toInformation(table);
		}
		
		@Override 
		protected void addDocumentInformation(DocumentTable content) { 
			if (mAddDocumentInformation.existMethod()) 
				mAddDocumentInformation.invoke(mObject, content);
			else
				super.addDocumentInformation(content);
		}
		
		@Override
		public String getDefaultCharset(String location) { 
			String charset = null; 
			
			if (mGetDefaultCharset.existMethod())
				charset = (String)mGetDefaultCharset.invoke(mObject, location);
			
			if (charset == null)
				charset = super.getDefaultCharset(location);
			
			return charset;
		}
	}
	
}
