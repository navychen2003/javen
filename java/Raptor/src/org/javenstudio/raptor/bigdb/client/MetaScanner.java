package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.TableNotFoundException;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.Writables;

import java.io.IOException;

/**
 * Scanner class that contains the <code>.META.</code> table scanning logic
 * and uses a Retryable scanner. Provided visitors will be called
 * for each row.
 * 
 * Although public visibility, this is not a public-facing API and may evolve in
 * minor releases.
 */
public class MetaScanner {

  /**
   * Scans the meta table and calls a visitor on each RowResult and uses a empty
   * start row value as table name.
   *
   * @param configuration conf
   * @param visitor A custom visitor
   * @throws IOException e
   */
  public static void metaScan(Configuration configuration,
      MetaScannerVisitor visitor)
  throws IOException {
    metaScan(configuration, visitor, DBConstants.EMPTY_START_ROW);
  }

  /**
   * Scans the meta table and calls a visitor on each RowResult. Uses a table
   * name to locate meta regions.
   *
   * @param configuration config
   * @param visitor visitor object
   * @param tableName table name
   * @throws IOException e
   */
  public static void metaScan(Configuration configuration,
      MetaScannerVisitor visitor, byte[] tableName)
  throws IOException {
    metaScan(configuration, visitor, tableName, null, Integer.MAX_VALUE);
  }

  /**
   * Scans the meta table and calls a visitor on each RowResult. Uses a table
   * name and a row name to locate meta regions. And it only scans at most
   * <code>rowLimit</code> of rows.
   *
   * @param configuration BigDB configuration.
   * @param visitor Visitor object.
   * @param tableName User table name.
   * @param row Name of the row at the user table. The scan will start from
   * the region row where the row resides.
   * @param rowLimit Max of processed rows. If it is less than 0, it
   * will be set to default value <code>Integer.MAX_VALUE</code>.
   * @throws IOException e
   */
  public static void metaScan(Configuration configuration,
      MetaScannerVisitor visitor, byte[] tableName, byte[] row,
      int rowLimit)
  throws IOException {
    int rowUpperLimit = rowLimit > 0 ? rowLimit: Integer.MAX_VALUE;

    DBConnection connection = DBConnectionManager.getConnection(configuration);
    // if row is not null, we want to use the startKey of the row's region as
    // the startRow for the meta scan.
    byte[] startRow;
    if (row != null) {
      // Scan starting at a particular row in a particular table
      assert tableName != null;
      byte[] searchRow =
        DBRegionInfo.createRegionName(tableName, row, DBConstants.NINES,
          false);

      DBTable metaTable = new DBTable(configuration, DBConstants.META_TABLE_NAME);
      Result startRowResult = metaTable.getRowOrBefore(searchRow,
          DBConstants.CATALOG_FAMILY);
      if (startRowResult == null) {
        throw new TableNotFoundException("Cannot find row in .META. for table: "
            + Bytes.toString(tableName) + ", row=" + Bytes.toString(searchRow));
      }
      byte[] value = startRowResult.getValue(DBConstants.CATALOG_FAMILY,
          DBConstants.REGIONINFO_QUALIFIER);
      if (value == null || value.length == 0) {
        throw new IOException("DBRegionInfo was null or empty in Meta for " +
          Bytes.toString(tableName) + ", row=" + Bytes.toString(searchRow));
      }
      DBRegionInfo regionInfo = Writables.getDBRegionInfo(value);

      byte[] rowBefore = regionInfo.getStartKey();
      startRow = DBRegionInfo.createRegionName(tableName, rowBefore,
          DBConstants.ZEROES, false);
    } else if (tableName == null || tableName.length == 0) {
      // Full META scan
      startRow = DBConstants.EMPTY_START_ROW;
    } else {
      // Scan META for an entire table
      startRow = DBRegionInfo.createRegionName(
          tableName, DBConstants.EMPTY_START_ROW, DBConstants.ZEROES, false);
    }

    // Scan over each meta region
    ScannerCallable callable;
    int rows = Math.min(rowLimit,
        configuration.getInt("bigdb.meta.scanner.caching", 100));
    do {
      final Scan scan = new Scan(startRow).addFamily(DBConstants.CATALOG_FAMILY);
      callable = new ScannerCallable(connection, DBConstants.META_TABLE_NAME,
          scan);
      // Open scanner
      connection.getRegionServerWithRetries(callable);

      int processedRows = 0;
      try {
        callable.setCaching(rows);
        done: do {
          if (processedRows >= rowUpperLimit) {
            break;
          }
          //we have all the rows here
          Result [] rrs = connection.getRegionServerWithRetries(callable);
          if (rrs == null || rrs.length == 0 || rrs[0].size() == 0) {
            break; //exit completely
          }
          for (Result rr : rrs) {
            if (processedRows >= rowUpperLimit) {
              break done;
            }
            if (!visitor.processRow(rr))
              break done; //exit completely
            processedRows++;
          }
          //here, we didn't break anywhere. Check if we have more rows
        } while(true);
        // Advance the startRow to the end key of the current region
        startRow = callable.getDBRegionInfo().getEndKey();
      } finally {
        // Close scanner
        callable.setClose();
        connection.getRegionServerWithRetries(callable);
      }
    } while (Bytes.compareTo(startRow, DBConstants.LAST_ROW) != 0);
  }

  /**
   * Visitor class called to process each row of the .META. table
   */
  public interface MetaScannerVisitor {
    /**
     * Visitor method that accepts a RowResult and the meta region location.
     * Implementations can return false to stop the region's loop if it becomes
     * unnecessary for some reason.
     *
     * @param rowResult result
     * @return A boolean to know if it should continue to loop in the region
     * @throws IOException e
     */
    public boolean processRow(Result rowResult) throws IOException;
  }
}

