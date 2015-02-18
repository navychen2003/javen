package org.javenstudio.raptor.bigdb.filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.bigdb.KeyValue;

/**
 * A wrapper filter that filters an entire row if any of the KeyValue checks do
 * not pass.
 * <p>
 * For example, if all columns in a row represent weights of different things,
 * with the values being the actual weights, and we want to filter out the
 * entire row if any of its weights are zero.  In this case, we want to prevent
 * rows from being emitted if a single key is filtered.  Combine this filter
 * with a {@link ValueFilter}:
 * <p>
 * <pre>
 * scan.setFilter(new SkipFilter(new ValueFilter(CompareOp.EQUAL,
 *     new BinaryComparator(Bytes.toBytes(0))));
 * </code>
 * Any row which contained a column whose value was 0 will be filtered out.
 * Without this filter, the other non-zero valued columns in the row would still
 * be emitted.
 */
public class SkipFilter extends FilterBase {
  private boolean filterRow = false;
  private Filter filter;

  public SkipFilter() {
    super();
  }

  public SkipFilter(Filter filter) {
    this.filter = filter;
  }

  public Filter getFilter() {
    return filter;
  }

  public void reset() {
    filter.reset();
    filterRow = false;
  }

  private void changeFR(boolean value) {
    filterRow = filterRow || value;
  }

  public ReturnCode filterKeyValue(KeyValue v) {
    ReturnCode c = filter.filterKeyValue(v);
    changeFR(c != ReturnCode.INCLUDE);
    return c;
  }

  public boolean filterRow() {
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

