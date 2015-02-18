package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.RemoteExceptionHandler;
import org.javenstudio.raptor.bigdb.TableNotDisabledException;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.regionserver.DBRegion;
import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * Instantiated to delete a table. Table must be offline.
 */
class TableDelete extends TableOperation {
  private static final Logger LOG = Logger.getLogger(TableDelete.class);

  TableDelete(final DBMaster master, final byte [] tableName) throws IOException {
    super(master, tableName);
  }

  @Override
  protected void processScanItem(String serverName,
      final DBRegionInfo info) throws IOException {
    if (isEnabled(info)) {
      LOG.debug("Region still enabled: " + info.toString());
      throw new TableNotDisabledException(tableName);
    }
  }

  @Override
  protected void postProcessMeta(MetaRegion m, DBRegionInterface server)
  throws IOException {
    for (DBRegionInfo i: unservedRegions) {
      if (!Bytes.equals(this.tableName, i.getTableDesc().getName())) {
        // Don't delete regions that are not from our table.
        continue;
      }
      // Delete the region
      try {
        DBRegion.removeRegionFromMETA(server, m.getRegionName(), i.getRegionName());
        DBRegion.deleteRegion(this.master.getFileSystem(),
          this.master.getRootDir(), i);

      } catch (IOException e) {
        LOG.error("failed to delete region " + Bytes.toString(i.getRegionName()),
          RemoteExceptionHandler.checkIOException(e));
      }
    }

    // delete the table's folder from fs.
    this.master.getFileSystem().delete(new Path(this.master.getRootDir(),
      Bytes.toString(this.tableName)), true);
  }
}
