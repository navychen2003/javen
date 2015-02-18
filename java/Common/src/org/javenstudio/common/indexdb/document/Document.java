package org.javenstudio.common.indexdb.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * Documents are the unit of indexing and search.
 *
 * A Document is a set of fields.  Each field has a name and a textual value.
 * A field may be {@link IFieldType#stored() stored} with the document, in which
 * case it is returned with search hits on the document.  Thus each document
 * should typically contain one or more stored fields which uniquely identify
 * it.
 *
 * <p>Note that fields which are <i>not</i> {@link IFieldType#stored() stored} are
 * <i>not</i> available in documents retrieved from the index, e.g. with {@link
 * ScoreDoc#doc} or {@link IndexReader#document(int)}.
 */
public final class Document implements IDocument {
	private final static String[] NO_STRINGS = new String[0];

	private final List<IField> mFields = new ArrayList<IField>();

	/** Constructs a new document with no fields. */
	public Document() {}

	@Override
	public final Iterator<IField> iterator() {
		return mFields.iterator();
	}

	/**
	 * <p>Adds a field to a document.  Several fields may be added with
	 * the same name.  In this case, if the fields are indexed, their text is
	 * treated as though appended for the purposes of search.</p>
	 * <p> Note that add like the removeField(s) methods only makes sense 
	 * prior to adding a document to an index. These methods cannot
	 * be used to change the content of an existing index! In order to achieve this,
	 * a document has to be deleted from an index and a new changed version of that
	 * document has to be added.</p>
	 */
	public final void addField(IField field) {
		mFields.add(field);
	}
  
	/**
	 * <p>Removes field with the specified name from the document.
	 * If multiple fields exist with this name, this method removes the first field that has been added.
	 * If there is no field with the specified name, the document remains unchanged.</p>
	 * <p> Note that the removeField(s) methods like the add method only make sense 
	 * prior to adding a document to an index. These methods cannot
	 * be used to change the content of an existing index! In order to achieve this,
	 * a document has to be deleted from an index and a new changed version of that
	 * document has to be added.</p>
	 */
	public final void removeField(String name) {
		Iterator<IField> it = mFields.iterator();
		while (it.hasNext()) {
			IField field = it.next();
			if (field.getName().equals(name)) {
				it.remove();
				return;
			}
		}
	}
  
	/**
	 * <p>Removes all fields with the given name from the document.
	 * If there is no field with the specified name, the document remains unchanged.</p>
	 * <p> Note that the removeField(s) methods like the add method only make sense 
	 * prior to adding a document to an index. These methods cannot
	 * be used to change the content of an existing index! In order to achieve this,
	 * a document has to be deleted from an index and a new changed version of that
	 * document has to be added.</p>
	 */
	public final void removeFields(String name) {
		Iterator<IField> it = mFields.iterator();
		while (it.hasNext()) {
			IField field = it.next();
			if (field.getName().equals(name)) 
				it.remove();
		}
	}

	/** 
	 * Returns a field with the given name if any exist in this document, or
	 * null.  If multiple fields exists with this name, this method returns the
	 * first value added.
	 */
	@Override
	public final IField getField(String name) {
		for (IField field : mFields) {
			if (field.getName().equals(name)) 
				return field;
		}
		return null;
	}

	/**
	 * Returns an array of {@link IField}s with the given name.
	 * This method returns an empty array when there are no
	 * matching fields.  It never returns null.
	 *
	 * @param name the name of the field
	 * @return a <code>Fieldable[]</code> array
	 */
	@Override
	public final IField[] getFields(String name) {
		List<IField> result = new ArrayList<IField>();
		for (IField field : mFields) {
			if (field.getName().equals(name)) 
				result.add(field);
		}

		return result.toArray(new IField[result.size()]);
	}
  
	/** 
	 * Returns a List of all the fields in a document.
	 * <p>Note that fields which are <i>not</i> stored are
	 * <i>not</i> available in documents retrieved from the
	 * index, e.g. {@link IndexSearcher#doc(int)} or {@link
	 * IndexReader#document(int)}.
	 */
	@Override
	public final IField[] getFields() {
		return mFields.toArray(new IField[mFields.size()]);
	}
  
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
	@Override
	public final String[] getStringValues(String name) {
		List<String> result = new ArrayList<String>();
		for (IField field : mFields) {
			if (field.getName().equals(name) && field.getStringValue() != null) 
				result.add(field.getStringValue());
		}
    
		if (result.size() == 0) 
			return NO_STRINGS;
    
		return result.toArray(new String[result.size()]);
	}

	/** 
	 * Returns the string value of the field with the given name if any exist in
	 * this document, or null.  If multiple fields exist with this name, this
	 * method returns the first value added. If only binary fields with this name
	 * exist, returns null.
	 * For {@link IntField}, {@link LongField}, {@link
	 * FloatField} and {@link DoubleField} it returns the string value of the number. If you want
	 * the actual numeric field instance back, use {@link #getField}.
	 */
	@Override
	public final String getStringValue(String name) {
		for (IField field : mFields) {
			if (field.getName().equals(name) && field.getStringValue() != null) 
				return field.getStringValue();
		}
		return null;
	}
  
	/**
	 * Returns an array of byte arrays for of the fields that have the name specified
	 * as the method parameter.  This method returns an empty
	 * array when there are no matching fields.  It never
	 * returns null.
	 *
	 * @param name the name of the field
	 * @return a <code>byte[][]</code> of binary field values
	 */
	@Override
	public final BytesRef[] getBinaryValues(String name) {
		final List<BytesRef> result = new ArrayList<BytesRef>();
		for (IField field : mFields) {
			if (field.getName().equals(name)) {
				final BytesRef bytes = field.getBinaryValue();
				if (bytes != null) 
					result.add(bytes);
			}
		}
  
		return result.toArray(new BytesRef[result.size()]);
	}
  
	/**
	 * Returns an array of bytes for the first (or only) field that has the name
	 * specified as the method parameter. This method will return <code>null</code>
	 * if no binary fields with the specified name are available.
	 * There may be non-binary fields with the same name.
	 *
	 * @param name the name of the field.
	 * @return a <code>byte[]</code> containing the binary field value or <code>null</code>
	 */
	@Override
	public final BytesRef getBinaryValue(String name) {
		for (IField field : mFields) {
			if (field.getName().equals(name)) {
				final BytesRef bytes = field.getBinaryValue();
				if (bytes != null) 
					return bytes;
			}
		}
		return null;
	}
	
	/** Prints the fields of a document for human consumption. */
	@Override
	public final String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Document{");
		for (int i = 0; i < mFields.size(); i++) {
			IField field = mFields.get(i);
			buffer.append(field.toString());
			if (i != mFields.size()-1)
				buffer.append(",");
		}
		buffer.append("}");
		return buffer.toString();
	}
	
}
