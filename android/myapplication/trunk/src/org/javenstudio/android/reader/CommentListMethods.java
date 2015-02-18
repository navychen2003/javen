package org.javenstudio.android.reader;

import org.javenstudio.cocoka.android.MainMethods;
import org.javenstudio.cocoka.android.ModuleManager;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.TagElement;
import org.javenstudio.common.parser.html.TagTree;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationBinderFactory;
import org.javenstudio.android.information.InformationNavItem;
import org.javenstudio.android.information.InformationOne;
import org.javenstudio.android.information.comment.CommentListItem;
import org.javenstudio.android.information.comment.CommentListTable;
import org.javenstudio.android.information.comment.CommentTable;

public class CommentListMethods {

	public static void registerMethods() { 
		final Class<?> clazz = CommentListMethods.class;
		
		MainMethods.registerMethod(clazz, "newCommentList", 
				Object.class);
		MainMethods.registerMethod(clazz, "newCommentPhotoList", 
				Object.class);
		MainMethods.registerMethod(clazz, "newCommentNewsList", 
				Object.class);
		MainMethods.registerMethod(clazz, "parseFieldDefault", 
				Object.class, String.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "parseFieldHtml", 
				Object.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "parseFieldText", 
				Object.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "parseLinkDefault", 
				Object.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "parseAttribute", 
				Object.class, Object.class, String.class);
		MainMethods.registerMethod(clazz, "parseAdminCommentDefault", 
				Object.class, Object.class, int.class);
		MainMethods.registerMethod(clazz, "getDocumentTableValue", 
				Object.class, String.class);
	}
	
	public static CommentListItem newCommentList(Object moduleClass) { 
		return new CommentListItemImpl((ModuleManager.ModuleClass)moduleClass, 
				InformationBinderFactory.getInstance().getCommentListBinder());
	}
	
	public static CommentListItem newCommentPhotoList(Object moduleClass) { 
		return new CommentListItemImpl((ModuleManager.ModuleClass)moduleClass, 
				InformationBinderFactory.getInstance().getPhotoListBinder());
	}
	
	public static CommentListItem newCommentNewsList(Object moduleClass) { 
		return new CommentListItemImpl((ModuleManager.ModuleClass)moduleClass, 
				InformationBinderFactory.getInstance().getNewsListBinder());
	}
	
	public static String parseFieldDefault(Object commentList, String fieldName, Object element, int endLength) { 
		CommentListItem list = (CommentListItem)commentList;
		return list.parseFieldDefault(fieldName, (TagElement)element, endLength);
	}
	
	public static String parseFieldHtml(Object commentList, Object element, int endLength) { 
		CommentListItem list = (CommentListItem)commentList;
		return list.parseFieldHtml((TagElement)element, endLength);
	}
	
	public static String parseFieldText(Object commentList, Object element, int endLength) { 
		CommentListItem list = (CommentListItem)commentList;
		return list.parseFieldText((TagElement)element, endLength);
	}
	
	public static String parseLinkDefault(Object commentList, Object element, int endLength) { 
		CommentListItem list = (CommentListItem)commentList;
		return list.parseLinkDefault((TagElement)element, endLength);
	}
	
	public static String parseAttribute(Object commentList, Object element, String name) { 
		CommentListItem list = (CommentListItem)commentList;
		return list.parseAttribute((TagElement)element, name);
	}
	
	public static boolean parseAdminCommentDefault(Object commentList, Object element, int endLength) { 
		CommentListItem list = (CommentListItem)commentList;
		return list.parseAdminCommentDefault((TagElement)element, endLength);
	}
	
	public static Object getDocumentTableValue(Object documentTable, String fieldName) { 
		if (documentTable != null) { 
			CommentListTable table = (CommentListTable)documentTable;
			return table.getTableValue(fieldName);
		}
		return null;
	}
	
	static class CommentListItemImpl extends CommentListItem {  
		
		private final ModuleManager.ModuleMethod mOnInitAnalyzer; 
		private final ModuleManager.ModuleMethod mNewAdminCommentTagTree; 
		private final ModuleManager.ModuleMethod mNewLocationsTagTree; 
		
		private final ModuleManager.ModuleMethod mNewTitleTagTree; 
		private final ModuleManager.ModuleMethod mNewSubjectTagTree; 
		private final ModuleManager.ModuleMethod mNewSummaryTagTree; 
		private final ModuleManager.ModuleMethod mNewContentTagTree; 
		private final ModuleManager.ModuleMethod mNewImageTagTree; 
		private final ModuleManager.ModuleMethod mNewLinkTagTree; 
		private final ModuleManager.ModuleMethod mNewAuthorTagTree; 
		private final ModuleManager.ModuleMethod mNewDateTagTree; 
		
		private final ModuleManager.ModuleMethod mParseLink; 
		private final ModuleManager.ModuleMethod mParseField; 
		private final ModuleManager.ModuleMethod mParseLocations; 
		private final ModuleManager.ModuleMethod mParseAdminComment; 
		private final ModuleManager.ModuleMethod mGetInformationField; 
		
