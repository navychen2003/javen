package org.javenstudio.common.indexdb.index.field;

import java.util.HashMap;

import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IndexOptions;

public final class FieldInfosBuilder implements IFieldInfos.Builder {
	
    private final HashMap<String,IFieldInfo> mByName = new HashMap<String,IFieldInfo>();
    private final FieldNumbers mGlobalFieldNumbers;

    public FieldInfosBuilder() {
    	this(new FieldNumbers());
    }
    
    /**
     * Creates a new instance with the given {@link FieldNumbers}. 
     */
    public FieldInfosBuilder(FieldNumbers globalFieldNumbers) {
    	assert globalFieldNumbers != null;
    	mGlobalFieldNumbers = globalFieldNumbers;
    }

    public void add(IFieldInfos other) {
    	for (IFieldInfo fieldInfo : other){ 
    		add(fieldInfo);
    	}
    }
   
    /**
     * adds the given field to this FieldInfos name / number mapping. The given FI
     * must be present in the global field number mapping before this method it
     * called
     */
    private void putInternal(FieldInfo fi) {
    	assert !mByName.containsKey(fi.getName());
    	assert mGlobalFieldNumbers.containsConsistent(Integer.valueOf(fi.getNumber()), fi.getName());
    	mByName.put(fi.getName(), fi);
    }
    
    /** 
     * If the field is not yet known, adds it. If it is known, checks to make
     *  sure that the isIndexed flag is the same as was given previously for this
     *  field. If not - marks it as being indexed.  Same goes for the TermVector
     * parameters.
     *
     * @param name The name of the field
     * @param isIndexed true if the field is indexed
     * @param storeTermVector true if the term vector should be stored
     * @param omitNorms true if the norms for the indexed field should be omitted
     * @param storePayloads true if payloads should be stored for this field
     * @param indexOptions if term freqs should be omitted for this field
     */
    // TODO: fix testCodecs to do this another way, its the only user of this
    public FieldInfo addOrUpdate(String name, boolean isIndexed, boolean storeTermVector,
    		boolean omitNorms, boolean storePayloads, IndexOptions indexOptions) {
    	return addOrUpdateInternal(name, -1, isIndexed, storeTermVector, 
    			omitNorms, storePayloads, indexOptions);
    }

    // NOTE: this method does not carry over termVector
    // booleans nor docValuesType; the indexer chain
    // (TermVectorsConsumerPerField, DocFieldProcessor) must
    // set these fields when they succeed in consuming
    // the document:
    public FieldInfo addOrUpdate(String name, IField.Type fieldType) {
    	// TODO: really, indexer shouldn't even call this
    	// method (it's only called from DocFieldProcessor);
    	// rather, each component in the chain should update
    	// what it "owns".  EG fieldType.indexOptions() should
    	// be updated by maybe FreqProxTermsWriterPerField:
    	return addOrUpdateInternal(
    			name, -1, fieldType.isIndexed(), false,
    			fieldType.isOmitNorms(), false,
    			fieldType.getIndexOptions());
    }

    private FieldInfo addOrUpdateInternal(String name, int preferredFieldNumber, boolean isIndexed,
    		boolean storeTermVector, boolean omitNorms, boolean storePayloads, IndexOptions indexOptions) {
    	FieldInfo fi = (FieldInfo)getFieldInfo(name);
    	if (fi == null) {
    		// get a global number for this field
    		final int fieldNumber = mGlobalFieldNumbers.addOrGet(name, preferredFieldNumber);
    		fi = addInternal(name, fieldNumber, isIndexed, storeTermVector, omitNorms, storePayloads, indexOptions);
    	} else {
    		fi.update(isIndexed, storeTermVector, omitNorms, storePayloads, indexOptions);
    	}
    	return fi;
    }
    
    public FieldInfo add(IFieldInfo fi) {
    	// IMPORTANT - reuse the field number if possible for consistent field numbers across segments
    	return addOrUpdateInternal(
    			fi.getName(), fi.getNumber(), fi.isIndexed(), fi.hasVectors(),
    			fi.isOmitsNorms(), fi.hasPayloads(),
    			fi.getIndexOptions());
    }
    
    private FieldInfo addInternal(String name, int fieldNumber, boolean isIndexed,
    		boolean storeTermVector, boolean omitNorms, boolean storePayloads,
    		IndexOptions indexOptions) {
    	mGlobalFieldNumbers.setIfNotSet(fieldNumber, name);
    	final FieldInfo fi = new FieldInfo(name, isIndexed, fieldNumber, storeTermVector, 
    			omitNorms, storePayloads, indexOptions, null);
    	putInternal(fi);
    	return fi;
    }

    public final FieldNumbers getGlobalFieldNumbers() { 
    	return mGlobalFieldNumbers;
    }
    
    public IFieldInfo getFieldInfo(String fieldName) {
    	return mByName.get(fieldName);
    }
    
    public final FieldInfos finish() {
    	return new FieldInfos(mByName.values().toArray(new FieldInfo[mByName.size()]));
    }
    
}
