package org.javenstudio.falcon.search.hits;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.document.Document;
import org.javenstudio.common.indexdb.document.DoubleField;
import org.javenstudio.common.indexdb.document.Field;
import org.javenstudio.common.indexdb.document.FieldType;
import org.javenstudio.common.indexdb.document.FloatField;
import org.javenstudio.common.indexdb.document.IntField;
import org.javenstudio.common.indexdb.document.LongField;
import org.javenstudio.common.indexdb.document.StoredField;
import org.javenstudio.common.indexdb.document.TextField;
import org.javenstudio.common.indexdb.index.field.StoredFieldVisitor;
import org.javenstudio.hornet.document.LazyDocument;

/** 
 * Future optimizations (yonik)
 *
 * If no cache is present:
 *   - use NO_LOAD instead of LAZY_LOAD
 *   - use LOAD_AND_BREAK if a single field is begin retrieved
 *
 * FieldSelector which loads the specified fields, and load all other
 * field lazily.
 */
// TODO: can we just subclass DocumentStoredFieldVisitor?
// need to open up access to its Document...
public class SetNonLazyFieldSelector extends StoredFieldVisitor {
    
    private final Document mDoc = new Document();
    private final LazyDocument mLazyDoc;
    private Set<String> mFieldsToLoad;

    public SetNonLazyFieldSelector(Set<String> toLoad, 
    		IIndexReader reader, int docID) {
    	mFieldsToLoad = toLoad;
    	mLazyDoc = new LazyDocument(reader, docID);
    }

    public final Document getDocument() { 
    	return mDoc;
    }
    
    public Status needsField(IFieldInfo fieldInfo) {
    	if (mFieldsToLoad.contains(fieldInfo.getName())) {
    		return Status.YES;
    	} else {
    		mDoc.addField(mLazyDoc.getField(fieldInfo));
    		return Status.NO;
    	}
    }

    @Override
    public void addBinaryField(IFieldInfo fieldInfo, byte[] value, 
    		int offset, int length) throws IOException {
    	mDoc.addField(new StoredField(fieldInfo.getName(), 
    			value, offset, length));
    }

    @Override
    public void addStringField(IFieldInfo fieldInfo, String value) 
    		throws IOException {
    	final FieldType ft = new FieldType(TextField.TYPE_STORED);
    	
    	ft.setStoreTermVectors(fieldInfo.hasVectors());
    	ft.setIndexed(fieldInfo.isIndexed());
    	ft.setOmitNorms(fieldInfo.isOmitsNorms());
    	ft.setIndexOptions(fieldInfo.getIndexOptions());
    	
    	mDoc.addField(Field.createText(fieldInfo.getName(), value, ft));
    }

    @Override
    public void addIntField(IFieldInfo fieldInfo, int value) {
    	FieldType ft = new FieldType(IntField.TYPE_NOT_STORED);
    	
    	ft.setStored(true);
    	ft.setIndexed(fieldInfo.isIndexed());
    	
    	mDoc.addField(new IntField(fieldInfo.getName(), value, ft));
    }

    @Override
    public void addLongField(IFieldInfo fieldInfo, long value) {
    	FieldType ft = new FieldType(LongField.TYPE_NOT_STORED);
    	
    	ft.setStored(true);
    	ft.setIndexed(fieldInfo.isIndexed());
    	
    	mDoc.addField(new LongField(fieldInfo.getName(), value, ft));
    }

    @Override
    public void addFloatField(IFieldInfo fieldInfo, float value) {
    	FieldType ft = new FieldType(FloatField.TYPE_NOT_STORED);
    	
    	ft.setStored(true);
    	ft.setIndexed(fieldInfo.isIndexed());
    	
    	mDoc.addField(new FloatField(fieldInfo.getName(), value, ft));
    }

    @Override
    public void addDoubleField(IFieldInfo fieldInfo, double value) {
    	FieldType ft = new FieldType(DoubleField.TYPE_NOT_STORED);
    	
    	ft.setStored(true);
    	ft.setIndexed(fieldInfo.isIndexed());
    	
    	mDoc.addField(new DoubleField(fieldInfo.getName(), value, ft));
    }
    
}
