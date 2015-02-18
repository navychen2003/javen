package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.raptor.io.Writable;

/**
 * Implementation of {@link Filter} that represents an ordered List of Filters
 * which will be evaluated with a specified boolean operator {@link Operator#MUST_PASS_ALL}
 * (<code>!AND</code>) or {@link Operator#MUST_PASS_ONE} (<code>!OR</code>).
 * Since you can use Filter Lists as children of Filter Lists, you can create a
 * hierarchy of filters to be evaluated.
 * Defaults to {@link Operator#MUST_PASS_ALL}.
 * <p>TODO: Fix creation of Configuration on serialization and deserialization.
 */
public class FilterList implements Filter {
  /** set operator */
  public static enum Operator {
    /** !AND */
    MUST_PASS_ALL,
    /** !OR */
    MUST_PASS_ONE
  }

  private Operator mOperator = Operator.MUST_PASS_ALL;
  private List<Filter> mFilters = new ArrayList<Filter>();

  /**
   * Default constructor, filters nothing. Required though for RPC
   * deserialization.
   */
  public FilterList() {
    super();
  }

  /**
   * Constructor that takes a set of {@link Filter}s. The default operator
   * MUST_PASS_ALL is assumed.
   *
   * @param rowFilters list of filters
   */
  public FilterList(final List<Filter> rowFilters) {
    this.mFilters = rowFilters;
  }

  /**
   * Constructor that takes an operator.
   *
   * @param operator Operator to process filter set with.
   */
  public FilterList(final Operator operator) {
    this.mOperator = operator;
  }

  /**
   * Constructor that takes a set of {@link Filter}s and an operator.
   *
   * @param operator Operator to process filter set with.
   * @param rowFilters Set of row filters.
   */
  public FilterList(final Operator operator, final List<Filter> rowFilters) {
    this.mFilters = rowFilters;
    this.mOperator = operator;
  }

  /**
   * Get the operator.
   *
   * @return operator
   */
  public Operator getOperator() {
    return mOperator;
  }

  /**
   * Get the filters.
   *
   * @return filters
   */
  public List<Filter> getFilters() {
    return mFilters;
  }

  /**
   * Add a filter.
   *
   * @param filter another filter
   */
  public void addFilter(Filter filter) {
	if (filter == null) return;
	
	for (Filter f : mFilters) {
	  if (f == filter) return;
	}
	
    this.mFilters.add(filter);
  }

  @Override
  public void reset() {
    for (Filter filter : mFilters) {
      filter.reset();
    }
  }

  @Override
  public boolean filterRowKey(byte[] rowKey, int offset, int length) {
    for (Filter filter : mFilters) {
      if (this.mOperator == Operator.MUST_PASS_ALL) {
        if (filter.filterAllRemaining() ||
            filter.filterRowKey(rowKey, offset, length)) {
          return true;
        }
      } else if (this.mOperator == Operator.MUST_PASS_ONE) {
        if (!filter.filterAllRemaining() &&
            !filter.filterRowKey(rowKey, offset, length)) {
          return false;
        }
      }
    }
    return this.mOperator == Operator.MUST_PASS_ONE;
  }

  @Override
  public boolean filterAllRemaining() {
    for (Filter filter : mFilters) {
      if (filter.filterAllRemaining()) {
        if (mOperator == Operator.MUST_PASS_ALL) {
          return true;
        }
      } else {
        if (mOperator == Operator.MUST_PASS_ONE) {
          return false;
        }
      }
    }
    return mOperator == Operator.MUST_PASS_ONE;
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue v) {
    for (Filter filter : mFilters) {
      if (mOperator == Operator.MUST_PASS_ALL) {
        if (filter.filterAllRemaining()) 
          return ReturnCode.NEXT_ROW;
        
        switch (filter.filterKeyValue(v)) {
        case INCLUDE:
          continue;
        case NEXT_ROW:
        case SKIP:
          return ReturnCode.SKIP;
		default:
			break;
        }
      } else if (mOperator == Operator.MUST_PASS_ONE) {
        if (filter.filterAllRemaining()) 
          continue;

        switch (filter.filterKeyValue(v)) {
        case INCLUDE:
          return ReturnCode.INCLUDE;
        case NEXT_ROW:
        case SKIP:
          // continue;
		default:
			break;
        }
      }
    }
    return mOperator == Operator.MUST_PASS_ONE?
      ReturnCode.SKIP: ReturnCode.INCLUDE;
  }

  @Override
  public void filterRow(List<KeyValue> kvs) {
    for (Filter filter : mFilters) {
      filter.filterRow(kvs);
    }
  }

  @Override
  public boolean hasFilterRow() {
    for (Filter filter : mFilters) {
      if (filter.hasFilterRow()) {
    	return true;
      }
    }
    return false;
  }

  @Override
  public boolean filterRow() {
    for (Filter filter : mFilters) {
      if (mOperator == Operator.MUST_PASS_ALL) {
        if (filter.filterAllRemaining() || filter.filterRow()) {
          return true;
        }
      } else if (mOperator == Operator.MUST_PASS_ONE) {
        if (!filter.filterAllRemaining()
            && !filter.filterRow()) {
          return false;
        }
      }
    }
    return  mOperator == Operator.MUST_PASS_ONE;
  }

  @Override
  public KeyValue getNextKeyHint(KeyValue currentKV) {
	for (Filter filter : mFilters) {
	  KeyValue kv = filter.getNextKeyHint(currentKV);
	  if (kv != null) return kv;
	}
	return null;
  }
  
  @Override
  public void readFields(final DataInput in) throws IOException {
    byte opByte = in.readByte();
    mOperator = Operator.values()[opByte];
    int size = in.readInt();
    if (size > 0) {
      mFilters = new ArrayList<Filter>(size);
      for (int i = 0; i < size; i++) {
        Filter filter = (Filter)DBObjectWritable.readObject(in, null);
        mFilters.add(filter);
      }
    }
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeByte(mOperator.ordinal());
    out.writeInt(mFilters.size());
    for (Filter filter : mFilters) {
      DBObjectWritable.writeObject(out, filter, Writable.class, null);
    }
  }
}
