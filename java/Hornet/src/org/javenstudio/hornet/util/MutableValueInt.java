package org.javenstudio.hornet.util;

/**
 * {@link MutableValue} implementation of type 
 * <code>int</code>.
 */
public class MutableValueInt extends MutableValue {
  protected int mValue;
  
  public int get() { return mValue; }
  public void set(int value) { mValue = value; }
  
  @Override
  public Object toObject() {
    return mExists ? mValue : null;
  }

  @Override
  public void copy(MutableValue source) {
    MutableValueInt s = (MutableValueInt) source;
    mValue = s.mValue;
    mExists = s.mExists;
  }

  @Override
  public MutableValue duplicate() {
    MutableValueInt v = new MutableValueInt();
    v.mValue = this.mValue;
    v.mExists = this.mExists;
    return v;
  }

  @Override
  public boolean equalsSameType(Object other) {
    MutableValueInt b = (MutableValueInt)other;
    return mValue == b.mValue && mExists == b.mExists;
  }

  @Override
  public int compareSameType(Object other) {
    MutableValueInt b = (MutableValueInt)other;
    int ai = mValue;
    int bi = b.mValue;
    if (ai<bi) return -1;
    else if (ai>bi) return 1;

    if (mExists == b.mExists) return 0;
    return mExists ? 1 : -1;
  }

  @Override
  public int hashCode() {
    // TODO: if used in HashMap, it already mixes the value... maybe use a straight value?
    return (mValue>>8) + (mValue>>16);
  }
}