		public CommentListItemImpl(ModuleManager.ModuleClass moduleClass, 
				InformationNavItem.NavBinder binder) { 
			super(binder);
			
			mOnInitAnalyzer = moduleClass.getMethod("onInitAnalyzer");
			mNewAdminCommentTagTree = moduleClass.getMethod("newAdminCommentTagTree");
			mNewLocationsTagTree = moduleClass.getMethod("newLocationsTagTree");
			
			mNewTitleTagTree = moduleClass.getMethod("newTitleTagTree");
			mNewSubjectTagTree = moduleClass.getMethod("newSubjectTagTree");
			mNewSummaryTagTree = moduleClass.getMethod("newSummaryTagTree");
			mNewContentTagTree = moduleClass.getMethod("newContentTagTree");
			mNewImageTagTree = moduleClass.getMethod("newImageTagTree");
			mNewLinkTagTree = moduleClass.getMethod("newLinkTagTree");
			mNewAuthorTagTree = moduleClass.getMethod("newAuthorTagTree");
			mNewDateTagTree = moduleClass.getMethod("newDateTagTree");
			
			mParseLink = moduleClass.getMethod("parseLink");
			mParseField = moduleClass.getMethod("parseField");
			mParseLocations = moduleClass.getMethod("parseLocations");
			mParseAdminComment = moduleClass.getMethod("parseAdminComment");
			mGetInformationField = moduleClass.getMethod("getInformationField");
		}
		
		@Override
		protected void onInitAnalyzer(HTMLHandler a) { 
			mOnInitAnalyzer.invoke(this, a); 
		}
		
		@Override 
		protected TagTree newAdminCommentTagTree(CommentListTable item) { 
			if (mNewAdminCommentTagTree.existMethod())
				return (TagTree)mNewAdminCommentTagTree.invoke(this, item);
			else
				return super.newAdminCommentTagTree(item);
		}
		
		@Override 
		protected TagTree newLocationsTagTree(CommentListTable item) { 
			if (mNewLocationsTagTree.existMethod())
				return (TagTree)mNewLocationsTagTree.invoke(this, item);
			else
				return super.newLocationsTagTree(item);
		}
		
		@Override 
		protected TagTree newLinkTagTree(CommentListTable item) { 
			if (mNewLinkTagTree.existMethod())
				return (TagTree)mNewLinkTagTree.invoke(this, item);
			else
				return super.newLinkTagTree(item);
		}
		
		@Override 
		protected TagTree newTitleTagTree(CommentListTable item) { 
			if (mNewTitleTagTree.existMethod())
				return (TagTree)mNewTitleTagTree.invoke(this, item);
			else
				return super.newTitleTagTree(item);
		}
		
		@Override 
		protected TagTree newSubjectTagTree(CommentListTable item) { 
			if (mNewSubjectTagTree.existMethod())
				return (TagTree)mNewSubjectTagTree.invoke(this, item);
			else
				return super.newSubjectTagTree(item);
		}
		
		@Override 
		protected TagTree newSummaryTagTree(CommentListTable item) { 
			if (mNewSummaryTagTree.existMethod())
				return (TagTree)mNewSummaryTagTree.invoke(this, item);
			else
				return super.newSummaryTagTree(item);
		}
		
		@Override 
		protected TagTree newContentTagTree(CommentListTable item) { 
			if (mNewContentTagTree.existMethod())
				return (TagTree)mNewContentTagTree.invoke(this, item);
			else
				return super.newContentTagTree(item);
		}
		
		@Override 
		protected TagTree newImageTagTree(CommentListTable item) { 
			if (mNewImageTagTree.existMethod())
				return (TagTree)mNewImageTagTree.invoke(this, item);
			else
				return super.newImageTagTree(item);
		}
		
		@Override 
		protected TagTree newAuthorTagTree(CommentListTable item) { 
			if (mNewAuthorTagTree.existMethod())
				return (TagTree)mNewAuthorTagTree.invoke(this, item);
			else
				return super.newAuthorTagTree(item);
		}
		
		@Override 
		protected TagTree newDateTagTree(CommentListTable item) { 
			if (mNewDateTagTree.existMethod())
				return (TagTree)mNewDateTagTree.invoke(this, item);
			else
				return super.newDateTagTree(item);
		}
		
		@Override 
		protected String parseLink(TagElement e, int endLength) { 
			if (mParseLink.existMethod())
				return (String)mParseLink.invoke(this, e, endLength);
			else
				return super.parseLink(e, endLength);
		}
		
		@Override 
		protected String parseField(String fieldName, TagElement e, int endLength) {
			if (mParseField.existMethod())
				return (String)mParseField.invoke(this, fieldName, e, endLength);
			else
				return super.parseField(fieldName, e, endLength);
		}
		
		@Override 
		protected boolean parseAdminComment(TagElement e, int endLength) { 
			if (mParseAdminComment.existMethod())
				return (Boolean)mParseAdminComment.invoke(this, e, endLength);
			else
				return super.parseAdminComment(e, endLength);
		}
		
		@Override 
		protected String[] parseLocations(String location, TagElement e, int endLength) { 
			if (mParseLocations.existMethod())
				return (String[])mParseLocations.invoke(this, location, e, endLength);
			else
				return super.parseLocations(location, e, endLength);
		}
		
		@Override 
		protected InformationOne newInformation(NavItem item, CommentListTable table) { 
			if (mGetInformationField.existMethod()) {
				InformationOne info = CommentListTable.createInformation(item, table); 
				
				String subject = (String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_SUBJECT);
				String title = (String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_TITLE);
				info.setTitle(title != null && title.length() > 0 ? title : subject);
				
				info.setSummary((String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_SUMMARY));
				info.setDate((String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_DATE));
				info.setAuthor((String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_AUTHOR));
				info.setLink((String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_LINK));
				info.setContent((String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_CONTENT));
				info.setImage((String)mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_IMAGE));
				
				info.setField(Information.EXTEND_IMAGE_KEY, mGetInformationField.invoke(this, table, 
						CommentTable.FIELD_EXTENDIMAGE));
				
				return info;
			}
			
			return super.newInformation(item, table);
		}
	}
	
}
