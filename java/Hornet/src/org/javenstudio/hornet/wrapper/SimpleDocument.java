package org.javenstudio.hornet.wrapper;

import java.io.Reader;

import org.javenstudio.common.indexdb.IDocument;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.document.Document;
import org.javenstudio.common.indexdb.document.Field;
import org.javenstudio.common.indexdb.document.FieldType;

public final class SimpleDocument {
	//private static final Logger LOG = Logger.getLogger(SimpleDocument.class);
	
    public static final int FLAG_INDEX = 1<<0;
    public static final int FLAG_TOKENIZE = 1<<1;
    public static final int FLAG_STORE_FIELD = 1<<2;
    public static final int FLAG_STORE_TERMVECTORS = 1<<3;
    public static final int FLAG_STORE_TERMVECTOR_OFFSETS = 1<<4;
    public static final int FLAG_STORE_TERMVECTOR_POSITIONS = 1<<5;
    public static final int FLAG_OMIT_NORMS = 1<<6;
    
    public static final int INDEX_DOCS = 1<<1;
    public static final int INDEX_FREQS = 1<<2;
    public static final int INDEX_POSITIONS = 1<<3;
    public static final int INDEX_OFFSETS = 1<<4;
	
	private final IDocument mDoc;
	private final boolean mCanChange;
	
	public SimpleDocument() {
		mDoc = new Document();
		mCanChange = true;
	}
	
	SimpleDocument(IDocument doc) { 
		mDoc = doc;
		mCanChange = false;
	}
	
	IDocument getDocument() { 
		return mDoc;
	}
	
	public SimpleField getField(String name) { 
		IField field = mDoc.getField(name);
		return field != null ? new SimpleField(field) : null;
	}
	
	private boolean isFlagSet(int value, int flag) { 
		return (value & flag) == flag;
	}
	
	private FieldType createFieldType(int flags, int indexOptions) { 
		boolean stored = isFlagSet(flags, FLAG_STORE_FIELD);
		boolean indexed = isFlagSet(flags, FLAG_INDEX);
		boolean tokenized = isFlagSet(flags, FLAG_TOKENIZE);
		boolean store_termvectors = isFlagSet(flags, FLAG_STORE_TERMVECTORS);
		boolean store_termvector_offsets = isFlagSet(flags, FLAG_STORE_TERMVECTOR_OFFSETS);
		boolean store_termvector_positions = isFlagSet(flags, FLAG_STORE_TERMVECTOR_POSITIONS);
		boolean omitNorms = isFlagSet(flags, FLAG_OMIT_NORMS);
		
		IndexOptions options = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
		if (isFlagSet(indexOptions, INDEX_DOCS)) { 
			options = IndexOptions.DOCS_ONLY;
			if (isFlagSet(indexOptions, INDEX_FREQS)) {
				options = IndexOptions.DOCS_AND_FREQS;
				if (isFlagSet(indexOptions, INDEX_POSITIONS)) { 
					options = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
					if (isFlagSet(indexOptions, INDEX_OFFSETS))
						options = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
				}
			}
		}
		
		if (!stored && !indexed) 
			throw new IllegalArgumentException("FLAG_INDEX or FLAG_STORE_FIELD must be set");
		
		FieldType type = new FieldType();
		type.setIndexed(indexed);
		type.setStored(stored);
		type.setTokenized(tokenized);
		type.setStoreTermVectors(store_termvectors);
		type.setStoreTermVectorOffsets(store_termvector_offsets);
		type.setStoreTermVectorPositions(store_termvector_positions);
		type.setOmitNorms(omitNorms);
		type.setIndexOptions(options);
		type.freeze();
		
		return type;
	}
	
	public void addField(String name, String value, int flags) { 
		addField(name, value, flags, 0);
	}
	
	public void addField(String name, String value, int flags, int indexOptions) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null || value == null) throw new NullPointerException();
		addField(Field.createText(name, value, createFieldType(flags, indexOptions)));
	}
	
	public void addField(String name, Reader value, int flags) { 
		addField(name, value, flags, 0);
	}
	
	public void addField(String name, Reader value, int flags, int indexOptions) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null || value == null) throw new NullPointerException();
		addField(Field.createText(name, value, createFieldType(flags, indexOptions)));
	}
	
	public void addField(String name, byte[] value) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null || value == null) throw new NullPointerException();
		addField(Field.create(name, value));
	}
	
	public void addField(String name, byte[] value, int offset, int length) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null || value == null) throw new NullPointerException();
		addField(Field.create(name, value, offset, length));
	}
	
	public void addField(String name, int value, boolean stored) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null) throw new NullPointerException();
		addField(Field.create(name, value, stored ? Field.Store.YES : Field.Store.NO));
	}
	
	public void addField(String name, long value, boolean stored) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null) throw new NullPointerException();
		addField(Field.create(name, value, stored ? Field.Store.YES : Field.Store.NO));
	}
	
	public void addField(String name, float value, boolean stored) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null) throw new NullPointerException();
		addField(Field.create(name, value, stored ? Field.Store.YES : Field.Store.NO));
	}
	
	public void addField(String name, double value, boolean stored) { 
		if (!mCanChange) throw new IllegalArgumentException("document cannot change");
		if (name == null) throw new NullPointerException();
		addField(Field.create(name, value, stored ? Field.Store.YES : Field.Store.NO));
	}
	
	private void addField(IField field) { 
		if (field == null) return;
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("addField: " + field);
		
		((Document)mDoc).addField(field);
	}
	
}
