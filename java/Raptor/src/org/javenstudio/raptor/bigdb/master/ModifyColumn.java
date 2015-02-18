package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.bigdb.DBColumnDescriptor;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.util.Bytes;

import java.io.IOException;

/** Instantiated to modify an existing column family on a table */
class ModifyColumn extends ColumnOperation {
  private final DBColumnDescriptor descriptor;
  private final byte [] columnName;

  ModifyColumn(final DBMaster master, final byte [] tableName,
    final byte [] columnName, DBColumnDescriptor descriptor)
  throws IOException {
    super(master, tableName);
    this.descriptor = descriptor;
    this.columnName = columnName;
  }

  @Override
  protected void postProcessMeta(MetaRegion m, DBRegionInterface server)
  throws IOException {
    for (DBRegionInfo i: unservedRegions) {
      if (i.getTableDesc().hasFamily(columnName)) {
        i.getTableDesc().addFamily(descriptor);
        updateRegionInfo(server, m.getRegionName(), i);
      } else { // otherwise, we have an error.
        throw new InvalidColumnNameException("Column family '" +
          Bytes.toString(columnName) +
          "' doesn't exist, so cannot be modified.");
      }
    }
  }
}

