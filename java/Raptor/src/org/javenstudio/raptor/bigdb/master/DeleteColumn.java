package org.javenstudio.raptor.bigdb.master;

import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.bigdb.ipc.DBRegionInterface;
import org.javenstudio.raptor.bigdb.regionserver.Store;

import java.io.IOException;

/** Instantiated to remove a column family from a table */
class DeleteColumn extends ColumnOperation {
  private final byte [] columnName;

  DeleteColumn(final DBMaster master, final byte [] tableName,
    final byte [] columnName)
  throws IOException {
    super(master, tableName);
    this.columnName = columnName;
  }

  @Override
  protected void postProcessMeta(MetaRegion m, DBRegionInterface server)
  throws IOException {
    for (DBRegionInfo i: unservedRegions) {
      i.getTableDesc().removeFamily(columnName);
      updateRegionInfo(server, m.getRegionName(), i);
      // Delete the directories used by the column
      Path tabledir =
        new Path(this.master.getRootDir(), i.getTableDesc().getNameAsString());
      this.master.getFileSystem().
        delete(Store.getStoreHomedir(tabledir, i.getEncodedName(),
        this.columnName), true);
    }
  }
}
