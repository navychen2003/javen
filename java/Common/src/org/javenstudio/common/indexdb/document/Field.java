package org.javenstudio.common.indexdb.document;

import java.io.Reader;

/**
 * A field is a section of a Document.  Each field has two parts, a name and a
 * value.  Values may be free text, provided as a String or as a Reader, or they
 * may be atomic keywords, which are not further processed.  Such keywords may
 * be used to represent dates, urls, etc.  Fields are optionally stored in the
 * index, so that they may be returned with hits on the document.
 */
public final class Field {
  
	/** Specifies whether a field's value should be stored. */
	public static enum Store {
		/** 
		 * Store the original field value in the index. This is useful for short texts
		 * like a document's title which should be displayed with the results. The
		 * value is stored in its original form, i.e. no analyzer is used before it is
		 * stored.
		 */
		YES,

		/** Do not store the field's value in the index. */
		NO
	}
	
	/**
	 * Create a string field by specifying its name, value and how it will
	 * be saved in the index. This field is indexed but not tokenized: the entire
	 * String value is indexed as a single token.
	 * 
	 * @param name The name of the field
	 * @param value The string to process
	 * @param store Whether <code>value</code> should be stored in the index
	 * @throws NullPointerException if name or value is <code>null</code>
	 * @throws IllegalArgumentException in any of the following situations:
	 * <ul> 
	 *  <li>the field is neither stored nor indexed</li> 
	 *  <li>the field is not indexed but termVector is <code>TermVector.YES</code></li>
	 * </ul> 
	 */
	public static Fieldable createString(String name, String value, Store store) {
		return new StringField(name, value, store); 
	}
	  
	/**
	 * Create a field by specifying its name, value and how it will
	 * be saved in the index.
	 * 
	 * @param name The name of the field
	 * @param value The string to process
	 * @param store Whether <code>value</code> should be stored in the index
	 * @throws NullPointerException if name or value is <code>null</code>
	 * @throws IllegalArgumentException in any of the following situations:
	 * <ul> 
	 *  <li>the field is neither stored nor indexed</li> 
	 *  <li>the field is not indexed but termVector is <code>TermVector.YES</code></li>
	 * </ul> 
	 */
	public static Fieldable createText(String name, String value, Store store) {
		return new TextField(name, value, store); 
	}
	
	/**
	 * Create a field by specifying its name, value and how it will
	 * be saved in the index.
	 * 
	 * @param name The name of the field
	 * @param value The string to process
	 * @param type The field type to specify custom index options
	 * @throws NullPointerException if name or value is <code>null</code>
	 * @throws IllegalArgumentException in any of the following situations:
	 * <ul> 
	 *  <li>the field is neither stored nor indexed</li> 
	 *  <li>the field is not indexed but termVector is <code>TermVector.YES</code></li>
	 * </ul> 
	 */
	public static Fieldable createText(String name, String value, FieldType type) {
		return new TextField(name, value, type); 
	}
	
	/**
	 * Create a tokenized and indexed field that is not stored, optionally with 
	 * storing term vectors.  The Reader is read only when the Document is added to the index,
	 * i.e. you may not close the Reader until {@link IndexWriter#addDocument(Document)}
	 * has been called.
	 * 
	 * @param name The name of the field
	 * @param reader The reader with the content
	 * @param store Whether <code>value</code> should be stored in the index
	 * @throws NullPointerException if name or reader is <code>null</code>
	 */
	public static Fieldable createText(String name, Reader reader, Store store) {
		return new TextField(name, reader, store);
	}
	
	/**
	 * Create a tokenized and indexed field that is not stored, optionally with 
	 * storing term vectors.  The Reader is read only when the Document is added to the index,
	 * i.e. you may not close the Reader until {@link IndexWriter#addDocument(Document)}
	 * has been called.
	 * 
	 * @param name The name of the field
	 * @param reader The reader with the content
	 * @param type The field type to specify custom index options
	 * @throws NullPointerException if name or reader is <code>null</code>
	 */
	public static Fieldable createText(String name, Reader reader, FieldType type) {
		return new TextField(name, reader, type);
	}
	
	/**
	 * Create a stored field with binary value. Optionally the value may be compressed.
	 * 
	 * @param name The name of the field
	 * @param value The binary value
	 */
	public static Fieldable create(String name, byte[] value) {
		return create(name, value, 0, value.length);
	}
	
	/**
	 * Create a stored field with binary value. Optionally the value may be compressed.
	 * 
	 * @param name The name of the field
	 * @param value The binary value
	 * @param offset Starting offset in value where this Field's bytes are
	 * @param length Number of bytes to use for this Field, starting at offset
	 */
	public static Fieldable create(String name, byte[] value, int offset, int length) {
		return new StoredField(name, value, offset, length);
	}
	
	/**
	 * Create a int field with long value. Optionally the value may be compressed.
	 * 
	 * @param name The name of the field
	 * @param value The int value
	 * @param store Whether <code>value</code> should be stored in the index
	 */
	public static Fieldable create(String name, int value, Store store) {
		return new IntField(name, value, store);
	}
	
	/**
	 * Create a long field with long value. Optionally the value may be compressed.
	 * 
	 * @param name The name of the field
	 * @param value The long value
	 * @param store Whether <code>value</code> should be stored in the index
	 */
	public static Fieldable create(String name, long value, Store store) {
		return new LongField(name, value, store);
	}
	
	/**
	 * Create a float field with long value. Optionally the value may be compressed.
	 * 
	 * @param name The name of the field
	 * @param value The float value
	 * @param store Whether <code>value</code> should be stored in the index
	 */
	public static Fieldable create(String name, float value, Store store) {
		return new FloatField(name, value, store);
	}
	
	/**
	 * Create a double field with long value. Optionally the value may be compressed.
	 * 
	 * @param name The name of the field
	 * @param value The double value
	 * @param store Whether <code>value</code> should be stored in the index
	 */
	public static Fieldable create(String name, double value, Store store) {
		return new DoubleField(name, value, store);
	}
	
}
