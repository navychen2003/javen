package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;

final class DocTermOrdsCache extends Cache {
	public DocTermOrdsCache(FieldCacheImpl wrapper) {
		super(wrapper);
    }

    @Override
    protected Object createValue(IAtomicReader reader, Entry entryKey, 
    		boolean setDocsWithField /* ignored */) throws IOException {
    	return reader.getContext().createDocTermOrds(reader, entryKey.mField);
    }
    
}
