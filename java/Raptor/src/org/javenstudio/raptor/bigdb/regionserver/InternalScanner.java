package org.javenstudio.raptor.bigdb.regionserver;

import org.javenstudio.raptor.bigdb.KeyValue;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Internal scanners differ from client-side scanners in that they operate on
 * HStoreKeys and byte[] instead of RowResults. This is because they are
 * actually close to how the data is physically stored, and therefore it is more
 * convenient to interact with them that way. It is also much easier to merge
 * the results across SortedMaps than RowResults.
 *
 * <p>Additionally, we need to be able to determine if the scanner is doing
 * wildcard column matches (when only a column family is specified or if a
 * column regex is specified) or if multiple members of the same column family
 * were specified. If so, we need to ignore the timestamp to ensure that we get
 * all the family members, as they may have been last updated at different
 * times.
 */
public interface InternalScanner extends Closeable {
  /**
   * Grab the next row's worth of values.
   * @param results return output array
   * @return true if more rows exist after this one, false if scanner is done
   * @throws IOException e
   */
  public boolean next(List<KeyValue> results) throws IOException;

  /**
   * Grab the next row's worth of values with a limit on the number of values
   * to return.
   * @param result return output array
   * @param limit limit on row count to get
   * @return true if more rows exist after this one, false if scanner is done
   * @throws IOException e
   */
  public boolean next(List<KeyValue> result, int limit) throws IOException;

  /**
   * Closes the scanner and releases any resources it has allocated
   * @throws IOException
   */
  public void close() throws IOException;
}

