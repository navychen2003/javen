package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.javenstudio.raptor.io.Writable;

/**
 * WALEdit: Used in HBase's transaction log (WAL) to represent
 * the collection of edits (KeyValue objects) corresponding to a
 * single transaction. The class implements "Writable" interface
 * for serializing/deserializing a set of KeyValue items.
 *
 * Previously, if a transaction contains 3 edits to c1, c2, c3 for a row R,
 * the DBLog would have three log entries as follows:
 *
 *    <logseq1-for-edit1>:<KeyValue-for-edit-c1>
 *    <logseq2-for-edit2>:<KeyValue-for-edit-c2>
 *    <logseq3-for-edit3>:<KeyValue-for-edit-c3>
 *
 * This presents problems because row level atomicity of transactions
 * was not guaranteed. If we crash after few of the above appends make
 * it, then recovery will restore a partial transaction.
 *
 * In the new world, all the edits for a given transaction are written
 * out as a single record, for example:
 *
 *   <logseq#-for-entire-txn>:<WALEdit-for-entire-txn>
 *
 * where, the WALEdit is serialized as:
 *   <-1, # of edits, <KeyValue>, <KeyValue>, ... >
 * For example:
 *   <-1, 3, <Keyvalue-for-edit-c1>, <KeyValue-for-edit-c2>, <KeyValue-for-edit-c3>>
 *
 * The -1 marker is just a special way of being backward compatible with
 * an old DBLog which would have contained a single <KeyValue>.
 *
 * The deserializer for WALEdit backward compatibly detects if the record
 * is an old style KeyValue or the new style WALEdit.
 *
 */
public class WALEdit implements Writable {
  private static final int VERSION_2 = -1;

  private final ArrayList<KeyValue> mKvs = new ArrayList<KeyValue>();
  private NavigableMap<byte[], Integer> mScopes;

  public WALEdit() {}

  public void add(KeyValue kv) {
    this.mKvs.add(kv);
  }

  public boolean isEmpty() {
    return mKvs.isEmpty();
  }

  public int size() {
    return mKvs.size();
  }

  public List<KeyValue> getKeyValues() {
    return mKvs;
  }

  public NavigableMap<byte[], Integer> getScopes() {
    return mScopes;
  }

  public void setScopes (NavigableMap<byte[], Integer> scopes) {
    // We currently process the map outside of WALEdit,
    // TODO revisit when replication is part of core
    this.mScopes = scopes;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    mKvs.clear();
    if (mScopes != null) 
      mScopes.clear();
    
    int versionOrLength = in.readInt();
    if (versionOrLength == VERSION_2) {
      // this is new style DBLog entry containing multiple KeyValues.
      int numEdits = in.readInt();
      for (int idx = 0; idx < numEdits; idx++) {
        KeyValue kv = new KeyValue();
        kv.readFields(in);
        this.add(kv);
      }
      int numFamilies = in.readInt();
      if (numFamilies > 0) {
        if (mScopes == null) 
          mScopes = new TreeMap<byte[], Integer>(Bytes.BYTES_COMPARATOR);
        
        for (int i = 0; i < numFamilies; i++) {
          byte[] fam = Bytes.readByteArray(in);
          int scope = in.readInt();
          mScopes.put(fam, scope);
        }
      }
    } else {
      // this is an old style DBLog entry. The int that we just
      // read is actually the length of a single KeyValue.
      KeyValue kv = new KeyValue();
      kv.readFields(versionOrLength, in);
      this.add(kv);
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(VERSION_2);
    out.writeInt(mKvs.size());
    // We interleave the two lists for code simplicity
    for (KeyValue kv : mKvs) {
      kv.write(out);
    }
    if (mScopes == null) {
      out.writeInt(0);
    } else {
      out.writeInt(mScopes.size());
      for (byte[] key : mScopes.keySet()) {
        Bytes.writeByteArray(out, key);
        out.writeInt(mScopes.get(key));
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("[#edits: " + mKvs.size() + " = <");
    for (KeyValue kv : mKvs) {
      sb.append(kv.toString());
      sb.append("; ");
    }
    if (mScopes != null) {
      sb.append(" scopes: " + mScopes.toString());
    }
    sb.append(">]");
    return sb.toString();
  }

}

