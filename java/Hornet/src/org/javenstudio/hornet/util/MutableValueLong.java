package org.javenstudio.hornet.util;

/**
 * {@link MutableValue} implementation of type 
 * <code>long</code>.
 */
public class MutableValueLong extends MutableValue {
  protected long mValue;

  public long get() { return mValue; }
  public void set(long value) { mValue = value; }
  
  @Override
  public Object toObject() {
    return mExists ? mValue : null;
  }

  @Override
  public void copy(MutableValue source) {
    MutableValueLong s = (MutableValueLong) source;
    mExists = s.mExists;
    mValue = s.mValue;
  }

  @Override
  public MutableValue duplicate() {
    MutableValueLong v = new MutableValueLong();
    v.mValue = this.mValue;
    v.mExists = this.mExists;
    return v;
  }

  @Override
  public boolean equalsSameType(Object other) {
    MutableValueLong b = (MutableValueLong)other;
    return mValue == b.mValue && mExists == b.mExists;
  }

  @Override
  public int compareSameType(Object other) {
    MutableValueLong b = (MutableValueLong)other;
    long bv = b.mValue;
    if (mValue < bv) return -1;
    if (mValue > bv) return 1;
    if (mExists == b.mExists) return 0;
    return mExists ? 1 : -1;
  }

  @Override
  public int hashCode() {
    return (int)mValue + (int)(mValue>>32);
  }
}
