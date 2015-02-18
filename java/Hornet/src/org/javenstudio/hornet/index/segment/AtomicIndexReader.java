package org.javenstudio.hornet.index.segment;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.field.FieldInfos;
import org.javenstudio.common.indexdb.index.field.Fields;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

/** 
 * {@code AtomicReader} is an abstract class, providing an interface for accessing an
 * index.  Search of an index is done entirely through this abstract interface,
 * so that any subclass which implements it is searchable. IndexReaders implemented
 * by this subclass do not consist of several sub-readers,
 * they are atomic. They support retrieval of stored fields, doc values, terms,
 * and postings.
 *
 * <p>For efficiency, in this API documents are often referred to via
 * <i>document numbers</i>, non-negative integers which each name a unique
 * document in the index.  These document numbers are ephemeral -- they may change
 * as documents are added to and deleted from an index.  Clients should thus not
 * rely on a given document having the same number between sessions.
 *
 * <p>
 * <a name="thread-safety"></a><p><b>NOTE</b>: {@link
 * IndexReader} instances are completely thread
 * safe, meaning multiple threads can call any of its methods,
 * concurrently.  If your application requires external
 * synchronization, you should <b>not</b> synchronize on the
 * <code>IndexReader</code> instance; use your own
 * (non-Lucene) objects instead.
 */
public abstract class AtomicIndexReader extends IndexReader implements IAtomicReader {

	private final AtomicIndexReaderRef mReaderContext = new AtomicIndexReaderRef(this);
	
	protected AtomicIndexReader() {
		super();
	}

	@Override
	public IAtomicReaderRef getReaderContext() { 
		ensureOpen();
		return mReaderContext;
	}
	
	/**
	 * Returns {@link Fields} for this reader.
	 * This method may return null if the reader has no
	 * postings.
	 */
	@Override
	public abstract IFields getFields() throws IOException;
  
	@Override
	public final int getDocFreq(String field, BytesRef term) throws IOException {
		final IFields fields = getFields();
		if (fields == null) 
			return 0;
		
		final ITerms terms = fields.getTerms(field);
		if (terms == null) 
			return 0;
		
		final ITermsEnum termsEnum = terms.iterator(null);
		if (termsEnum.seekExact(term, true)) 
			return termsEnum.getDocFreq();
		else 
			return 0;
	}

	/** 
	 * Returns the number of documents containing the term
	 * <code>t</code>.  This method returns 0 if the term or
	 * field does not exists.  This method does not take into
	 * account deleted documents that have not yet been merged
	 * away. 
	 */
	@Override
	public final long getTotalTermFreq(ITerm term) throws IOException {
		final IFields fields = getFields();
		if (fields == null) 
			return 0;
		
		final ITerms terms = fields.getTerms(term.getField());
		if (terms == null) 
			return 0;
		
		final ITermsEnum termsEnum = terms.iterator(null);
		if (termsEnum.seekExact(term.getBytes(), true)) 
			return termsEnum.getTotalTermFreq();
		else 
			return 0;
	}

	/** This may return null if the field does not exist.*/
	public final ITerms getTerms(String field) throws IOException {
		final IFields fields = getFields();
		if (fields == null) 
			return null;
		
		return fields.getTerms(field);
	}

	/** 
	 * Returns {@link DocsEnum} for the specified field &
	 *  term.  This will return null if either the field or
	 *  term does not exist. 
	 */
	public final IDocsEnum getTermDocsEnum(Bits liveDocs, String field, BytesRef term) 
			throws IOException {
		return getTermDocsEnum(liveDocs, field, term, IDocsEnum.FLAG_FREQS);
	}
	
	/** 
	 * Returns {@link DocsEnum} for the specified field &
	 *  term, with control over whether freqs are required.
	 *  Some codecs may be able to optimize their
	 *  implementation when freqs are not required. This will
	 *  return null if the field or term does not
	 *  exist.  See {@link TermsEnum#docs(Bits,DocsEnum,int)}. 
	 */
	public final IDocsEnum getTermDocsEnum(Bits liveDocs, String field, BytesRef term, 
			int flags) throws IOException {
		assert field != null && term != null;
		final IFields fields = getFields();
		if (fields != null) {
			final ITerms terms = fields.getTerms(field);
			if (terms != null) {
				final ITermsEnum termsEnum = terms.iterator(null);
				if (termsEnum.seekExact(term, true)) 
					return termsEnum.getDocs(liveDocs, null, flags);
			}
		}
		return null;
	}

	/** 
	 * Returns {@link DocsAndPositionsEnum} for the specified
	 *  field & term.  This will return null if the
	 *  field or term does not exist or positions weren't indexed. 
	 *  @see #termPositionsEnum(Bits, String, BytesRef, int) 
	 */
	public final IDocsAndPositionsEnum getTermPositionsEnum(Bits liveDocs, 
			String field, BytesRef term) throws IOException {
		return getTermPositionsEnum(liveDocs, field, term, 
				IDocsAndPositionsEnum.FLAG_OFFSETS | IDocsAndPositionsEnum.FLAG_PAYLOADS);
	}
	
	/** 
	 * Returns {@link DocsAndPositionsEnum} for the specified
	 *  field & term, with control over whether offsets and payloads are
	 *  required.  Some codecs may be able to optimize their
	 *  implementation when offsets and/or payloads are not required.
	 *  This will return null if the field or term
	 *  does not exist or positions weren't indexed.  See
	 *  {@link TermsEnum#docsAndPositions(Bits,DocsAndPositionsEnum,int)}. 
	 */
	public final IDocsAndPositionsEnum getTermPositionsEnum(Bits liveDocs, 
			String field, BytesRef term, int flags) throws IOException {
		assert field != null && term != null;
		final IFields fields = getFields();
		if (fields != null) {
			final ITerms terms = fields.getTerms(field);
			if (terms != null) {
				final ITermsEnum termsEnum = terms.iterator(null);
				if (termsEnum.seekExact(term, true)) 
					return termsEnum.getDocsAndPositions(liveDocs, null, flags);
			}
		}
		return null;
	}

	/** 
	 * Returns the number of unique terms (across all fields)
	 *  in this reader.
	 */
	public final long getUniqueTermCount() throws IOException {
		final IFields fields = getFields();
		if (fields == null) 
			return 0;
		
		return fields.getUniqueTermCount();
	}
  
	/**
	 * Get the {@link FieldInfos} describing all fields in
	 * this reader.
	 * 
	 */
	public abstract IFieldInfos getFieldInfos();
  
	/** 
	 * Returns the {@link Bits} representing live (not
	 *  deleted) docs.  A set bit indicates the doc ID has not
	 *  been deleted.  If this method returns null it means
	 *  there are no deleted documents (all documents are
	 *  live).
	 *
	 *  The returned instance has been safely published for
	 *  use by multiple threads without additional
	 *  synchronization.
	 */
	public abstract Bits getLiveDocs();
	
}
