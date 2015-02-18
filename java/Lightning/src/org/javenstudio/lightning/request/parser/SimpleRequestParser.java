package org.javenstudio.lightning.request.parser;

import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.SimpleParams;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.request.RequestParser;

/**
 * The simple parser just uses the params directly
 */
public class SimpleRequestParser implements RequestParser {
	private static final Logger LOG = Logger.getLogger(SimpleRequestParser.class);

	@Override
	public Params parseParamsAndFillStreams(RequestInput input,
			List<ContentStream> streams) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("parseParamsAndFillStreams: input=" + input);
		
		return new SimpleParams(input.getParameterMap());
	}

}
