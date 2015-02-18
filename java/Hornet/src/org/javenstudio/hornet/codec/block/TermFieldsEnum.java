package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.Iterator;

import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.index.field.FieldsEnum;

//Iterates through all fields
final class TermFieldsEnum extends FieldsEnum {
	
    private final Iterator<BlockFieldReader> mIt;
    private BlockFieldReader mCurrent;

    public TermFieldsEnum(BlockTreeTermsReader reader) {
    	mIt = reader.getFields().values().iterator();
    }

    @Override
    public String next() {
    	if (mIt.hasNext()) {
    		mCurrent = mIt.next();
    		return mCurrent.getFieldInfo().getName();
    	} else {
    		mCurrent = null;
    		return null;
    	}
    }
    
    @Override
    public ITerms getTerms() throws IOException {
    	return mCurrent;
    }
    
}
