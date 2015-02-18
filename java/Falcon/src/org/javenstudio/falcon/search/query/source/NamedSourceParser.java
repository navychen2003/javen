package org.javenstudio.falcon.search.query.source;

import org.javenstudio.falcon.search.query.ValueSourceParser;

public abstract class NamedSourceParser extends ValueSourceParser {
	
	private final String mName;
	
	public NamedSourceParser(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
	
}
