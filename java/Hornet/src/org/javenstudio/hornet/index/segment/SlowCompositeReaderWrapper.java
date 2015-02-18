package org.javenstudio.hornet.index.segment;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.index.field.MultiFields;

/**
 * This class forces a composite reader (eg a {@link
 * MultiReader} or {@link DirectoryReader}) to emulate an
 * atomic reader.  This requires implementing the postings
 * APIs on-the-fly, using the static methods in {@link
 * MultiFields}, {@link MultiDocValues}, by stepping through
 * the sub-readers to merge fields/terms, appending docs, etc.
 *
 * <p><b>NOTE</b>: this class almost always results in a
 * performance hit.  If this is important to your use case,
 * you'll get better performance by gathering the sub readers using
 * {@link IndexReader#getContext()} to get the
 * atomic leaves and then operate per-AtomicReader,
 * instead of using this class.
 */
public final class SlowCompositeReaderWrapper extends AtomicIndexReader {

	private final CompositeIndexReader mReader;
	//private final Map<String, DocValues> normsCache = new HashMap<String, DocValues>();
	private final IFields mFields;
	private final Bits mLiveDocs;
  
	/** 
	 * This method is sugar for getting an {@link AtomicReader} from
	 * an {@link IndexReader} of any kind. If the reader is already atomic,
	 * it is returned unchanged, otherwise wrapped by this class.
	 */
	public static IAtomicReader wrap(IIndexReader reader) throws IOException {
		if (reader instanceof CompositeIndexReader) {
			return new SlowCompositeReaderWrapper((CompositeIndexReader) reader);
		} else {
			assert reader instanceof IAtomicReader;
			return (IAtomicReader) reader;
		}
	}

	/** 
	 * Sole constructor, wrapping the provided {@link CompositeReader}. 
	 */
	public SlowCompositeReaderWrapper(CompositeIndexReader reader) throws IOException {
		super();
		
		mReader = reader;
		mFields = MultiFields.getFields(mReader);
		mLiveDocs = MultiFields.getLiveDocs(mReader);
		mReader.registerParentReader(this);
	}

	@Override
	public IIndexContext getContext() {
		// TODO Auto-generated method stub
		return mReader.getContext();
	}
	
	@Override
	public IDirectory getDirectory() { 
		return mReader.getDirectory();
	}
	
	@Override
	public IFields getFields() {
		ensureOpen();
		return mFields;
	}

	//@Override
	//public DocValues docValues(String field) throws IOException {
	//	ensureOpen();
	//	return MultiDocValues.getDocValues(in, field);
	//}
  
	//@Override
	//public synchronized DocValues normValues(String field) throws IOException {
	//	ensureOpen();
	//	DocValues values = normsCache.get(field);
	//	if (values == null) {
	//		values = MultiDocValues.getNormDocValues(in, field);
	//		normsCache.put(field, values);
	//	}
	//	return values;
	//}
  
	@Override
	public IFields getTermVectors(int docID) throws IOException {
		ensureOpen();
		return mReader.getTermVectors(docID);
	}

	@Override
	public int getNumDocs() {
		// Don't call ensureOpen() here (it could affect performance)
		return mReader.getNumDocs();
	}

	@Override
	public int getMaxDoc() {
		// Don't call ensureOpen() here (it could affect performance)
		return mReader.getMaxDoc();
	}

	@Override
	public void document(int docID, IFieldVisitor visitor) throws IOException {
		ensureOpen();
		mReader.document(docID, visitor);
	}

	@Override
	public Bits getLiveDocs() {
		ensureOpen();
		return mLiveDocs;
	}

	@Override
	public IFieldInfos getFieldInfos() {
		ensureOpen();
		return MultiFields.getMergedFieldInfos(mReader);
	}
  
	@Override
	public boolean hasDeletions() {
		ensureOpen();
		return mLiveDocs != null;
	}

	@Override
	public Object getCacheKey() {
		return mReader.getCacheKey();
	}

	@Override
	public Object getCombinedCoreAndDeletesKey() {
		return mReader.getCombinedCoreAndDeletesKey();
	}

	@Override
	protected void doClose() throws IOException {
		// TODO: as this is a wrapper, should we really close the delegate?
		mReader.close();
	}
	
	@Override
	public String toString() {
		return "SlowCompositeReaderWrapper{" + mReader + "}";
	}
	
}
