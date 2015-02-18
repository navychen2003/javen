package org.javenstudio.falcon.datum.table.store;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for client-side scanning.
 * Go to {@link HTable} to obtain instances.
 */
public interface ResultScanner extends Closeable, Iterable<Result> {

  /**
   * Grab the next row's worth of values. The scanner will return a Result.
   * @return Result object if there is another row, null if the scanner is
   * exhausted.
   * @throws IOException e
   */
  public Result next() throws IOException;

  /**
   * @param nbRows number of rows to return
   * @return Between zero and <param>nbRows</param> Results
   * @throws IOException e
   */
  public Result [] next(int nbRows) throws IOException;

  /**
   * Closes the scanner and releases any resources it has allocated
   */
  public void close();
}
