package org.javenstudio.hornet.index.field;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.IFieldsEnum;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.field.FieldInfo;
import org.javenstudio.common.indexdb.index.field.FieldInfos;
import org.javenstudio.common.indexdb.index.field.FieldInfosBuilder;
import org.javenstudio.common.indexdb.index.field.Fields;
import org.javenstudio.common.indexdb.index.field.FieldsEnum;
import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.index.term.Terms;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.index.term.MultiBits;
import org.javenstudio.hornet.index.term.MultiTerms;

/**
 * Exposes flex API, merged from flex API of sub-segments.
 * This is useful when you're interacting with an {@link
 * IndexReader} implementation that consists of sequential
 * sub-readers (eg {@link DirectoryReader} or {@link
 * MultiReader}).
 *
 * <p><b>NOTE</b>: for composite readers, you'll get better
 * performance by gathering the sub readers using
 * {@link IndexReader#getTopReaderContext()} to get the
 * atomic leaves and then operate per-AtomicReader,
 * instead of using this class.
 *
 */
public final class MultiFields extends Fields {
	
	private final IFields[] mSubs;
	private final ReaderSlice[] mSubSlices;
	private final Map<String,Terms> mTerms = new ConcurrentHashMap<String,Terms>();

	/** 
	 * Returns a single {@link Fields} instance for this
	 *  reader, merging fields/terms/docs/positions on the
	 *  fly.  This method will return null if the reader 
	 *  has no postings.
	 *
	 *  <p><b>NOTE</b>: this is a slow way to access postings.
	 *  It's better to get the sub-readers and iterate through them
	 *  yourself. 
	 */
	public static IFields getFields(IIndexReader reader) throws IOException {
		final List<IAtomicReaderRef> leaves = reader.getReaderContext().getLeaves();
		switch (leaves.size()) {
		case 0:
			// no fields
			return null;
			
		case 1:
			// already an atomic reader / reader with one leave
			return leaves.get(0).getReader().getFields();
			
		default:
			final List<IFields> fields = new ArrayList<IFields>();
			final List<ReaderSlice> slices = new ArrayList<ReaderSlice>();
			
			for (final IAtomicReaderRef ctx : leaves) {
				final IAtomicReader r = ctx.getReader();
				final IFields f = r.getFields();
				if (f != null) {
					fields.add(f);
					slices.add(new ReaderSlice(ctx.getDocBase(), r.getMaxDoc(), fields.size()-1));
				}
			}
			
			if (fields.isEmpty()) {
				return null;
			} else if (fields.size() == 1) {
				return fields.get(0);
			} else {
				return new MultiFields(fields.toArray(new IFields[0]),
						slices.toArray(new ReaderSlice[0]));
			}
		}
	}

	public static Bits getLiveDocs(IIndexReader reader) {
		if (reader.hasDeletions()) {
			final List<IAtomicReaderRef> leaves = reader.getReaderContext().getLeaves();
			final int size = leaves.size();
			
			assert size > 0 : "A reader with deletions must have at least one leave";
			if (size == 1) 
				return leaves.get(0).getReader().getLiveDocs();
			
			final Bits[] liveDocs = new Bits[size];
			final int[] starts = new int[size + 1];
			
			for (int i = 0; i < size; i++) {
				// record all liveDocs, even if they are null
				final IAtomicReaderRef ctx = leaves.get(i);
				liveDocs[i] = ctx.getReader().getLiveDocs();
				starts[i] = ctx.getDocBase();
			}
			
			starts[size] = reader.getMaxDoc();
			return new MultiBits(liveDocs, starts, true);
		}
		
		return null;
	}

	/**  This method may return null if the field does not exist.*/
	public static ITerms getTerms(IIndexReader r, String field) throws IOException {
		final IFields fields = getFields(r);
		if (fields == null) 
			return null;
		else 
			return fields.getTerms(field);
	}
  
	/** 
	 * Returns {@link DocsEnum} for the specified field &
	 *  term.  This will return null if the field or term does
	 *  not exist. 
	 */
	public static IDocsEnum getTermDocsEnum(
			IIndexReader r, Bits liveDocs, String field, BytesRef term) 
			throws IOException {
		return getTermDocsEnum(r, liveDocs, field, term, IDocsEnum.FLAG_FREQS);
	}
	
	/** 
	 * Returns {@link DocsEnum} for the specified field &
	 *  term, with control over whether freqs are required.
	 *  Some codecs may be able to optimize their
	 *  implementation when freqs are not required.  This will
	 *  return null if the field or term does not exist.  See {@link
	 *  TermsEnum#docs(Bits,DocsEnum,int)}.
	 */
	public static IDocsEnum getTermDocsEnum(
			IIndexReader r, Bits liveDocs, String field, BytesRef term, int flags) 
			throws IOException {
		assert field != null;
		assert term != null;
		
		final ITerms terms = getTerms(r, field);
		if (terms != null) {
			final ITermsEnum termsEnum = terms.iterator(null);
			if (termsEnum.seekExact(term, true)) 
				return termsEnum.getDocs(liveDocs, null, flags);
		}
		
		return null;
	}

