package org.javenstudio.falcon.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.javenstudio.common.util.Logger;

public final class XMLLogger implements ErrorHandler, ErrorListener, XMLReporter {

	private final Logger LOG;

	public XMLLogger(Logger log) {
		LOG = log;
	}

	// ErrorHandler

	public void warning(SAXParseException e) {
    	LOG.warn("XML parse warning in \"" + e.getSystemId() + "\", line " 
    			+ e.getLineNumber() + ", column " + e.getColumnNumber() + ": " 
    			+ e.getMessage());
	}

	public void error(SAXParseException e) throws SAXException {
		throw e;
	}

	public void fatalError(SAXParseException e) throws SAXException {
		throw e;
	}

	// ErrorListener

	public void warning(TransformerException e) {
		LOG.warn(e.getMessageAndLocation());
	}

	public void error(TransformerException e) throws TransformerException {
		throw e;
	}

	public void fatalError(TransformerException e) throws TransformerException {
		throw e;
	}

	// XMLReporter

	public void report(String message, String errorType, Object relatedInformation, Location loc) {
		final StringBuilder sb = new StringBuilder("XML parser reported ").append(errorType);
		if (loc !=  null) {
			sb.append(" in \"").append(loc.getSystemId()).append("\", line ")
				.append(loc.getLineNumber()).append(", column ").append(loc.getColumnNumber());
		}
		LOG.warn(sb.append(": ").append(message).toString());
	}

}
