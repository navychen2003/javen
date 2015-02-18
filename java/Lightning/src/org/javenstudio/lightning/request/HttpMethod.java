package org.javenstudio.lightning.request;

import java.util.Locale;

public enum HttpMethod {
	GET, POST, HEAD, OTHER;

	public static HttpMethod getMethod(String method) {
		try {
			return HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			return OTHER;
		}
	}
	
}