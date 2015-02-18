package org.javenstudio.common.indexdb.document;

import java.io.Reader;

import org.javenstudio.common.indexdb.ITokenStream;

/** 
 * A field that is indexed and tokenized, without term
 *  vectors.  For example this would be used on a 'body'
 *  field, that contains the bulk of a document's text. 
 */
public final class TextField extends AbstractField {

	/** Indexed, tokenized, not stored. */
	public static final FieldType TYPE_NOT_STORED = new FieldType();

	/** Indexed, tokenized, stored. */
	public static final FieldType TYPE_STORED = new FieldType();

	static {
		TYPE_NOT_STORED.setIndexed(true);
		TYPE_NOT_STORED.setTokenized(true);
		TYPE_NOT_STORED.freeze();

		TYPE_STORED.setIndexed(true);
		TYPE_STORED.setTokenized(true);
		TYPE_STORED.setStored(true);
		TYPE_STORED.freeze();
	}

	// TODO: add sugar for term vectors...?

	/** Creates a new TextField with Reader value. */
	public TextField(String name, Reader reader, Field.Store store) {
		super(name, reader, store == Field.Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
	}

	public TextField(String name, Reader reader, FieldType type) {
		super(name, reader, type);
	}
	
	/** Creates a new TextField with String value. */
	public TextField(String name, String value, Field.Store store) {
		super(name, value, store == Field.Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
	}
  
	public TextField(String name, String value, FieldType type) {
		super(name, value, type);
	}
	
	/** Creates a new un-stored TextField with TokenStream value. */
	public TextField(String name, ITokenStream stream) {
		super(name, stream, TYPE_NOT_STORED);
	}
	
}
