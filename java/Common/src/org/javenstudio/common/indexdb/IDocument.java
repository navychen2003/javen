package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.BytesRef;

public interface IDocument extends Iterable<IField> {

	/** 
	 * Returns a field with the given name if any exist in this document, or
	 * null.  If multiple fields exists with this name, this method returns the
	 * first value added.
	 */
	public IField getField(String name);
	
	/**
	 * Returns an array of {@link IField}s with the given name.
	 * This method returns an empty array when there are no
	 * matching fields.  It never returns null.
	 *
	 * @param name the name of the field
	 * @return a <code>Fieldable[]</code> array
	 */
	public IField[] getFields(String name);
	
	/** 
	 * Returns a List of all the fields in a document.
	 * <p>Note that fields which are <i>not</i> stored are
	 * <i>not</i> available in documents retrieved from the
	 * index, e.g. {@link IndexSearcher#doc(int)} or {@link
	 * IndexReader#document(int)}.
	 */
	public IField[] getFields();
	
	/**
	 * Returns an array of values of the field specified as the method parameter.
	 * This method returns an empty array when there are no
	 * matching fields.  It never returns null.
	 * For {@link IntField}, {@link LongField}, {@link
	 * FloatField} and {@link DoubleField} it returns the string value of the number. If you want
	 * the actual numeric field instances back, use {@link #getFields}.
	 * @param name the name of the field
	 * @return a <code>String[]</code> of field values
	 */
	public String[] getStringValues(String name);
	
	/** 
	 * Returns the string value of the field with the given name if any exist in
	 * this document, or null.  If multiple fields exist with this name, this
	 * method returns the first value added. If only binary fields with this name
	 * exist, returns null.
	 * For {@link IntField}, {@link LongField}, {@link
	 * FloatField} and {@link DoubleField} it returns the string value of the number. If you want
	 * the actual numeric field instance back, use {@link #getField}.
	 */
	public String getStringValue(String name);
	
	/**
	 * Returns an array of byte arrays for of the fields that have the name specified
	 * as the method parameter.  This method returns an empty
	 * array when there are no matching fields.  It never
	 * returns null.
	 *
	 * @param name the name of the field
	 * @return a <code>byte[][]</code> of binary field values
	 */
	public BytesRef[] getBinaryValues(String name);
	
	/**
	 * Returns an array of bytes for the first (or only) field that has the name
	 * specified as the method parameter. This method will return <code>null</code>
	 * if no binary fields with the specified name are available.
	 * There may be non-binary fields with the same name.
	 *
	 * @param name the name of the field.
	 * @return a <code>byte[]</code> containing the binary field value or <code>null</code>
	 */
	public BytesRef getBinaryValue(String name);
	
}
