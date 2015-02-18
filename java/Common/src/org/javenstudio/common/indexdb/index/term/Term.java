package org.javenstudio.common.indexdb.index.term;

import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * A Term represents a word from text.  This is the unit of search.  It is
 * composed of two elements, the text of the word, as a string, and the name of
 * the field that the text occurred in.
 *
 * Note that terms may represent more than words from text fields, but also
 * things like dates, email addresses, urls, etc.
 */
public final class Term implements ITerm {
	
	private String mField;
	private BytesRef mBytes;

  	/** 
  	 * Constructs a Term with the given field and bytes.
  	 * <p>Note that a null field or null bytes value results in undefined
  	 * behavior for most Indexdb APIs that accept a Term parameter. 
  	 *
  	 * <p>WARNING: the provided BytesRef is not copied, but used directly.
  	 * Therefore the bytes should not be modified after construction, for
  	 * example, you should clone a copy rather than pass reused bytes from
  	 * a TermsEnum.
  	 */
  	public Term(String fld, BytesRef bytes) {
  		mField = fld;
  		mBytes = bytes;
  	}

  	/** 
  	 * Constructs a Term with the given field and text.
  	 * <p>Note that a null field or null text value results in undefined
  	 * behavior for most Indexdb APIs that accept a Term parameter. 
  	 */
  	public Term(String fld, String text) {
  		this(fld, new BytesRef(text));
  	}

  	/** 
  	 * Constructs a Term with the given field and empty text.
  	 * This serves two purposes: 1) reuse of a Term with the same field.
  	 * 2) pattern for a query.
  	 * 
  	 * @param fld
  	 */
  	public Term(String fld) {
  		this(fld, new BytesRef());
  	}

  	/** 
  	 * Returns the field of this term.   The field indicates
     * the part of a document which this term came from. 
     */
  	@Override
  	public final String getField() { 
  		return mField; 
  	}

  	/** 
  	 * Returns the text of this term.  In the case of words, this is simply the
     * text of the word.  In the case of dates and other types, this is an
     * encoding of the object as a string.
     */
  	@Override
  	public final String getText() { 
  		return mBytes.utf8ToString(); 
  	}

  	/** Returns the bytes of this term. */
  	@Override
  	public final BytesRef getBytes() { 
  		return mBytes; 
  	}

  	@Override
  	public boolean equals(Object obj) {
  		if (this == obj) return true;
  		if (obj == null) return false;
  		if (getClass() != obj.getClass())
  			return false;
  		
  		Term other = (Term) obj;
  		if (mField == null) {
  			if (other.mField != null)
  				return false;
  		} else if (!mField.equals(other.mField))
  			return false;
  		if (mBytes == null) {
  			if (other.mBytes != null)
  				return false;
  		} else if (!mBytes.equals(other.mBytes)) {
  			return false;
  		}
  		
  		return true;
  	}

  	@Override
  	public int hashCode() {
  		final int prime = 31;
  		int result = 1;
  		result = prime * result + ((mField == null) ? 0 : mField.hashCode());
  		result = prime * result + ((mBytes == null) ? 0 : mBytes.hashCode());
  		return result;
  	}

  	/** 
  	 * Compares two terms, returning a negative integer if this
  	 * term belongs before the argument, zero if this term is equal to the
  	 * argument, and a positive integer if this term belongs after the argument.
  	 *
  	 * The ordering of terms is first by field, then by text.
  	 */
  	@Override
  	public final int compareTo(ITerm other) {
  		if (getClass() != other.getClass()) {
  			return -1;
  		} else if (mField.equals(((Term)other).mField)) {
  			return mBytes.compareTo(((Term)other).mBytes);
  		} else {
  			return mField.compareTo(((Term)other).mField);
  		}
  	}

  	/** 
  	 * Resets the field and text of a Term. 
  	 * <p>WARNING: the provided BytesRef is not copied, but used directly.
  	 * Therefore the bytes should not be modified after construction, for
  	 * example, you should clone a copy rather than pass reused bytes from
  	 * a TermsEnum.
  	 */
  	public final void set(String fld, BytesRef bytes) {
  		mField = fld;
  		mBytes = bytes;
  	}

  	public final void setField(String fld) { 
  		mField = fld;
  	}
  	
  	@Override
  	public final String toString() { 
  		return mField + ":" + mBytes.utf8ToString(); 
  	}

  	public ITerm deepCopyOf() {
  		return new Term(mField, BytesRef.deepCopyOf(mBytes));
  	}
  	
}
