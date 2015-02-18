package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.TableNotDisabledException;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Writables;

abstract class ColumnOperation extends TableOperation {
  private static final Logger LOG = Logger.getLogger(ColumnOperation.class);

  protected ColumnOperation(final DBMaster master, final byte [] tableName)
  throws IOException {
    super(master, tableName);
  }

  @Override
  protected void processScanItem(String serverName, final DBRegionInfo info)
  throws IOException {
    if (isEnabled(info)) {
      throw new TableNotDisabledException(tableName);
    }
  }

  protected void updateRegionInfo(DBRegionInterface server, byte [] regionName,
    DBRegionInfo i) throws IOException {
    Put put = new Put(i.getRegionName());
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER, Writables.getBytes(i));
    server.put(regionName, put);
    if (LOG.isDebugEnabled()) {
      LOG.debug("updated columns in row: " + i.getRegionNameAsString());
    }
  }
}

