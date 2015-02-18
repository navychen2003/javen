package org.javenstudio.falcon.search.update;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represent the field and boost information needed to construct and index
 * a Document.  Like the ResultItem, the field values should
 * match those specified in schema.xml 
 *
 * @since 1.3
 */
public class InputDocument implements Iterable<InputField>, 
		Map<String,InputField>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final Map<String,InputField> mFields;
	private float mDocumentBoost = 1.0f;

	public InputDocument() {
		mFields = new LinkedHashMap<String,InputField>();
	}
  
	public InputDocument(Map<String,InputField> fields) {
		mFields = fields;
	}
  
	/**
	 * Remove all fields and boosts from the document
	 */
	public void clear() {
		if (mFields != null) 
			mFields.clear();
	}

	///////////////////////////////////////////////////////////////////
	// Add / Set fields
	///////////////////////////////////////////////////////////////////

	/** 
	 * Add a field with implied null value for boost.
	 * 
	 * @see #addField(String, Object, float)
	 * @param name name of the field to add
	 * @param value value of the field
	 */
	public void addField(String name, Object value) {
		addField(name, value, 1.0f);
	}
  
	/** 
	 * Get the first value for a field.
	 * 
	 * @param name name of the field to fetch
	 * @return first value of the field or null if not present
	 */
	public Object getFieldValue(String name) {
		InputField field = getField(name);
		
		Object o = null;
		if (field != null) 
			o = field.getFirstValue();
		
		return o;
	}
  
	/** 
	 * Get all the values for a field.
	 * 
	 * @param name name of the field to fetch
	 * @return value of the field or null if not set
	 */
	public Collection<Object> getFieldValues(String name) {
		InputField field = getField(name);
		if (field != null) 
			return field.getValues();
    
		return null;
	} 
  
	/** 
	 * Get all field names.
	 * 
	 * @return Set of all field names.
	 */
	public Collection<String> getFieldNames() {
		return mFields.keySet();
	}
  
	/** 
	 * Set a field with implied null value for boost.
	 * 
	 * @see #setField(String, Object, float)
	 * @param name name of the field to set
	 * @param value value of the field
	 */
	public void setField(String name, Object value) {
		setField(name, value, 1.0f);
	}
  
	public void setField(String name, Object value, float boost) {
		InputField field = new InputField(name);
		mFields.put(name, field);
		field.setValue(value, boost);
	}

	/**
	 * Adds a field with the given name, value and boost.  If a field with the
	 * name already exists, then the given value is appended to the value of that
	 * field, with the new boost. If the value is a collection, then each of its
	 * values will be added to the field.
	 *
	 * @param name Name of the field to add
	 * @param value Value of the field
	 * @param boost Boost value for the field
	 */
	public void addField(String name, Object value, float boost) {
		InputField field = mFields.get(name);
		if (field == null || field.getValue() == null) 
			setField(name, value, boost);
		else 
			field.addValue(value, boost);
	}

	/**
	 * Remove a field from the document
	 * 
	 * @param name The field name whose field is to be removed from the document
	 * @return the previous field with <tt>name</tt>, or
	 *         <tt>null</tt> if there was no field for <tt>key</tt>.
	 */
	public InputField removeField(String name) {
		return mFields.remove(name);
	}

	///////////////////////////////////////////////////////////////////
	// Get the field values
	///////////////////////////////////////////////////////////////////

	public InputField getField(String field) {
		return mFields.get(field);
	}

	public Iterator<InputField> iterator() {
		return mFields.values().iterator();
	}
  
	public float getDocumentBoost() {
		return mDocumentBoost;
	}

	public void setDocumentBoost(float documentBoost) {
		mDocumentBoost = documentBoost;
	}
  
	@Override
	public String toString() {
		return "InputDocument{" + mFields.values() + "}";
	}
  
	public InputDocument deepCopy() {
		InputDocument clone = new InputDocument();
		Set<Entry<String,InputField>> entries = mFields.entrySet();
		for (Map.Entry<String,InputField> fieldEntry : entries) {
			clone.mFields.put(fieldEntry.getKey(), fieldEntry.getValue().deepCopy());
		}
		clone.mDocumentBoost = mDocumentBoost;
		return clone;
	}

	//---------------------------------------------------
	// MAP interface
	//---------------------------------------------------

	@Override
	public boolean containsKey(Object key) {
		return mFields.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return mFields.containsValue(value);
	}

	@Override
	public Set<Entry<String, InputField>> entrySet() {
		return mFields.entrySet();
	}

	@Override
	public InputField get(Object key) {
		return mFields.get(key);
	}

	@Override
	public boolean isEmpty() {
		return mFields.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return mFields.keySet();
	}

	@Override
	public InputField put(String key, InputField value) {
		return mFields.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends InputField> t) {
		mFields.putAll( t );
	}

	@Override
	public InputField remove(Object key) {
		return mFields.remove(key);
	}

	@Override
	public int size() {
		return mFields.size();
	}

	@Override
	public Collection<InputField> values() {
		return mFields.values();
	}
	
}
