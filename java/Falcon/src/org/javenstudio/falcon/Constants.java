package org.javenstudio.falcon;

import java.nio.charset.Charset;

public class Constants {

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	
	public static final String PROJECT = "falcon";
	public static final String PROJECT_BASE = "org.javenstudio." + PROJECT;
	public static final String[] PROJECT_PACKAGES = {"", "params.", "util.", 
		"search.", "search.analysis.", "search.cache.", "search.comparator.", "search.facet.", 
		"search.filter.", "search.grouping.", "search.hits.", "search.params.", "search.query.", 
		"search.query.source.", "search.schema.", "search.schema.type.", "search.shard.", 
		"search.similarity.", "search.stats.", "search.store.", "search.transformer.", 
		"search.handler.", "search.component.", "search.update."};
	
	public static final String SCHEMA_XML_FILENAME = "schema.xml";
	
	public static final String RESOURCE_LOADER_URI_SCHEME = "falres";
	public static final String RESOURCE_LOADER_AUTHORITY_ABSOLUTE = "@";
	
}
