package org.javenstudio.hornet.util;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * {@link MutableValue} implementation of type 
 * {@link String}.
 */
public class MutableValueStr extends MutableValue {
  protected BytesRef mValue = new BytesRef();

  public BytesRef get() { return mValue; }
  public void set(BytesRef value) { mValue = value; }
  
  @Override
  public Object toObject() {
    return mExists ? mValue.utf8ToString() : null;
  }

  @Override
  public void copy(MutableValue source) {
    MutableValueStr s = (MutableValueStr) source;
    mExists = s.mExists;
    mValue.copyBytes(s.mValue);
  }

  @Override
  public MutableValue duplicate() {
    MutableValueStr v = new MutableValueStr();
    v.mValue.copyBytes(mValue);
    v.mExists = this.mExists;
    return v;
  }

  @Override
  public boolean equalsSameType(Object other) {
    MutableValueStr b = (MutableValueStr)other;
    return mValue.equals(b.mValue) && mExists == b.mExists;
  }

  @Override
  public int compareSameType(Object other) {
    MutableValueStr b = (MutableValueStr)other;
    int c = mValue.compareTo(b.mValue);
    if (c != 0) return c;
    if (mExists == b.mExists) return 0;
    return mExists ? 1 : -1;
  }

  @Override
  public int hashCode() {
    return mValue.hashCode();
  }
}
