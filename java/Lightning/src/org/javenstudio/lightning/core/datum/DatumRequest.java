package org.javenstudio.lightning.core.datum;

import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.QueryRequest;
import org.javenstudio.lightning.request.RequestInput;

public class DatumRequest extends QueryRequest {

	public DatumRequest(DatumCore core, RequestInput input, Params params) {
		super(core, input, params);
	}
	
}
