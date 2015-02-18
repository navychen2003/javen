package org.javenstudio.common.indexdb.index.field;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.document.Document;
import org.javenstudio.common.indexdb.document.FieldType;
import org.javenstudio.common.indexdb.document.StoredField;
import org.javenstudio.common.indexdb.document.TextField;

/** 
 * A {@link FieldVisitor} that creates a {@link
 *  Document} containing all stored fields, or only specific
 *  requested fields provided to {@link #DocumentStoredFieldVisitor(Set)}
 *  This is used by {@link IndexReader#document(int)} to load a
 *  document.
 */
public class StoredFieldVisitor extends FieldVisitor {
	
	private final Document mDoc = new Document();
	private final Set<String> mFieldsToAdd;

	/** Load only fields named in the provided <code>Set&lt;String&gt;</code>. */
	public StoredFieldVisitor(Set<String> fieldsToAdd) {
		mFieldsToAdd = fieldsToAdd;
	}

	/** Load only fields named in the provided <code>Set&lt;String&gt;</code>. */
	public StoredFieldVisitor(String... fields) {
		mFieldsToAdd = new HashSet<String>(fields.length);
		for (String field : fields) {
			mFieldsToAdd.add(field);
		}
	}

	/** Load all stored fields. */
	public StoredFieldVisitor() {
		mFieldsToAdd = null;
	}

	@Override
	public void addBinaryField(IFieldInfo fieldInfo, byte[] value, int offset, int length) 
			throws IOException {
		mDoc.addField(new StoredField(fieldInfo.getName(), value));
	}

	@Override
	public void addStringField(IFieldInfo fieldInfo, String value) throws IOException {
		final FieldType ft = new FieldType(TextField.TYPE_STORED);
		ft.setStoreTermVectors(fieldInfo.hasVectors());
		ft.setIndexed(fieldInfo.isIndexed());
		ft.setOmitNorms(fieldInfo.isOmitsNorms());
		ft.setIndexOptions(fieldInfo.getIndexOptions());
		mDoc.addField(new TextField(fieldInfo.getName(), value, ft));
	}

	@Override
	public void addIntField(IFieldInfo fieldInfo, int value) {
		mDoc.addField(new StoredField(fieldInfo.getName(), value));
	}

	@Override
	public void addLongField(IFieldInfo fieldInfo, long value) {
		mDoc.addField(new StoredField(fieldInfo.getName(), value));
	}

	@Override
	public void addFloatField(IFieldInfo fieldInfo, float value) {
		mDoc.addField(new StoredField(fieldInfo.getName(), value));
	}

	@Override
	public void addDoubleField(IFieldInfo fieldInfo, double value) {
		mDoc.addField(new StoredField(fieldInfo.getName(), value));
	}

	@Override
	public Status needsField(IFieldInfo fieldInfo) throws IOException {
		return mFieldsToAdd == null || mFieldsToAdd.contains(fieldInfo.getName()) ? 
				Status.YES : Status.NO;
	}

	public IDocument getDocument() {
		return mDoc;
	}
  
}