	/** 
	 * Returns {@link DocsAndPositionsEnum} for the specified
	 *  field & term.  This will return null if the field or
	 *  term does not exist or positions were not indexed. 
	 *  @see #getTermPositionsEnum(IndexReader, Bits, String, BytesRef, int) 
	 */
	public static IDocsAndPositionsEnum getTermPositionsEnum(
			IIndexReader r, Bits liveDocs, String field, BytesRef term) 
			throws IOException {
		return getTermPositionsEnum(r, liveDocs, field, term, 
				IDocsAndPositionsEnum.FLAG_OFFSETS | IDocsAndPositionsEnum.FLAG_PAYLOADS);
	}
	
	/** 
	 * Returns {@link DocsAndPositionsEnum} for the specified
	 *  field & term, with control over whether offsets and payloads are
	 *  required.  Some codecs may be able to optimize
	 *  their implementation when offsets and/or payloads are not
	 *  required. This will return null if the field or term does not
	 *  exist or positions were not indexed. See {@link
	 *  TermsEnum#docsAndPositions(Bits,DocsAndPositionsEnum,int)}. 
	 */
	public static IDocsAndPositionsEnum getTermPositionsEnum(
			IIndexReader r, Bits liveDocs, String field, BytesRef term, int flags) 
			throws IOException {
		assert field != null;
		assert term != null;
		
		final ITerms terms = getTerms(r, field);
		if (terms != null) {
			final ITermsEnum termsEnum = terms.iterator(null);
			if (termsEnum.seekExact(term, true)) 
				return termsEnum.getDocsAndPositions(liveDocs, null, flags);
		}
		
		return null;
	}

	public MultiFields(IFields[] subs, ReaderSlice[] subSlices) {
		mSubs = subs;
		mSubSlices = subSlices;
	}

	@Override
	public IFieldsEnum iterator() {
		final List<IFieldsEnum> fieldsEnums = new ArrayList<IFieldsEnum>();
		final List<ReaderSlice> fieldsSlices = new ArrayList<ReaderSlice>();
		
		for (int i=0; i < mSubs.length; i++) {
			fieldsEnums.add(mSubs[i].iterator());
			fieldsSlices.add(mSubSlices[i]);
		}
		
		if (fieldsEnums.size() == 0) 
			return FieldsEnum.EMPTY;
		
		return new MultiFieldsEnum(this,
				fieldsEnums.toArray(new IFieldsEnum[0]),
				fieldsSlices.toArray(new ReaderSlice[0]));
	}

	@Override
	public ITerms getTerms(String field) throws IOException {
		Terms result = mTerms.get(field);
		if (result != null)
			return result;

		// Lazy init: first time this field is requested, we
		// create & add to terms:
		final List<Terms> subs2 = new ArrayList<Terms>();
		final List<ReaderSlice> slices2 = new ArrayList<ReaderSlice>();

		// Gather all sub-readers that share this field
		for (int i=0; i < mSubs.length; i++) {
			final Terms terms = (Terms)mSubs[i].getTerms(field);
			if (terms != null) {
				subs2.add(terms);
				slices2.add(mSubSlices[i]);
			}
		}
		
		if (subs2.size() == 0) {
			result = null;
			// don't cache this case with an unbounded cache, since the number of fields that don't exist
			// is unbounded.
			
		} else {
			result = new MultiTerms(subs2.toArray(new Terms[0]),
					slices2.toArray(new ReaderSlice[0]));
			mTerms.put(field, result);
		}

		return result;
	}

	@Override
	public int size() { return -1; }

	public static long totalTermFreq(IIndexReader r, String field, BytesRef text) throws IOException {
		final Terms terms = (Terms)getTerms(r, field);
		if (terms != null) {
			final ITermsEnum termsEnum = terms.iterator(null);
			if (termsEnum.seekExact(text, true)) 
				return termsEnum.getTotalTermFreq();
		}
		return 0;
	}

	/** 
	 * Call this to get the (merged) FieldInfos for a
	 *  composite reader. 
	 *  <p>
	 *  NOTE: the returned field numbers will likely not
	 *  correspond to the actual field numbers in the underlying
	 *  readers, and codec metadata ({@link FieldInfo#getAttribute(String)}
	 *  will be unavailable.
	 */
	public static FieldInfos getMergedFieldInfos(IIndexReader reader) {
		final FieldInfosBuilder builder = new FieldInfosBuilder();
		for (final IAtomicReaderRef ctx : reader.getReaderContext().getLeaves()) {
			builder.add(ctx.getReader().getFieldInfos());
		}
		return builder.finish();
	}

	public static Collection<String> getIndexedFields(IIndexReader reader) {
		final Collection<String> fields = new HashSet<String>();
		for (final IFieldInfo fieldInfo : getMergedFieldInfos(reader)) {
			if (fieldInfo.isIndexed()) 
				fields.add(fieldInfo.getName());
		}
		return fields;
	}
	
}

