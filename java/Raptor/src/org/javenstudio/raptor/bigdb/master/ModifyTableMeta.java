package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.DBConstants;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.DBTableDescriptor;
import org.javenstudio.raptor.bigdb.TableNotDisabledException;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.bigdb.util.Writables;

/** Instantiated to modify table descriptor metadata */
class ModifyTableMeta extends TableOperation {

  private static Logger LOG = Logger.getLogger(ModifyTableMeta.class);

  private DBTableDescriptor desc;

  ModifyTableMeta(final DBMaster master, final byte [] tableName,
    DBTableDescriptor desc)
  throws IOException {
    super(master, tableName);
    this.desc = desc;
    LOG.debug("modifying " + Bytes.toString(tableName) + ": " +
        desc.toString());
  }

  protected void updateRegionInfo(DBRegionInterface server, byte [] regionName,
    DBRegionInfo i)
  throws IOException {
    Put put = new Put(i.getRegionName());
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER, Writables.getBytes(i));
    server.put(regionName, put);
    LOG.debug("updated DBTableDescriptor for region " + i.getRegionNameAsString());
  }

  @Override
  protected void processScanItem(String serverName,
      final DBRegionInfo info) throws IOException {
    if (isEnabled(info)) {
      throw new TableNotDisabledException(Bytes.toString(tableName));
    }
  }

  @Override
  protected void postProcessMeta(MetaRegion m, DBRegionInterface server)
  throws IOException {
    for (DBRegionInfo i: unservedRegions) {
      i.setTableDesc(desc);
      updateRegionInfo(server, m.getRegionName(), i);
    }
    // kick off a meta scan right away
    master.getRegionManager().metaScannerThread.triggerNow();
  }
}

