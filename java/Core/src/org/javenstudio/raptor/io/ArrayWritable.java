package org.javenstudio.raptor.io;

import java.io.*;
import java.lang.reflect.Array;

/** 
 * A Writable for arrays containing instances of a class. The elements of this
 * writable must all be instances of the same class. If this writable will be
 * the input for a Reducer, you will need to create a subclass that sets the
 * value to be of the proper type.
 *
 * For example:
 * <code>
 * public class IntArrayWritable extends ArrayWritable {
 *   public IntArrayWritable() { 
 *     super(IntWritable.class); 
 *   }	
 * }
 * </code>
 */
public class ArrayWritable implements Writable {
  private Class<? extends Writable> valueClass;
  private Writable[] values;

  public ArrayWritable(Class<? extends Writable> valueClass) {
    if (valueClass == null) { 
      throw new IllegalArgumentException("null valueClass"); 
    }    
    this.valueClass = valueClass;
  }

  public ArrayWritable(Class<? extends Writable> valueClass, Writable[] values) {
    this(valueClass);
    this.values = values;
  }

  @SuppressWarnings("deprecation")
  public ArrayWritable(String[] strings) {
    this(UTF8.class, new Writable[strings.length]);
    for (int i = 0; i < strings.length; i++) {
      values[i] = new UTF8(strings[i]);
    }
  }

  @SuppressWarnings("rawtypes")
  public Class getValueClass() {
    return valueClass;
  }

  public String[] toStrings() {
    String[] strings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      strings[i] = values[i].toString();
    }
    return strings;
  }

  public Object toArray() {
    Object result = Array.newInstance(valueClass, values.length);
    for (int i = 0; i < values.length; i++) {
      Array.set(result, i, values[i]);
    }
    return result;
  }

  public void set(Writable[] values) { this.values = values; }

  public Writable[] get() { return values; }

  public void readFields(DataInput in) throws IOException {
    values = new Writable[in.readInt()];          // construct values
    for (int i = 0; i < values.length; i++) {
      Writable value = WritableFactories.newInstance(valueClass);
      value.readFields(in);                       // read a value
      values[i] = value;                          // store it in values
    }
  }

  public void write(DataOutput out) throws IOException {
    out.writeInt(values.length);                 // write values
    for (int i = 0; i < values.length; i++) {
      values[i].write(out);
    }
  }

}
