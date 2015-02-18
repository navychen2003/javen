package org.javenstudio.hornet.index;

import java.io.IOException;
import java.util.Iterator;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.index.term.Term;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.store.ram.RAMFile;
import org.javenstudio.common.indexdb.store.ram.RAMInputStream;
import org.javenstudio.common.indexdb.store.ram.RAMOutputStream;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Prefix codes term instances (prefixes are shared)
 * 
 */
final class PrefixCodedTerms implements Iterable<ITerm> {
	
	private final IIndexContext mContext;
	private final RAMFile mBuffer;
  
	private PrefixCodedTerms(IIndexContext context, RAMFile buffer) {
		mContext = context;
		mBuffer = buffer;
	}
  
	/** @return size in bytes */
	public long getSizeInBytes() {
		return mBuffer.getSizeInBytes();
	}
  
	/** @return iterator over the bytes */
	@Override
	public Iterator<ITerm> iterator() {
		return new PrefixCodedTermsIterator();
	}
  
	private class PrefixCodedTermsIterator implements Iterator<ITerm> {
		private final IndexInput mInput;
		private String mField = "";
		private BytesRef mBytes = new BytesRef();
		private Term mTerm = new Term(mField, mBytes);

		PrefixCodedTermsIterator() {
			try {
				mInput = new RAMInputStream(mContext, mBuffer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean hasNext() {
			return mInput.getFilePointer() < mInput.length();
		}
    
		@Override
		public ITerm next() {
			assert hasNext();
			try {
				int code = mInput.readVInt();
				if ((code & 1) != 0) {
					// new field
					mField = mInput.readString();
				}
				int prefix = code >>> 1;
				int suffix = mInput.readVInt();
				
				mBytes.grow(prefix + suffix);
				mInput.readBytes(mBytes.getBytes(), prefix, suffix);
				mBytes.mLength = prefix + suffix;
				
				mTerm.set(mField, mBytes);
				return mTerm;
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
    
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
  
	/** Builds a PrefixCodedTerms: call add repeatedly, then finish. */
	public static class Builder {
		private final IIndexContext mContext;
		private final RAMFile mBuffer;
		private final RAMOutputStream mOutput;
		private final Term mLastTerm;

		public Builder(IIndexContext context) { 
			mContext = context;
			mBuffer = new RAMFile();
			mOutput = new RAMOutputStream(context, mBuffer);
			mLastTerm = new Term("");
		}
		
		/** add a term */
		public void add(ITerm term) {
			assert mLastTerm.equals(new Term("")) || term.compareTo(mLastTerm) > 0;

			try {
				int prefix = sharedPrefix(mLastTerm.getBytes(), term.getBytes());
				int suffix = term.getBytes().mLength - prefix;
				
				if (term.getField().equals(mLastTerm.getField())) {
					mOutput.writeVInt(prefix << 1);
				} else {
					mOutput.writeVInt(prefix << 1 | 1);
					mOutput.writeString(term.getField());
				}
				
				mOutput.writeVInt(suffix);
				mOutput.writeBytes(term.getBytes().mBytes, term.getBytes().mOffset + prefix, suffix);
				
				mLastTerm.getBytes().copyBytes(term.getBytes());
				mLastTerm.setField(term.getField());
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
    
		/** return finalized form */
		public PrefixCodedTerms finish() {
			try {
				mOutput.close();
				return new PrefixCodedTerms(mContext, mBuffer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
    
		private int sharedPrefix(BytesRef term1, BytesRef term2) {
			int pos1 = 0;
			int pos1End = pos1 + Math.min(term1.getLength(), term2.getLength());
			int pos2 = 0;
			
			while (pos1 < pos1End) {
				if (term1.mBytes[term1.mOffset + pos1] != term2.mBytes[term2.mOffset + pos2]) 
					return pos1;
				
				pos1 ++;
				pos2 ++;
			}
			
			return pos1;
		}
	}
	
}
