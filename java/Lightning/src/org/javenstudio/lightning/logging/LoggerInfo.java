package org.javenstudio.lightning.logging;

import org.javenstudio.falcon.util.NamedMap;

/**
 * Wrapper class for Logger implementaions
 */
public abstract class LoggerInfo implements Comparable<LoggerInfo> {
	
	public static final String ROOT_NAME = "root";

	protected final String mName;
	protected String mLevel;

	public LoggerInfo(String name) {
		mName = name;
	}

	public String getLevel() { return mLevel; }
	public String getName() { return mName; }
  
	public abstract boolean isSet();

	public NamedMap<?> getInfo() {
		NamedMap<Object> info = new NamedMap<Object>();
		info.add("name", getName());
		info.add("level", getLevel());
		info.add("set", isSet());
		
		return info;
	}

	@Override
	public int compareTo(LoggerInfo other) {
		if (this.equals(other))
			return 0;

		String tN = this.getName();
		String oN = other.getName();

		if (ROOT_NAME.equals(tN))
			return -1;
		if (ROOT_NAME.equals(oN))
			return 1;

		return tN.compareTo(oN);
	}
	
}
