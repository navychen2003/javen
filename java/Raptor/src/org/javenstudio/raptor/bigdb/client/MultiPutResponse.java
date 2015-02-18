package org.javenstudio.raptor.bigdb.client;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.bigdb.util.Bytes;


/**
 * Response class for MultiPut.
 */
public class MultiPutResponse implements Writable {

  protected MultiPut request; // used in client code ONLY

  protected Map<byte[], Integer> answers = new TreeMap<byte[], Integer>(Bytes.BYTES_COMPARATOR);

  public MultiPutResponse() {}

  public void addResult(byte[] regionName, int result) {
    answers.put(regionName, result);
  }

  public Integer getAnswer(byte[] region) {
    return answers.get(region);
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(answers.size());
    for( Map.Entry<byte[],Integer> e : answers.entrySet()) {
      Bytes.writeByteArray(out, e.getKey());
      out.writeInt(e.getValue());
    }
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    answers.clear();

    int mapSize = in.readInt();
    for( int i = 0 ; i < mapSize ; i++ ) {
      byte[] key = Bytes.readByteArray(in);
      int value = in.readInt();

      answers.put(key, value);
    }
  }
}

