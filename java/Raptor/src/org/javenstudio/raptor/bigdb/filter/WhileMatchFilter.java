package org.javenstudio.raptor.bigdb.filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.bigdb.KeyValue;

/**
 * A wrapper filter that returns true from {@link #filterAllRemaining()} as soon
 * as the wrapped filters {@link Filter#filterRowKey(byte[], int, int)},
 * {@link Filter#filterKeyValue(org.javenstudio.raptor.bigdb.KeyValue)},
 * {@link org.javenstudio.raptor.bigdb.filter.Filter#filterRow()} or
 * {@link org.javenstudio.raptor.bigdb.filter.Filter#filterAllRemaining()} methods
 * returns true.
 */
public class WhileMatchFilter extends FilterBase {
  private boolean filterAllRemaining = false;
  private Filter filter;

  public WhileMatchFilter() {
    super();
  }

  public WhileMatchFilter(Filter filter) {
    this.filter = filter;
  }

  public Filter getFilter() {
    return filter;
  }

  public void reset() {
    this.filter.reset();
  }

  private void changeFAR(boolean value) {
    filterAllRemaining = filterAllRemaining || value;
  }

  public boolean filterAllRemaining() {
    return this.filterAllRemaining || this.filter.filterAllRemaining();
  }

  public boolean filterRowKey(byte[] buffer, int offset, int length) {
    boolean value = filter.filterRowKey(buffer, offset, length);
    changeFAR(value);
    return value;
  }

  public ReturnCode filterKeyValue(KeyValue v) {
    ReturnCode c = filter.filterKeyValue(v);
    changeFAR(c != ReturnCode.INCLUDE);
    return c;
  }

  public boolean filterRow() {
    boolean filterRow = this.filter.filterRow();
    changeFAR(filterRow);
    return filterRow;
  }

  public void write(DataOutput out) throws IOException {
    out.writeUTF(this.filter.getClass().getName());
    this.filter.write(out);
  }

  public void readFields(DataInput in) throws IOException {
    String className = in.readUTF();
    try {
      this.filter = (Filter)(Class.forName(className).newInstance());
      this.filter.readFields(in);
    } catch (InstantiationException e) {
      throw new RuntimeException("Failed deserialize.", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed deserialize.", e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Failed deserialize.", e);
    }
  }
}

