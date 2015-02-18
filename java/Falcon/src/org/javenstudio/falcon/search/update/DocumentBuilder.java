package org.javenstudio.falcon.search.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.document.Document;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.CopyField;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;

// Not thread safe - by design.  Create a new builder for each thread.
public class DocumentBuilder {
	
	private final IndexSchema mSchema;
	private Map<String,String> mMap;
	private Document mDoc;

	public DocumentBuilder(IndexSchema schema) {
		mSchema = schema;
	}

	public void startDoc() {
		mDoc = new Document();
		mMap = new HashMap<String,String>();
	}

	protected void addSingleField(SchemaField sfield, String val, float boost) 
			throws ErrorException {
		// we don't check for a null val ourselves because a SchemaFieldType
		// might actually want to map it to something.  If createField()
		// returns null, then we don't store the field.
		if (sfield.isPolyField()) {
			Fieldable[] fields = sfield.createFields(val, boost);
			if (fields.length > 0) {
				if (!sfield.isMultiValued()) {
					String oldValue = mMap.put(sfield.getName(), val);
					if (oldValue != null) {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"ERROR: multiple values encountered for non multiValued field " + sfield.getName()
								+ ": first='" + oldValue + "' second='" + val + "'");
					}
				}
				
				// Add each field
				for (Fieldable field : fields) {
					mDoc.addField(field);
				}
			}
		} else {
			Fieldable field = sfield.createField(val, boost);
			if (field != null) {
				if (!sfield.isMultiValued()) {
					String oldValue = mMap.put(sfield.getName(), val);
					if (oldValue != null) {
						throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST, 
								"ERROR: multiple values encountered for non multiValued field " + sfield.getName()
								+ ": first='" + oldValue + "' second='" + val + "'");
					}
				}
			}
			
