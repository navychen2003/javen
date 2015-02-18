package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.codec.IFieldsFormat;

/**
 * Codec API for reading stored fields:
 * 
 * You need to implement {@link #visitDocument(int, FieldVisitor)} to
 * read the stored fields for a document, implement {@link #clone()} (creating
 * clones of any IndexInputs used, etc), and {@link #close()}
 * 
 */
public abstract class FieldsReader implements IFieldsFormat.Reader {

	/** Visit the stored fields for document <code>n</code> */
	public abstract void visitDocument(int n, IFieldVisitor visitor) 
			throws CorruptIndexException, IOException;

	public abstract FieldsReader clone();
	
}
