package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Implementation of Filter interface that limits results to a specific page
 * size. It terminates scanning once the number of filter-passed rows is >
 * the given page size.
 * <p>
 * Note that this filter cannot guarantee that the number of results returned
 * to a client are <= page size. This is because the filter is applied
 * separately on different region servers. It does however optimize the scan of
 * individual HRegions by making sure that the page size is never exceeded
 * locally.
 */
public class PageFilter extends FilterBase {

  private long mPageSize = Long.MAX_VALUE;
  private int mRowsAccepted = 0;

  /**
   * Default constructor, filters nothing. Required though for RPC
   * deserialization.
   */
  public PageFilter() {
    super();
  }

  /**
   * Constructor that takes a maximum page size.
   *
   * @param pageSize Maximum result size.
   */
  public PageFilter(final long pageSize) {
    this.mPageSize = pageSize;
  }

  public long getPageSize() {
    return mPageSize;
  }
  
  public void setPageSize(long pageSize) { 
	mPageSize = pageSize;
  }

  @Override
  public boolean filterAllRemaining() {
    return this.mRowsAccepted >= this.mPageSize;
  }

  @Override
  public boolean filterRow() {
    this.mRowsAccepted++;
    return this.mRowsAccepted > this.mPageSize;
  }

  @Override
  public void readFields(final DataInput in) throws IOException {
    this.mPageSize = in.readLong();
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeLong(mPageSize);
  }
  
}
