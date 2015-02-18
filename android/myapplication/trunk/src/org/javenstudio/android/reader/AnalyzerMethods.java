package org.javenstudio.android.reader;

import org.javenstudio.cocoka.android.MainMethods;
import org.javenstudio.cocoka.android.ModuleManager;
import org.javenstudio.common.parser.html.HTMLHandler;
import org.javenstudio.common.parser.html.Tag;
import org.javenstudio.common.parser.html.TagElement;
import org.javenstudio.common.parser.html.TagTree;
import org.javenstudio.android.information.comment.CommentListTable;
import org.javenstudio.android.information.comment.CommentTable;

public class AnalyzerMethods {

	public static void registerMethods() { 
		final Class<?> clazz = AnalyzerMethods.class;
		
		MainMethods.registerMethod(clazz, "newSubContentBeginTag", 
				Object.class, String.class, String[].class, String[].class);
		
		MainMethods.registerMethod(clazz, "newSubContentEndTag", 
				Object.class, String.class, String[].class, String[].class);
		
		MainMethods.registerMethod(clazz, "newSubContentTag", 
				Object.class, String.class, String[].class, String[].class);
		
		MainMethods.registerMethod(clazz, "newSubContentTagWithMatcher", 
				Object.class, String.class, Object.class);
		
		MainMethods.registerMethod(clazz, "newTagChild", 
				Object.class, String.class, String[].class, String[].class);
		
		MainMethods.registerMethod(clazz, "newTagChildWithMatcher", 
				Object.class, String.class, Object.class);
		
		MainMethods.registerMethod(clazz, "getTagTreeRootTag", 
				Object.class);
		
		MainMethods.registerMethod(clazz, "newCommentListTableTagTree", 
				Object.class, String.class, String[].class, String[].class);
		
		MainMethods.registerMethod(clazz, "newCommentListTableTagTreeWithMatcher", 
				Object.class, String.class, Object.class);
		
		MainMethods.registerMethod(clazz, "newCommentTableTagTree", 
				Object.class, String.class, String[].class, String[].class);
		
		MainMethods.registerMethod(clazz, "newCommentTableTagTreeWithMatcher", 
				Object.class, String.class, Object.class);
		
		MainMethods.registerMethod(clazz, "getTagElementAttribute", 
				Object.class, String.class);
		
		MainMethods.registerMethod(clazz, "newAttributesMatcher", 
				Object.class, Object.class);
	}
	
	public static Tag.AttributesMatcher newAttributesMatcher(Object moduleClass, Object matcher) { 
		return new AttributesMatcherImpl((ModuleManager.ModuleClass)moduleClass, matcher);
	}
	
	public static Tag newSubContentBeginTag(Object analyzer, String name, String[] attrNames, String[] attrValues) { 
		HTMLHandler a = (HTMLHandler)analyzer;
		return a.newSubContentBeginTag(name, attrNames, attrValues);
	}
	
	public static Tag newSubContentEndTag(Object analyzer, String name, String[] attrNames, String[] attrValues) { 
		HTMLHandler a = (HTMLHandler)analyzer;
		return a.newSubContentEndTag(name, attrNames, attrValues);
	}
	
	public static Tag newSubContentTag(Object analyzer, String name, String[] attrNames, String[] attrValues) { 
		HTMLHandler a = (HTMLHandler)analyzer;
		return a.newSubContentTag(name, attrNames, attrValues);
	}
	
	public static Tag newSubContentTagWithMatcher(Object analyzer, String name, Object matcher) { 
		HTMLHandler a = (HTMLHandler)analyzer;
		return a.newSubContentTag(name, (Tag.AttributesMatcher)matcher);
	}
	
	public static Tag newTagChild(Object tag, String name, String[] attrNames, String[] attrValues) { 
		Tag t = (Tag)tag;
		return t.newChild(name, attrNames, attrValues);
	}
	
	public static Tag newTagChildWithMatcher(Object tag, String name, Object matcher) { 
		Tag t = (Tag)tag;
		return t.newChild(name, (Tag.AttributesMatcher)matcher);
	}
	
	public static Tag getTagTreeRootTag(Object tagTree) { 
		TagTree t = (TagTree)tagTree;
		return t.getRootTag();
	}
	
	public static TagTree newCommentListTableTagTree(Object commentListTable, String name, String[] attrNames, String[] attrValues) { 
		CommentListTable t = (CommentListTable)commentListTable;
		return t.newTagTree(name, attrNames, attrValues);
	}
	
	public static TagTree newCommentListTableTagTreeWithMatcher(Object commentListTable, String name, Object matcher) { 
		CommentListTable t = (CommentListTable)commentListTable;
		return t.newTagTree(name, (Tag.AttributesMatcher)matcher);
	}
	
	public static TagTree newCommentTableTagTree(Object commentTable, String name, String[] attrNames, String[] attrValues) { 
		CommentTable t = (CommentTable)commentTable;
		return t.newTagTree(name, attrNames, attrValues);
	}
	
	public static TagTree newCommentTableTagTreeWithMatcher(Object commentTable, String name, Object matcher) { 
		CommentTable t = (CommentTable)commentTable;
		return t.newTagTree(name, (Tag.AttributesMatcher)matcher);
	}
	
	public static String getTagElementAttribute(Object tagElement, String name) { 
		TagElement e = (TagElement)tagElement;
		return e.getAttribute(name);
	}
	
	static class AttributesMatcherImpl implements Tag.AttributesMatcher {
		private final ModuleManager.ModuleMethod mCheckAttribute; 
		private final ModuleManager.ModuleMethod mCheckAttributesMatch; 
		private final Object mMatcher;
		
		public AttributesMatcherImpl(ModuleManager.ModuleClass moduleClass, Object matcher) { 
			mCheckAttribute = moduleClass.getMethod("checkAttribute");
			mCheckAttributesMatch = moduleClass.getMethod("checkAttributesMatch");
			mMatcher = matcher;
		}
		
		@Override
		public boolean matchAttributes(Tag.IAttributes attributes) { 
			for (int i=0; i < attributes.getLength(); i++) { 
				String name = attributes.getLocalName(i); 
				String value = attributes.getValue(i); 
				if (name == null || value == null) 
					continue; 
				
				mCheckAttribute.invoke(mMatcher, name, value);
			}
			
			return (Boolean)mCheckAttributesMatch.invoke(mMatcher);
		}
	}
	
}
