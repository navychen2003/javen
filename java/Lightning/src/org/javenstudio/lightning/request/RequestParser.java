package org.javenstudio.lightning.request;

import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.Params;

//I guess we don't really even need the interface, but i'll keep it here just for kicks
public interface RequestParser {

	public Params parseParamsAndFillStreams(RequestInput input, 
			List<ContentStream> streams) throws ErrorException;
	
}
