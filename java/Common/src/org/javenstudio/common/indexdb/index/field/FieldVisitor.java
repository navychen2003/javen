package org.javenstudio.common.indexdb.index.field;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldVisitor;

/**
 * Expert: provides a low-level means of accessing the stored field
 * values in an index.  See {@link IndexReader#document(int,
 * StoredFieldVisitor)}.
 *
 * See {@link StoredFieldVisitor}, which is a
 * <code>StoredFieldVisitor</code> that builds the
 * {@link Document} containing all stored fields.  This is
 * used by {@link IndexReader#document(int)}.
 *
 */
public abstract class FieldVisitor implements IFieldVisitor {
	
	/** Process a binary field. */
	public abstract void addBinaryField(IFieldInfo fieldInfo, 
			byte[] value, int offset, int length) throws IOException;

	/** Process a string field */
	public abstract void addStringField(IFieldInfo fieldInfo, 
			String value) throws IOException;

	/** Process a int numeric field. */
	public abstract void addIntField(IFieldInfo fieldInfo, 
			int value) throws IOException;

	/** Process a long numeric field. */
	public abstract void addLongField(IFieldInfo fieldInfo, 
			long value) throws IOException;

	/** Process a float numeric field. */
	public abstract void addFloatField(IFieldInfo fieldInfo, 
			float value) throws IOException;

	/** Process a double numeric field. */
	public abstract void addDoubleField(IFieldInfo fieldInfo, 
			double value) throws IOException;
  
	/**
	 * Hook before processing a field.
	 * Before a field is processed, this method is invoked so that
	 * subclasses can return a {@link Status} representing whether
	 * they need that particular field or not, or to stop processing
	 * entirely.
	 */
	public abstract Status needsField(IFieldInfo fieldInfo) throws IOException;

}