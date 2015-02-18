package org.javenstudio.raptor.bigdb.util;

import org.javenstudio.raptor.bigdb.DBRegionInfo;
import org.javenstudio.raptor.io.DataInputBuffer;
import org.javenstudio.raptor.io.Writable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Utility class with methods for manipulating Writable objects
 */
public class Writables {
  /**
   * @param w writable
   * @return The bytes of <code>w</code> gotten by running its
   * {@link Writable#write(java.io.DataOutput)} method.
   * @throws IOException e
   * @see #getWritable(byte[], Writable)
   */
  public static byte [] getBytes(final Writable w) throws IOException {
    if (w == null) {
      throw new IllegalArgumentException("Writable cannot be null");
    }
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(byteStream);
    try {
      w.write(out);
      out.close();
      out = null;
      return byteStream.toByteArray();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * Set bytes into the passed Writable by calling its
   * {@link Writable#readFields(java.io.DataInput)}.
   * @param bytes serialized bytes
   * @param w An empty Writable (usually made by calling the null-arg
   * constructor).
   * @return The passed Writable after its readFields has been called fed
   * by the passed <code>bytes</code> array or IllegalArgumentException
   * if passed null or an empty <code>bytes</code> array.
   * @throws IOException e
   * @throws IllegalArgumentException
   */
  public static Writable getWritable(final byte [] bytes, final Writable w)
  throws IOException {
    return getWritable(bytes, 0, bytes.length, w);
  }

  /**
   * Set bytes into the passed Writable by calling its
   * {@link Writable#readFields(java.io.DataInput)}.
   * @param bytes serialized bytes
   * @param offset offset into array
   * @param length length of data
   * @param w An empty Writable (usually made by calling the null-arg
   * constructor).
   * @return The passed Writable after its readFields has been called fed
   * by the passed <code>bytes</code> array or IllegalArgumentException
   * if passed null or an empty <code>bytes</code> array.
   * @throws IOException e
   * @throws IllegalArgumentException
   */
  public static Writable getWritable(final byte [] bytes, final int offset,
    final int length, final Writable w)
  throws IOException {
    if (bytes == null || length <=0) {
      throw new IllegalArgumentException("Can't build a writable with empty " +
        "bytes array");
    }
    if (w == null) {
      throw new IllegalArgumentException("Writable cannot be null");
    }
    DataInputBuffer in = new DataInputBuffer();
    try {
      in.reset(bytes, offset, length);
      w.readFields(in);
      return w;
    } finally {
      in.close();
    }
  }

  /**
   * @param bytes serialized bytes
   * @return A DBRegionInfo instance built out of passed <code>bytes</code>.
   * @throws IOException e
   */
  public static DBRegionInfo getDBRegionInfo(final byte [] bytes)
  throws IOException {
    return (DBRegionInfo)getWritable(bytes, new DBRegionInfo());
  }

  /**
   * @param bytes serialized bytes
   * @return A DBRegionInfo instance built out of passed <code>bytes</code>
   * or <code>null</code> if passed bytes are null or an empty array.
   * @throws IOException e
   */
  public static DBRegionInfo getDBRegionInfoOrNull(final byte [] bytes)
  throws IOException {
    return (bytes == null || bytes.length <= 0)?
        null : getDBRegionInfo(bytes);
  }

  /**
   * Copy one Writable to another.  Copies bytes using data streams.
   * @param src Source Writable
   * @param tgt Target Writable
   * @return The target Writable.
   * @throws IOException e
   */
  public static Writable copyWritable(final Writable src, final Writable tgt)
  throws IOException {
    return copyWritable(getBytes(src), tgt);
  }

  /**
   * Copy one Writable to another.  Copies bytes using data streams.
   * @param bytes Source Writable
   * @param tgt Target Writable
   * @return The target Writable.
   * @throws IOException e
   */
  public static Writable copyWritable(final byte [] bytes, final Writable tgt)
  throws IOException {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
    try {
      tgt.readFields(dis);
    } finally {
      dis.close();
    }
    return tgt;
  }
}
