package org.javenstudio.falcon.util;

import org.javenstudio.falcon.util.NamedList;

/**
 * MBean interface for getting various ui friendly strings and URLs
 * for use by objects which are 'pluggable' to make server administration
 * easier.
 */
public interface InfoMBean {
	
	public static final String CATEGORY_CORE = "CORE";
	public static final String CATEGORY_OTHER = "OTHER";
	public static final String CATEGORY_CACHE = "CACHE";
	public static final String CATEGORY_HIGHLIGHTING = "HIGHLIGHTING";
	public static final String CATEGORY_QUERYHANDLER = "QUERYHANDLER";
	public static final String CATEGORY_UPDATEHANDLER = "UPDATEHANDLER";
	
	public String getMBeanKey();
	
	/**
	 * Simple common usage name, e.g. BasicQueryHandler,
	 * or fully qualified clas name.
	 */
	public String getMBeanName();
	
	/** Simple common usage version, e.g. 2.0 */
	public String getMBeanVersion();
	
	/** Simple one or two line description */
	public String getMBeanDescription();
	
	/** Purpose of this Class */
	public String getMBeanCategory();
	
	/** CVS Source, SVN Source, etc */
	//public String getMBeanSource();
	
	/**
	 * Documentation URL list.
	 *
	 * <p>
	 * Suggested documentation URLs: Homepage for sponsoring project,
	 * FAQ on class usage, Design doc for class, Wiki, bug reporting URL, etc...
	 * </p>
	 */
	//public URL[] getMBeanDocs();
	
	/**
	 * Any statistics this instance would like to be publicly available via
	 * the Administration interface.
	 *
	 * <p>
	 * Any Object type may be stored in the list, but only the
	 * <code>toString()</code> representation will be used.
	 * </p>
	 */
	public NamedList<?> getMBeanStatistics();
	
}
