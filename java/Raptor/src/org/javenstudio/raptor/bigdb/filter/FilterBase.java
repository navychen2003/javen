package org.javenstudio.raptor.bigdb.filter;

import org.javenstudio.raptor.bigdb.KeyValue;

import java.util.List;

/**
 * Abstract base class to help you implement new Filters.  Common "ignore" or NOOP type
 * methods can go here, helping to reduce boiler plate in an ever-expanding filter
 * library.
 *
 * If you could instantiate FilterBase, it would end up being a "null" filter -
 * that is one that never filters anything.
 *
 * @inheritDoc
 */
public abstract class FilterBase implements Filter {

  /**
   * Filters that are purely stateless and do nothing in their reset() methods can inherit
   * this null/empty implementation.
   *
   * @inheritDoc
   */
  @Override
  public void reset() {
  }

  /**
   * Filters that do not filter by row key can inherit this implementation that
   * never filters anything. (ie: returns false).
   *
   * @inheritDoc
   */
  @Override
  public boolean filterRowKey(byte [] buffer, int offset, int length) {
    return false;
  }

  /**
   * Filters that never filter all remaining can inherit this implementation that
   * never stops the filter early.
   *
   * @inheritDoc
   */
  @Override
  public boolean filterAllRemaining() {
    return false;
  }

  /**
   * Filters that dont filter by key value can inherit this implementation that
   * includes all KeyValues.
   *
   * @inheritDoc
   */
  @Override
  public ReturnCode filterKeyValue(KeyValue ignored) {
    return ReturnCode.INCLUDE;
  }

  /**
   * Filters that never filter by modifying the returned List of KeyValues can
   * inherit this implementation that does nothing.
   *
   * @inheritDoc
   */
  @Override
  public void filterRow(List<KeyValue> ignored) {
  }

  /**
   * Fitlers that never filter by modifying the returned List of KeyValues can
   * inherit this implementation that does nothing.
   *
   * @inheritDoc
   */
  @Override
  public boolean hasFilterRow() {
    return false;
  }

  /**
   * Filters that never filter by rows based on previously gathered state from
   * @{link #filterKeyValue(KeyValue)} can inherit this implementation that
   * never filters a row.
   *
   * @inheritDoc
   */
  @Override
  public boolean filterRow() {
    return false;
  }

  /**
   * Filters that are not sure which key must be next seeked to, can inherit
   * this implementation that, by default, returns a null KeyValue.
   *
   * @inheritDoc
   */
  public KeyValue getNextKeyHint(KeyValue currentKV) {
    return null;
  }

}

