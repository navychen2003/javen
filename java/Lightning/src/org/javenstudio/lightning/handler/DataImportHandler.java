package org.javenstudio.lightning.handler;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;

public final class DataImportHandler implements RequestHandler {

	private final RequestHandler mHandler;
	
	public DataImportHandler(RequestHandler handler) { 
		if (handler == null) 
			throw new NullPointerException("input handler is null");
		
		mHandler = handler;
	}
	
	@Override
	public void init(NamedList<?> args) throws ErrorException {
		mHandler.init(args);
	}

	@Override
	public void handleRequest(Request req, Response rsp) throws ErrorException {
		mHandler.handleRequest(req, rsp);
	}
	
	@Override
	public boolean handleResponse(Request req, Response rsp, 
			ResponseOutput output) throws ErrorException { 
		return false;
	}
	
	@Override
	public String getMBeanKey() {
		return mHandler.getMBeanKey();
	}

	@Override
	public String getMBeanName() {
		return getClass().getName();
	}

	@Override
	public String getMBeanVersion() {
		return mHandler.getMBeanVersion();
	}

	@Override
	public String getMBeanDescription() {
		return mHandler.getMBeanDescription();
	}

	@Override
	public String getMBeanCategory() {
		return mHandler.getMBeanCategory();
	}

	@Override
	public NamedList<?> getMBeanStatistics() {
		return mHandler.getMBeanStatistics();
	}

}
