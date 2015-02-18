package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IFieldVisitor {

	/**
	 * Enumeration of possible return values for {@link #needsField}.
	 */
	public static enum Status {
		/** YES: the field should be visited. */
		YES,
	    /** NO: don't visit this field, but continue processing fields for this document. */
	    NO,
	    /** STOP: don't visit this field and stop processing any other fields for this document. */
	    STOP
	}
	
	/** Process a binary field. */
	public void addBinaryField(IFieldInfo fieldInfo, byte[] value, int offset, int length) 
			throws IOException;

	/** Process a string field */
	public void addStringField(IFieldInfo fieldInfo, String value) throws IOException;

	/** Process a int numeric field. */
	public void addIntField(IFieldInfo fieldInfo, int value) throws IOException;

	/** Process a long numeric field. */
	public void addLongField(IFieldInfo fieldInfo, long value) throws IOException;

	/** Process a float numeric field. */
	public void addFloatField(IFieldInfo fieldInfo, float value) throws IOException;

	/** Process a double numeric field. */
	public void addDoubleField(IFieldInfo fieldInfo, double value) throws IOException;
  
	/**
	 * Hook before processing a field.
	 * Before a field is processed, this method is invoked so that
	 * subclasses can return a {@link Status} representing whether
	 * they need that particular field or not, or to stop processing
	 * entirely.
	 */
	public Status needsField(IFieldInfo fieldInfo) throws IOException;
	
}
