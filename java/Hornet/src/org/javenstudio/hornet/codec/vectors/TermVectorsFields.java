package org.javenstudio.hornet.codec.vectors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.field.Fields;
import org.javenstudio.common.indexdb.index.field.FieldsEnum;

final class TermVectorsFields extends Fields {
	
	private final StoredTermVectorsReader mReader;
	
	private final Map<Integer,Integer> mFieldNumberToIndex = 
    		new HashMap<Integer,Integer>();
	
    private final int[] mFieldNumbers;
    private final long[] mFieldFPs;
    
    public TermVectorsFields(StoredTermVectorsReader reader, int docID) 
    		throws IOException {
    	mReader = reader;
    	
    	mReader.seekTvx(docID);
    	mReader.getTvdStream().seek(mReader.getTvxStream().readLong());
      
    	final int fieldCount = mReader.getTvdStream().readVInt();
    	assert fieldCount >= 0;
    	
    	if (fieldCount != 0) {
    		mFieldNumbers = new int[fieldCount];
    		mFieldFPs = new long[fieldCount];
    		
    		for (int fieldUpto=0; fieldUpto < fieldCount; fieldUpto++) {
    			final int fieldNumber = mReader.getTvdStream().readVInt();
    			mFieldNumbers[fieldUpto] = fieldNumber;
    			mFieldNumberToIndex.put(fieldNumber, fieldUpto);
    		}

    		long position = mReader.getTvxStream().readLong();
    		mFieldFPs[0] = position;
    		
    		for (int fieldUpto=1; fieldUpto < fieldCount; fieldUpto++) {
    			position += mReader.getTvdStream().readVLong();
    			mFieldFPs[fieldUpto] = position;
    		}
    		
    	} else {
    		// TODO: we can improve writer here, eg write 0 into
    		// tvx file, so we know on first read from tvx that
    		// this doc has no TVs
    		mFieldNumbers = null;
    		mFieldFPs = null;
    	}
    }
    
    @Override
    public IFieldsEnum iterator() {
    	return new FieldsEnum() {
    		private int mFieldUpto;

    		private String getFieldName() { 
    			if (mFieldNumbers != null && mFieldUpto < mFieldNumbers.length) 
    				return mReader.getFieldInfos().getFieldInfo(mFieldNumbers[mFieldUpto]).getName();
    			else
    				return null;
    		}
    		
    		@Override
    		public String next() {
    			String name = getFieldName();
    			if (name != null) { 
    				mFieldUpto ++;
    				return name;
    			}
    			
    			throw new NoSuchElementException();
    		}

    		//@Override
    		//public boolean hasNext() {
    		//	return mFieldNumbers != null && mFieldUpto < mFieldNumbers.length;
    		//}
    		
    		@Override
    		public ITerms getTerms() throws IOException { 
    			return TermVectorsFields.this.getTerms(getFieldName());
    		}
    	};
    }

    @Override
    public ITerms getTerms(String field) throws IOException {
    	if (field == null) 
    		return null;
    	
    	final IFieldInfo fieldInfo = mReader.getFieldInfos().getFieldInfo(field);
    	if (fieldInfo == null) {
    		// No such field
    		return null;
    	}

    	final Integer fieldIndex = mFieldNumberToIndex.get(fieldInfo.getNumber());
    	if (fieldIndex == null) {
    		// Term vectors were not indexed for this field
    		return null;
    	}

    	return new TermVectorsTerms(mReader, mFieldFPs[fieldIndex]);
    }

    @Override
    public int size() {
    	if (mFieldNumbers != null) 
    		return mFieldNumbers.length;
    	
    	return 0;
    }
    
}
