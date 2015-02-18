package org.javenstudio.hornet.util;

/**
 * {@link MutableValue} implementation of type 
 * <code>boolean</code>.
 */
public class MutableValueBool extends MutableValue {
  protected boolean mValue;

  public boolean get() { return mValue; }
  public void set(boolean value) { mValue = value; }
  
  @Override
  public Object toObject() {
    return mExists ? mValue : null;
  }

  @Override
  public void copy(MutableValue source) {
    MutableValueBool s = (MutableValueBool) source;
    mValue = s.mValue;
    mExists = s.mExists;
  }

  @Override
  public MutableValue duplicate() {
    MutableValueBool v = new MutableValueBool();
    v.mValue = this.mValue;
    v.mExists = this.mExists;
    return v;
  }

  @Override
  public boolean equalsSameType(Object other) {
    MutableValueBool b = (MutableValueBool)other;
    return mValue == b.mValue && mExists == b.mExists;
  }

  @Override
  public int compareSameType(Object other) {
    MutableValueBool b = (MutableValueBool)other;
    if (mValue != b.mValue) return mValue ? 1 : 0;
    if (mExists == b.mExists) return 0;
    return mExists ? 1 : -1;
  }

  @Override
  public int hashCode() {
    return mValue ? 2 : (mExists ? 1 : 0);
  }
}
