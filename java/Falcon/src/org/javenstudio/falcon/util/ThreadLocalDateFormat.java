package org.javenstudio.falcon.util;

import java.text.DateFormat;

public class ThreadLocalDateFormat extends ThreadLocal<DateFormat> {
	
	private final DateFormat mProto;
	
	public ThreadLocalDateFormat(DateFormat d) {
		super();
		mProto = d;
	}
	
	@Override
	protected DateFormat initialValue() {
		return (DateFormat) mProto.clone();
	}
	
}