			mDoc.addField(field);
		}
	}

	/**
	 * Add the specified {@link SchemaField} to the document. Does not invoke the copyField mechanism.
	 * @param sfield The {@link SchemaField} to add
	 * @param val The value to add
	 * @param boost The boost factor
	 *
	 * @see #addField(String, String)
	 * @see #addField(String, String, float)
	 * @see #addSingleField(SchemaField, String, float)
	 */
	public void addField(SchemaField sfield, String val, float boost) throws ErrorException {
		addSingleField(sfield,val,boost);
	}

	/**
	 * Add the Field and value to the document, invoking the copyField mechanism
	 * @param name The name of the field
	 * @param val The value to add
	 *
	 * @see #addField(String, String, float)
	 * @see #addField(SchemaField, String, float)
	 * @see #addSingleField(SchemaField, String, float)
	 */
	public void addField(String name, String val) throws ErrorException {
		addField(name, val, 1.0f);
	}

	/**
	 * Add the Field and value to the document with the specified boost, invoking the copyField mechanism
	 * @param name The name of the field.
	 * @param val The value to add
	 * @param boost The boost
	 *
	 * @see #addField(String, String)
	 * @see #addField(SchemaField, String, float)
	 * @see #addSingleField(SchemaField, String, float)
	 */
	public void addField(String name, String val, float boost) throws ErrorException {
		SchemaField sfield = mSchema.getFieldOrNull(name);
		if (sfield != null) 
			addField(sfield,val,boost);

		// Check if we should copy this field to any other fields.
		// This could happen whether it is explicit or not.
		final List<CopyField> copyFields = mSchema.getCopyFieldsList(name);
		if (copyFields != null) {
			for(CopyField cf : copyFields) {
				addSingleField(cf.getDestination(), cf.getLimitedValue(val), boost);
			}
		}

		// error if this field name doesn't match anything
		if (sfield == null && (copyFields == null || copyFields.size() == 0)) {
			throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST, 
					"ERROR:unknown field '" + name + "'");
		}
	}

	public void endDoc() {
		// do nothing
	}

	// specific to this type of document builder
	public Document getDoc() throws ErrorException {
		// Check for all required fields -- Note, all fields with a
		// default value are defacto 'required' fields.  
		List<String> missingFields = null;
		
		for (SchemaField field : mSchema.getRequiredFields()) {
			if (mDoc.getField(field.getName() ) == null) {
				if (field.getDefaultValue() != null) {
					addField(mDoc, field, field.getDefaultValue(), 1.0f);
					
				} else {
					if (missingFields == null) 
						missingFields = new ArrayList<String>(1);
					
					missingFields.add(field.getName());
				}
			}
		}
  
		if (missingFields != null) {
			StringBuilder builder = new StringBuilder();
			// add the uniqueKey if possible
			if (mSchema.getUniqueKeyField() != null) {
				String n = mSchema.getUniqueKeyField().getName();
				String v = mDoc.getField(n).getStringValue();
				
				builder.append("Document [" + n + "=" + v + "] ");
			}
			
			builder.append("missing required fields: ");
			
			for (String field : missingFields) {
				builder.append(field);
				builder.append(" ");
			}
			
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					builder.toString());
		}
    
		Document ret = mDoc; 
		mDoc = null;
		
		return ret;
	}

	private static void addField(Document doc, SchemaField field, Object val, float boost) 
			throws ErrorException {
		if (field.isPolyField()) {
			Fieldable[] farr = field.getType().createFields(field, val, boost);
			for (Fieldable f : farr) {
				if (f != null) 
					doc.addField(f); // null fields are not added
			}
		} else {
			Fieldable f = field.createField(val, boost);
			if (f != null) 
				doc.addField(f);  // null fields are not added
		}
	}
  
	private static String getID(InputDocument doc, IndexSchema schema) {
		String id = "";
		
		SchemaField sf = schema.getUniqueKeyField();
		if (sf != null) 
			id = "[doc=" + doc.getFieldValue(sf.getName()) + "] ";
		
		return id;
	}

	/**
	 * Convert a InputDocument to a Document.
	 * 
	 * This function should go elsewhere.  This builds the Document without an
	 * extra Map<> checking for multiple values.  
	 * 
	 * TODO: /!\ NOTE /!\ This semantics of this function are still in flux.  
	 * Something somewhere needs to be able to fill up a ResultItem from
	 * a document - this is one place that may happen.  It may also be
	 * moved to an independent function
	 * 
	 * @since 1.3
	 */
	public static Document toDocument(InputDocument doc, IndexSchema schema) 
			throws ErrorException { 
		final Document out = new Document();
		final float docBoost = doc.getDocumentBoost();
    
		// Load fields from InputDocument to Document
		for (InputField field : doc) {
			String name = field.getName();
			SchemaField sfield = schema.getFieldOrNull(name);
			boolean used = false;

			float boost = field.getBoost();
			boolean applyBoost = (sfield != null && sfield.isIndexed() && !sfield.isOmitNorms());
      
			// Make sure it has the correct number
			if (sfield != null && !sfield.isMultiValued() && field.getValueCount() > 1) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"ERROR: " + getID(doc, schema) + "multiple values encountered for non multiValued field " + 
						sfield.getName() + ": " + field.getValue() );
			}
      
			if (applyBoost == false && boost != 1.0F) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
						"ERROR: " + getID(doc, schema) + "cannot set an index-time boost, unindexed or norms are omitted for field " + 
						sfield.getName() + ": " + field.getValue() );
			}

			// Indexdb no longer has a native docBoost, so we have to multiply 
			// it ourselves (do this after the applyBoost error check so we don't 
			// give an error on fields that don't support boost just because of a 
			// docBoost)
			boost *= docBoost;

			// load each field value
			boolean hasField = false;
			
			try {
				for (Object v : field) {
					if (v == null) 
						continue;
          
					hasField = true;
					if (sfield != null) {
						used = true;
						addField(out, sfield, v, applyBoost ? boost : 1f);
					}
  
					// Check if we should copy this field to any other fields.
					// This could happen whether it is explicit or not.
					List<CopyField> copyFields = schema.getCopyFieldsList(name);
					
					for (CopyField cf : copyFields) {
						SchemaField destinationField = cf.getDestination();
						
						// check if the copy field is a multivalued or not
						if (!destinationField.isMultiValued() && out.getField(destinationField.getName()) != null) {
							throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
									"ERROR: " + getID(doc, schema) + "multiple values encountered for non multiValued copy field " +
									destinationField.getName() + ": " + v);
						}
  
						used = true;
            
						// Perhaps trim the length of a copy field
						Object val = v;
						if (val instanceof String && cf.getMaxChars() > 0) 
							val = cf.getLimitedValue((String)val);
						
						addField(out, destinationField, val, 
								destinationField.isIndexed() && !destinationField.isOmitNorms() ? boost : 1F);
					}
          
					// The boost for a given field is the product of the 
					// *all* boosts on values of that field. 
					// For multi-valued fields, we only want to set the boost on the
					// first field.
					boost = 1.0f;
				}
			} catch (ErrorException ex) {
				throw ex;
				
			} catch (Exception ex) {
				throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST,
						"ERROR: " + getID(doc, schema)+"Error adding field '" + 
						field.getName() + "'='" + field.getValue() + "' msg=" + ex.getMessage(), ex);
			}
      
			// make sure the field was used somehow...
			if (!used && hasField) {
				throw new ErrorException( ErrorException.ErrorCode.BAD_REQUEST,
						"ERROR: " + getID(doc, schema) + "unknown field '" + name + "'");
			}
		}
    
		// Now validate required fields or add default values
		// fields with default values are defacto 'required'
		for (SchemaField field : schema.getRequiredFields()) {
			if (out.getField(field.getName()) == null) {
				if (field.getDefaultValue() != null) {
					addField(out, field, field.getDefaultValue(), 1.0f);
					
				} else {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							getID(doc, schema) + "missing required field: " + field.getName());
				}
			}
		}
		
		return out;
	}
	
}
