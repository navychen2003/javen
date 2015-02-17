package org.javenstudio.cocoka.storage.io;

/**
 * A {@link IWritable} which is also {@link Comparable}. 
 *
 * <p><code>IWritableComparable</code>s can be compared to each other, typically 
 * via <code>Comparator</code>s. Any type which is to be used as a 
 * <code>key</code> in the framework should implement this
 * interface.</p>
 *  
 * <p>Example:</p>
 * <p><blockquote><pre>
 *     public class MyWritableComparable implements IWritableComparable {
 *       // Some data
 *       private int counter;
 *       private long timestamp;
 *       
 *       public void write(DataOutput out) throws IOException {
 *         out.writeInt(counter);
 *         out.writeLong(timestamp);
 *       }
 *       
 *       public void readFields(DataInput in) throws IOException {
 *         counter = in.readInt();
 *         timestamp = in.readLong();
 *       }
 *       
 *       public int compareTo(MyWritableComparable w) {
 *         int thisValue = this.value;
 *         int thatValue = ((IntWritable)o).value;
 *         return (thisValue &lt; thatValue ? -1 : (thisValue==thatValue ? 0 : 1));
 *       }
 *     }
 * </pre></blockquote></p>
 */
public interface IWritableComparable<T> extends IWritable, Comparable<T> {
	
}