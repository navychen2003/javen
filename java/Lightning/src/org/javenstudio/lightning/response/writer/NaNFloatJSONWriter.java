package org.javenstudio.lightning.response.writer;

import java.io.IOException;
import java.io.Writer;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public abstract class NaNFloatJSONWriter extends JSONWriter {

	protected abstract String getNaN();
	protected abstract String getInf();

	public NaNFloatJSONWriter(Writer writer, 
			Request req, Response rsp) throws ErrorException {
		super(writer, req, rsp);
	}

	@Override
	public void writeFloat(String name, float val) throws IOException {
		if (Float.isNaN(val)) {
			getWriter().write(getNaN());
			
		} else if (Float.isInfinite(val)) {
			if (val < 0.0f)
				getWriter().write('-');
			getWriter().write(getInf());
			
		} else {
			writeFloat(name, Float.toString(val));
		}
	}

	@Override
	public void writeDouble(String name, double val) throws IOException {
		if (Double.isNaN(val)) {
			getWriter().write(getNaN());
			
		} else if (Double.isInfinite(val)) {
			if (val < 0.0)
				getWriter().write('-');
			getWriter().write(getInf());
			
		} else {
			writeDouble(name, Double.toString(val));
		}
	}
	
}
