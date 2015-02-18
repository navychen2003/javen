package org.javenstudio.hornet.util;

/**
 * {@link MutableValue} implementation of type 
 * <code>double</code>.
 */
public class MutableValueDouble extends MutableValue {
  protected double mValue;

  public double get() { return mValue; }
  public void set(double value) { mValue = value; }
  
  @Override
  public Object toObject() {
    return mExists ? mValue : null;
  }

  @Override
  public void copy(MutableValue source) {
    MutableValueDouble s = (MutableValueDouble) source;
    mValue = s.mValue;
    mExists = s.mExists;
  }

  @Override
  public MutableValue duplicate() {
    MutableValueDouble v = new MutableValueDouble();
    v.mValue = this.mValue;
    v.mExists = this.mExists;
    return v;
  }

  @Override
  public boolean equalsSameType(Object other) {
    MutableValueDouble b = (MutableValueDouble)other;
    return mValue == b.mValue && mExists == b.mExists;
  }

  @Override
  public int compareSameType(Object other) {
    MutableValueDouble b = (MutableValueDouble)other;
    int c = Double.compare(mValue, b.mValue);
    if (c != 0) return c;
    if (!mExists) return -1;
    if (!b.mExists) return 1;
    return 0;
  }

  @Override
  public int hashCode() {
    long x = Double.doubleToLongBits(mValue);
    return (int)x + (int)(x>>>32);
  }
}
