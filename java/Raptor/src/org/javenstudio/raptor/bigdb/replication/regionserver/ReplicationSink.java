package org.javenstudio.raptor.bigdb.replication.regionserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.TableNotFoundException;
import org.javenstudio.raptor.bigdb.client.Delete;
import org.javenstudio.raptor.bigdb.client.DBTableInterface;
import org.javenstudio.raptor.bigdb.client.DBTablePool;
import org.javenstudio.raptor.bigdb.client.Put;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;
import org.javenstudio.raptor.bigdb.regionserver.wal.WALEdit;
import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * This class is responsible for replicating the edits coming
 * from another cluster.
 * <p/>
 * This replication process is currently waiting for the edits to be applied
 * before the method can return. This means that the replication of edits
 * is synchronized (after reading from DBLogs in ReplicationSource) and that a
 * single region server cannot receive edits from two sources at the same time
 * <p/>
 * This class uses the native HBase client in order to replicate entries.
 * <p/>
 *
 * TODO make this class more like ReplicationSource wrt log handling
 */
public class ReplicationSink {

  private static final Logger LOG = Logger.getLogger(ReplicationSink.class);
  // Name of the HDFS directory that contains the temporary rep logs
  public static final String REPLICATION_LOG_DIR = ".replogs";
  private final Configuration conf;
  // Pool used to replicated
  private final DBTablePool pool;
  // boolean coming from HRS to know when the process stops
  private final AtomicBoolean stop;
  private final ReplicationSinkMetrics metrics;

  /**
   * Create a sink for replication
   *
   * @param conf                conf object
   * @param stopper             boolean to tell this thread to stop
   * @throws IOException thrown when HDFS goes bad or bad file name
   */
  public ReplicationSink(Configuration conf, AtomicBoolean stopper)
      throws IOException {
    this.conf = conf;
    this.pool = new DBTablePool(this.conf,
        conf.getInt("replication.sink.htablepool.capacity", 10));
    this.stop = stopper;
    this.metrics = new ReplicationSinkMetrics();
  }

  /**
   * Replicate this array of entries directly into the local cluster
   * using the native client.
   *
   * @param entries
   * @throws IOException
   */
  public void replicateEntries(DBLog.Entry[] entries)
      throws IOException {
    if (entries.length == 0) {
      return;
    }
    // Very simple optimization where we batch sequences of rows going
    // to the same table.
    try {
      long totalReplicated = 0;
      // Map of table => list of puts, we only want to flushCommits once per
      // invocation of this method per table.
      Map<byte[], List<Put>> puts = new TreeMap<byte[], List<Put>>(Bytes.BYTES_COMPARATOR);
      for (DBLog.Entry entry : entries) {
        WALEdit edit = entry.getEdit();
        List<KeyValue> kvs = edit.getKeyValues();
        if (kvs.get(0).isDelete()) {
          Delete delete = new Delete(kvs.get(0).getRow(),
              kvs.get(0).getTimestamp(), null);
          for (KeyValue kv : kvs) {
            if (kv.isDeleteFamily()) {
              delete.deleteFamily(kv.getFamily());
            } else if (!kv.isEmptyColumn()) {
              delete.deleteColumn(kv.getFamily(),
                  kv.getQualifier());
            }
          }
          delete(entry.getKey().getTablename(), delete);
        } else {
          byte[] table = entry.getKey().getTablename();
          List<Put> tableList = puts.get(table);
          if (tableList == null) {
            tableList = new ArrayList<Put>();
            puts.put(table, tableList);
          }
          // With mini-batching, we need to expect multiple rows per edit
          byte[] lastKey = kvs.get(0).getRow();
          Put put = new Put(kvs.get(0).getRow(),
              kvs.get(0).getTimestamp());
          for (KeyValue kv : kvs) {
            if (!Bytes.equals(lastKey, kv.getRow())) {
              tableList.add(put);
              put = new Put(kv.getRow(), kv.getTimestamp());
            }
            put.add(kv.getFamily(), kv.getQualifier(), kv.getValue());
            lastKey = kv.getRow();
          }
          tableList.add(put);
        }
        totalReplicated++;
      }
      for(byte [] table : puts.keySet()) {
        put(table, puts.get(table));
      }
      this.metrics.setAgeOfLastAppliedOp(
          entries[entries.length-1].getKey().getWriteTime());
      this.metrics.appliedBatchesRate.inc(1);
      LOG.info("Total replicated: " + totalReplicated);
    } catch (IOException ex) {
      if (ex.getCause() instanceof TableNotFoundException) {
        LOG.warn("Losing edits because: ", ex);
      } else {
        // Should we log rejected edits in a file for replay?
        LOG.error("Unable to accept edit because", ex);
        this.stop.set(true);
        throw ex;
      }
    } catch (RuntimeException re) {
      if (re.getCause() instanceof TableNotFoundException) {
        LOG.warn("Losing edits because: ", re);
      } else {
        this.stop.set(true);
        throw re;
      }
    }
  }

  /**
   * Do the puts and handle the pool
   * @param tableName table to insert into
   * @param puts list of puts
   * @throws IOException
   */
  private void put(byte[] tableName, List<Put> puts) throws IOException {
    if (puts.isEmpty()) {
      return;
    }
    DBTableInterface table = null;
    try {
      table = this.pool.getTable(tableName);
      table.put(puts);
      this.metrics.appliedOpsRate.inc(puts.size());
    } finally {
      if (table != null) {
        this.pool.putTable(table);
      }
    }
  }

  /**
   * Do the delete and handle the pool
   * @param tableName table to delete in
   * @param delete the delete to use
   * @throws IOException
   */
  private void delete(byte[] tableName, Delete delete) throws IOException {
    DBTableInterface table = null;
    try {
      table = this.pool.getTable(tableName);
      table.delete(delete);
      this.metrics.appliedOpsRate.inc(1);
    } finally {
      if (table != null) {
        this.pool.putTable(table);
      }
    }
  }
}

