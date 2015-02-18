package org.javenstudio.common.indexdb.document;

import org.javenstudio.common.indexdb.IndexOptions;

/** 
 * A field that is indexed but not tokenized: the entire
 *  String value is indexed as a single token.  For example
 *  this might be used for a 'country' field or an 'id'
 *  field, or any field that you intend to use for sorting
 *  or access through the field cache. 
 */
public final class StringField extends AbstractField {

	/** 
	 * Indexed, not tokenized, omits norms, indexes
	 *  DOCS_ONLY, not stored. 
	 */
	public static final FieldType TYPE_NOT_STORED = new FieldType();

	/** 
	 * Indexed, not tokenized, omits norms, indexes
	 *  DOCS_ONLY, stored 
	 */
	public static final FieldType TYPE_STORED = new FieldType();

	static {
		TYPE_NOT_STORED.setIndexed(true);
		TYPE_NOT_STORED.setOmitNorms(true);
		TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS_ONLY);
		TYPE_NOT_STORED.freeze();

		TYPE_STORED.setIndexed(true);
		TYPE_STORED.setOmitNorms(true);
		TYPE_STORED.setIndexOptions(IndexOptions.DOCS_ONLY);
		TYPE_STORED.setStored(true);
		TYPE_STORED.freeze();
	}

	/** Creates a new StringField. */
	public StringField(String name, String value, Field.Store stored) {
		super(name, value, stored == Field.Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
	}

	@Override
	public String getStringValue() {
		return (mFieldsData == null) ? null : mFieldsData.toString();
	}
	
}
