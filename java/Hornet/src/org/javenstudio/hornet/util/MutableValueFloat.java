package org.javenstudio.hornet.util;

/**
 * {@link MutableValue} implementation of type 
 * <code>float</code>.
 */
public class MutableValueFloat extends MutableValue {
  protected float mValue;

  public float get() { return mValue; }
  public void set(float value) { mValue = value; }
  
  @Override
  public Object toObject() {
    return mExists ? mValue : null;
  }

  @Override
  public void copy(MutableValue source) {
    MutableValueFloat s = (MutableValueFloat) source;
    mValue = s.mValue;
    mExists = s.mExists;
  }

  @Override
  public MutableValue duplicate() {
    MutableValueFloat v = new MutableValueFloat();
    v.mValue = this.mValue;
    v.mExists = this.mExists;
    return v;
  }

  @Override
  public boolean equalsSameType(Object other) {
    MutableValueFloat b = (MutableValueFloat)other;
    return mValue == b.mValue && mExists == b.mExists;
  }

  @Override
  public int compareSameType(Object other) {
    MutableValueFloat b = (MutableValueFloat)other;
    int c = Float.compare(mValue, b.mValue);
    if (c != 0) return c;
    if (mExists == b.mExists) return 0;
    return mExists ? 1 : -1;
  }

  @Override
  public int hashCode() {
    return Float.floatToIntBits(mValue);
  }
}
