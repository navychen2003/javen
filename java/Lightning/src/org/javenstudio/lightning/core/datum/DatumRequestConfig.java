package org.javenstudio.lightning.core.datum;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.falcon.util.ModifiableParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.DefaultRequestConfig;
import org.javenstudio.lightning.request.RequestAcceptor;
import org.javenstudio.lightning.request.RequestBase;
import org.javenstudio.lightning.request.RequestInput;

public class DatumRequestConfig extends DefaultRequestConfig {

	public DatumRequestConfig(ContextResource config) throws ErrorException { 
		super(config);
	}
	
	@Override
	protected RequestBase createRequest(RequestAcceptor acceptor, RequestInput input, 
			Params params) throws ErrorException { 
		if (params != null && params instanceof ModifiableParams) {
			ModifiableParams mparams = (ModifiableParams)params;
			String queryPath = input.getQueryPath();
			if (queryPath != null) { 
				if (queryPath.startsWith(DatumFileHandler.PATH_REWRITE))
					DatumFileHandler.rewriteRequest(input, mparams);
				else if (queryPath.startsWith(DatumImageHandler.PATH_REWRITE))
					DatumImageHandler.rewriteRequest(input, mparams);
				else if (queryPath.startsWith(DatumDownloadHandler.PATH_REWRITE))
					DatumDownloadHandler.rewriteRequest(input, mparams);
			}
		}
		
		return super.createRequest(acceptor, input, params);
	}
	
}
