package org.javenstudio.raptor.bigdb.regionserver;

/**
 * Implementing classes of this interface will be used for the tracking
 * and enforcement of columns and numbers of versions during the course of a
 * Get or Scan operation.
 * <p>
 * Currently there are two different types of Store/Family-level queries.
 * <ul><li>{@link ExplicitColumnTracker} is used when the query specifies
 * one or more column qualifiers to return in the family.
 * <p>
 * This class is utilized by {@link ScanQueryMatcher} through two methods:
 * <ul><li>{@link #checkColumn} is called when a Put satisfies all other
 * conditions of the query.  This method returns a {@link org.javenstudio.raptor.bigdb.regionserver.ScanQueryMatcher.MatchCode} to define
 * what action should be taken.
 * <li>{@link #update} is called at the end of every StoreFile or memstore.
 * <p>
 * This class is NOT thread-safe as queries are never multi-threaded
 */
public interface ColumnTracker {
  /**
   * Keeps track of the number of versions for the columns asked for
   * @param bytes
   * @param offset
   * @param length
   * @return The match code instance.
   */
  public ScanQueryMatcher.MatchCode checkColumn(byte [] bytes, int offset, int length);

  /**
   * Updates internal variables in between files
   */
  public void update();

  /**
   * Resets the Matcher
   */
  public void reset();

  /**
   *
   * @return <code>true</code> when done.
   */
  public boolean done();

  /**
   * Used by matcher and scan/get to get a hint of the next column
   * to seek to after checkColumn() returns SKIP.  Returns the next interesting
   * column we want, or NULL there is none (wildcard scanner).
   *
   * Implementations aren't required to return anything useful unless the most recent
   * call was to checkColumn() and the return code was SKIP.  This is pretty implementation
   * detail-y, but optimizations are like that.
   *
   * @return null, or a ColumnCount that we should seek to
   */
  public ColumnCount getColumnHint();
}

