package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBColumnDescriptor;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;

import java.io.IOException;

/** Instantiated to add a column family to a table */
class AddColumn extends ColumnOperation {
  private final DBColumnDescriptor newColumn;

  AddColumn(final DBMaster master, final byte [] tableName,
    final DBColumnDescriptor newColumn)
  throws IOException {
    super(master, tableName);
    this.newColumn = newColumn;
  }

  @Override
  protected void postProcessMeta(MetaRegion m, DBRegionInterface server)
  throws IOException {
    for (DBRegionInfo i: unservedRegions) {
      // All we need to do to add a column is add it to the table descriptor.
      // When the region is brought on-line, it will find the column missing
      // and create it.
      i.getTableDesc().addFamily(newColumn);
      updateRegionInfo(server, m.getRegionName(), i);
    }
  }
}
