package org.javenstudio.lightning.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.javenstudio.common.util.Log;

public class DefaultFormatter extends Formatter {

	@Override
    public synchronized String format(LogRecord record) {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append(Log.formatTime(record.getMillis())).append('[');
		sbuf.append(record.getLoggerName()).append(',');
		sbuf.append(record.getLevel().getName()).append(']');
		sbuf.append(record.getMessage());
		return sbuf.toString();
    }
	
}
