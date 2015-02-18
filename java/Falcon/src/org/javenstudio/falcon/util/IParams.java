package org.javenstudio.falcon.util;

import org.javenstudio.falcon.ErrorException;

public interface IParams {
	
	public static final String TOKEN = "token";

	public String getParam(String name) throws ErrorException;
	public String getParam(String name, String def) throws ErrorException;
	public String[] getParams(String name) throws ErrorException;
	
}
