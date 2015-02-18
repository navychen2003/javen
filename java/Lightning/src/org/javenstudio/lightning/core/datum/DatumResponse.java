package org.javenstudio.lightning.core.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.QueryResponse;
import org.javenstudio.lightning.response.ResponseOutput;

public class DatumResponse extends QueryResponse {

	public DatumResponse(DatumCore core, Request req, 
			ResponseOutput output) throws ErrorException { 
		super(output);
	}
	
}
