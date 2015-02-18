package org.javenstudio.common.indexdb.search;

/**
 * Expert: A ScoreDoc which also contains information about
 * how to sort the referenced document.  In addition to the
 * document number and score, this object contains an array
 * of values for the document from the field(s) used to sort.
 * For example, if the sort criteria was to sort by fields
 * "a", "b" then "c", the <code>fields</code> object array
 * will have three elements, corresponding respectively to
 * the term values for the document in fields "a", "b" and "c".
 * The class of each element in the array will be either
 * Integer, Float or String depending on the type of values
 * in the terms of each field.
 *
 * @see ScoreDoc
 * @see TopFieldDocs
 */
public class FieldDoc extends ScoreDoc {

	/** 
	 * Expert: The values which are used to sort the referenced document.
	 * The order of these will match the original sort criteria given by a
	 * Sort object.  Each Object will have been returned from
	 * the <code>value</code> method corresponding
	 * FieldComparator used to sort this field.
	 * @see Sort
	 * @see IndexSearcher#search(Query,Filter,int,Sort)
	 */
	private Object[] mFields;

	/** Expert: Creates one of these objects with empty sort information. */
	public FieldDoc(int doc, float score) {
		super (doc, score);
	}

	/** Expert: Creates one of these objects with the given sort information. */
	public FieldDoc(int doc, float score, Object[] fields) {
		super (doc, score);
		mFields = fields;
	}
  
	/** Expert: Creates one of these objects with the given sort information. */
	public FieldDoc(int doc, float score, Object[] fields, int shardIndex) {
		super (doc, score, shardIndex);
		mFields = fields;
	}
  
	public Object[] getFields() { return mFields; }
	public int getFieldSize() { return mFields.length; }
	public Object getFieldAt(int index) { return mFields[index]; }
	
	// A convenience method for debugging.
	@Override
	public String toString() {
		// super.toString returns the doc and score information, so just add the
		// fields information
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("[");
		for (int i = 0; i < mFields.length; i++) {
			sb.append(mFields[i]).append(", ");
		}
		sb.setLength(sb.length() - 2); // discard last ", "
		sb.append("]");
		return sb.toString();
	}
	
}
