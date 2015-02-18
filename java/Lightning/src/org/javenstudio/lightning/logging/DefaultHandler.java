package org.javenstudio.lightning.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class DefaultHandler extends StreamHandler {

	public DefaultHandler() {
		this(true);
	}
	
    public DefaultHandler(boolean debug) {
        setLevel(debug ? Level.ALL : Level.INFO);
        setFormatter(new DefaultFormatter());
        setOutputStream(System.err);
    }
    
    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }
    
	@Override
    public void close() {
        flush();
    }
	
}
