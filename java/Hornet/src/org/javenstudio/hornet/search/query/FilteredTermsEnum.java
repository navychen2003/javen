package org.javenstudio.hornet.search.query;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.ITermState;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Abstract class for enumerating a subset of all terms. 
 * 
 * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * greater than all that precede it.</p>
 * <p><em>Please note:</em> Consumers of this enum cannot
 * call {@code seek()}, it is forward only; it throws
 * {@link UnsupportedOperationException} when a seeking method
 * is called.
 */
public abstract class FilteredTermsEnum extends TermsEnum {

	private final ITermsEnum mTermsEnum;
	
	private BytesRef mInitialSeekTerm = null;
	private BytesRef mActualTerm = null;
	private boolean mDoSeek;
  
	/** 
	 * Return if term is accepted, not accepted or the iteration should ended
	 * (and possibly seek).
	 */
	protected abstract AcceptStatus accept(BytesRef term) throws IOException;

	/**
	 * Creates a filtered {@link TermsEnum} on a terms enum.
	 * @param tenum the terms enumeration to filter.
	 */
	public FilteredTermsEnum(final ITermsEnum tenum) {
		this(tenum, true);
	}

	/**
	 * Creates a filtered {@link TermsEnum} on a terms enum.
	 * @param tenum the terms enumeration to filter.
	 */
	public FilteredTermsEnum(final ITermsEnum tenum, final boolean startWithSeek) {
		assert tenum != null;
		mTermsEnum = tenum;
		mDoSeek = startWithSeek;
	}

	/**
	 * Use this method to set the initial {@link BytesRef}
	 * to seek before iterating. This is a convenience method for
	 * subclasses that do not override {@link #nextSeekTerm}.
	 * If the initial seek term is {@code null} (default),
	 * the enum is empty.
	 * <P>You can only use this method, if you keep the default
	 * implementation of {@link #nextSeekTerm}.
	 */
	protected final void setInitialSeekTerm(BytesRef term) {
		mInitialSeekTerm = term;
	}
  
	/** 
	 * On the first call to {@link #next} or if {@link #accept} returns
	 * {@link AcceptStatus#YES_AND_SEEK} or {@link AcceptStatus#NO_AND_SEEK},
	 * this method will be called to eventually seek the underlying TermsEnum
	 * to a new position.
	 * On the first call, {@code currentTerm} will be {@code null}, later
	 * calls will provide the term the underlying enum is positioned at.
	 * This method returns per default only one time the initial seek term
	 * and then {@code null}, so no repositioning is ever done.
	 * <p>Override this method, if you want a more sophisticated TermsEnum,
	 * that repositions the iterator during enumeration.
	 * If this method always returns {@code null} the enum is empty.
	 * <p><em>Please note:</em> This method should always provide a greater term
	 * than the last enumerated term, else the behaviour of this enum
	 * violates the contract for TermsEnums.
	 */
	protected BytesRef nextSeekTerm(final BytesRef currentTerm) throws IOException {
		final BytesRef t = mInitialSeekTerm;
		mInitialSeekTerm = null;
		return t;
	}

	@Override
	public BytesRef getTerm() throws IOException {
		return mTermsEnum.getTerm();
	}

	@Override
	public Comparator<BytesRef> getComparator() {
		return mTermsEnum.getComparator();
	}
    
	@Override
	public int getDocFreq() throws IOException {
		return mTermsEnum.getDocFreq();
	}

	@Override
	public long getTotalTermFreq() throws IOException {
		return mTermsEnum.getTotalTermFreq();
	}

	/** 
	 * This enum does not support seeking!
	 * @throws UnsupportedOperationException In general, subclasses do not
	 *         support seeking.
	 */
	@Override
	public boolean seekExact(BytesRef term, boolean useCache) throws IOException {
		throw new UnsupportedOperationException(getClass().getName() + " does not support seeking");
	}

	/** 
	 * This enum does not support seeking!
	 * @throws UnsupportedOperationException In general, subclasses do not
	 *         support seeking.
	 */
	@Override
	public SeekStatus seekCeil(BytesRef term, boolean useCache) throws IOException {
		throw new UnsupportedOperationException(getClass().getName() + " does not support seeking");
	}

	/** 
	 * This enum does not support seeking!
	 * @throws UnsupportedOperationException In general, subclasses do not
	 *         support seeking.
	 */
	@Override
	public void seekExact(long ord) throws IOException {
		throw new UnsupportedOperationException(getClass().getName() + " does not support seeking");
	}

	@Override
	public long getOrd() throws IOException {
		return mTermsEnum.getOrd();
	}

	@Override
	public IDocsEnum getDocs(Bits bits, IDocsEnum reuse, int flags) throws IOException {
		return mTermsEnum.getDocs(bits, reuse, flags);
	}
    
	@Override
	public IDocsAndPositionsEnum getDocsAndPositions(Bits bits, 
			IDocsAndPositionsEnum reuse, int flags) throws IOException {
		return mTermsEnum.getDocsAndPositions(bits, reuse, flags);
	}
  
	/** 
	 * This enum does not support seeking!
	 * @throws UnsupportedOperationException In general, subclasses do not
	 *         support seeking.
	 */
	@Override
	public void seekExact(BytesRef term, ITermState state) throws IOException {
		throw new UnsupportedOperationException(getClass().getName() + " does not support seeking");
	}
  
	/** Returns the filtered enums term state */
	@Override
	public ITermState getTermState() throws IOException {
		assert mTermsEnum != null;
		return mTermsEnum.getTermState();
	}

	@SuppressWarnings("fallthrough")
	@Override
	public BytesRef next() throws IOException {
		for (;;) {
			// Seek or forward the iterator
			if (mDoSeek) {
				mDoSeek = false;
				
				final BytesRef t = nextSeekTerm(mActualTerm);
				// Make sure we always seek forward:
				assert mActualTerm == null || t == null || getComparator().compare(t, mActualTerm) > 0: 
					"curTerm=" + mActualTerm + " seekTerm=" + t;
				
				if (t == null || mTermsEnum.seekCeil(t, false) == SeekStatus.END) {
					// no more terms to seek to or enum exhausted
					return null;
				}
				
				mActualTerm = mTermsEnum.getTerm();
				
			} else {
				mActualTerm = mTermsEnum.next();
				if (mActualTerm == null) {
					// enum exhausted
					return null;
				}
			}
      
			// check if term is accepted
			switch (accept(mActualTerm)) {
			case YES_AND_SEEK:
				mDoSeek = true;
				// term accepted, but we need to seek so fall-through
			case YES:
				// term accepted
				return mActualTerm;
			case NO_AND_SEEK:
				// invalid term, seek next time
				mDoSeek = true;
				break;
			case END:
				// we are supposed to end the enum
				return null;
			default:
				break;
			}
		}
	}

}
