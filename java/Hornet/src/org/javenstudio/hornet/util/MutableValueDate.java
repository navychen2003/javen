package org.javenstudio.hornet.util;

import java.util.Date;

/**
 * {@link MutableValue} implementation of type 
 * {@link Date}.
 */
public class MutableValueDate extends MutableValueLong {
  @Override
  public Object toObject() {
    return mExists ? new Date(mValue) : null;
  }

  @Override
  public MutableValue duplicate() {
    MutableValueDate v = new MutableValueDate();
    v.mValue = this.mValue;
    v.mExists = this.mExists;
    return v;
  }  
}