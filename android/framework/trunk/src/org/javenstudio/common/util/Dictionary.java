package org.javenstudio.common.util;

import java.io.IOException;
import java.io.InputStream; 
import java.io.ByteArrayInputStream; 
import java.io.ByteArrayOutputStream; 
import java.io.BufferedInputStream; 
import java.io.FileInputStream; 
import java.io.File; 
import java.io.RandomAccessFile;
import java.util.zip.InflaterInputStream; 

public class Dictionary {
  
  private static final boolean DEFAULT_CACHE_VALUES = false; 
  
  public static class Item {
    public byte[] key = null; 
    public byte[] value = null; 
    public byte[] data = null; 
    public int dataLength = 0; 
    public int offsetPart = 0; 
    public int offset = 0; 
  }
  
  private DoubleArrayTrie da = null;
  private int[] offsets = null; 
  private byte[] values = null; 
  private byte[] header = null; 
  private long valuesOffset = 0; 
  private int valuesSize = 0; 
  private int indexValue = 0; 
  private long offsetData = 0; 
  private int size = 0; 
  private File file = null;
  private RandomAccessFile accessFile = null; 
  private boolean cacheValues = DEFAULT_CACHE_VALUES; 

  public Dictionary(String filename) {
    this(filename, DEFAULT_CACHE_VALUES); 
  }
  
  public Dictionary(String filename, boolean cacheValues) {
    this(new File(filename), cacheValues); 
  }
  
  public Dictionary(File file) {
    this(file, DEFAULT_CACHE_VALUES); 
  }
  
  public Dictionary(File file, boolean cacheValues) {
    reset(); 
    this.file = file; 
    this.cacheValues = cacheValues; 
  }
  
  private void reset() {
    this.size = 0; 
    this.da = null; 
    this.offsets = null; 
    this.values = null; 
    this.header = null; 
    this.offsetData = 0; 
    this.indexValue = 0; 
    this.valuesOffset = 0; 
    this.valuesSize = 0; 
  }
  
  public byte[] getHeader() throws IOException {
    ensureOpen(false); 
    return this.header; 
  }
  
  private void closeFiles() throws IOException {
    if (accessFile != null) {
      try {
        accessFile.close(); 
      } finally {
        accessFile = null; 
      }
    }
  }
  
  private RandomAccessFile getRandomAccessFile() throws IOException {
    synchronized (this) {
      if (accessFile == null) {
        accessFile = new RandomAccessFile(file, "r"); 
      }
      
      return accessFile; 
    }
  }
  
  private void checkHeader(InputStream is) throws IOException {
    byte[] tmp = new byte[3]; 
    if (is.read(tmp, 0, 3) != 3) 
      throw new IOException("wrong dictionary header size"); 
    
    if (tmp[0] != 'D' && tmp[1] != 'I' && tmp[2] != 'C') 
      throw new IOException("wrong dictionary header"); 
    
    int headerSize = DoubleArrayTrie.readVInt(is); 
    if (headerSize > 0) {
      this.header = new byte[headerSize]; 
      is.read(this.header, 0, headerSize); 
    }
  }
  
  private int readSize(InputStream is) throws IOException {
    byte[] buf = new byte[4]; 
    if (is.read(buf, 0, 4) != 4) 
      throw new IOException("wrong dictionary data"); 
    
    int datasize = DictionaryMaker.bytes2Int(buf); 
    if (datasize < 0) 
      throw new IOException("wrong dictionary data size: "+datasize); 
    
    return datasize; 
  }
  
  private static class OffsetInputStream extends InputStream {
    private InputStream in = null; 
    private long offset = 0; 
    
    public OffsetInputStream(InputStream in) {
      this.in = in; 
    }
    
    public long offset() { return offset; }
    
    public int read() throws IOException {
      int b = in.read(); 
      if (b != -1) offset += 1; 
      return b; 
    }
    
    public long skip(long n) throws IOException {
      long num = in.skip(n); 
      if (num > 0) offset += num; 
      return num; 
    }
    
    public int available() throws IOException { return in.available(); }
    public void close() throws IOException { in.close(); }
    public void reset() throws IOException { in.reset(); }
    public void mark(int readlimit) { in.mark(readlimit); }
    public boolean markSupported() { return in.markSupported(); }
  }
  
  private void ensureOpen() throws IOException {
	  ensureOpen(true); 
  }
  
  private void ensureOpen(boolean loadIndexValues) throws IOException {
    if (da != null) return; 
    
    synchronized (this) {
      if (file == null) 
        return; 
      
      FileInputStream fis = new FileInputStream(file); 
      try {
        OffsetInputStream in = new OffsetInputStream(new BufferedInputStream(fis)); 
        
        checkHeader(in); 
        
        if (loadIndexValues) {
	        da = new DoubleArrayTrie(); 
	        da.load(in); 
	        
	        size = da.size(); 
	        
	        int offsetsize = readSize(in); 
	        //if (offsetsize != size) 
	        //  throw new IOException("offsets data size wrong: "+offsetsize+" != "+size); 
	        
	        if (offsetsize > 0) {
	          offsets = new int[offsetsize]; 
	          for (int i=0; i < offsets.length; i++) {
	            offsets[i] = DoubleArrayTrie.readVInt(in); 
	          }
	        } else if (offsetsize < 0) 
	          throw new IOException("offsets data size error: "+offsetsize); 
	        
	        int datasize = readSize(in); 
	        valuesSize = datasize; 
	        valuesOffset = in.offset(); 
	        
	        if (datasize > 0) {
	          if (cacheValues) {
  	          values = new byte[datasize]; 
  	          if (in.read(values, 0, datasize) != datasize) 
  	            throw new IOException("read dictionary data error"); 
  	        
  	        } else {
  	          byte[] tmp = new byte[10240]; 
  	          int leftsize = datasize; 
  	          while (leftsize > 0) {
  	            int readsize = leftsize > tmp.length ? tmp.length : leftsize; 
  	            if ((readsize = in.read(tmp, 0, readsize)) < 0) 
  	              throw new IOException("read dictionary data error"); 
  	            
  	            leftsize -= readsize; 
  	          }
  	        }
	          
	          long offset = valuesOffset + valuesSize; //in.offset(); 
	          if (offset <= 0) 
	            throw new IOException("dictionary data offset error"); 
	          
	          offsetData = offset; 
	          
	        } else if (datasize < 0) 
	          throw new IOException("dictionary data size error: "+datasize); 
        }
        
      } finally {
        try {
          fis.close(); 
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }
  
  private byte[] getValue(int offset) throws IOException {
    if (values != null) 
      return getValueFromCache(offset); 
    else
      return getValueFromFile(offset); 
  }
  
  private byte[] getValueFromFile(int offset) throws IOException {
    if (offset < 0 || offset + 4 >= valuesSize) 
      return null; 
    
    RandomAccessFile raf = getRandomAccessFile(); 
    
    long offsetStart = valuesOffset + offset; 
    raf.seek(offsetStart); 
    
    int ret = 0; 
    byte[] tmpsize = new byte[4]; 
    if ((ret = raf.read(tmpsize, 0, tmpsize.length)) != tmpsize.length) 
      throw new IOException("read value length error: "+ret); 
    
    int len = DictionaryMaker.bytes2Int(tmpsize); 
    if (len < 0) 
      throw new IOException("read value length wrong: "+len); 
    
    byte[] tmp = new byte[len]; 
    if ((ret = raf.read(tmp, 0, tmp.length)) != tmp.length) 
      throw new IOException("read value error: "+ret); 
    
    return tmp; 
  }
  
  private byte[] getValueFromCache(int offset) throws IOException {
    if (values == null || offset + 4 >= values.length) 
      return null; 
    
    int i = 0; 
    
    byte[] lenbuf = new byte[4]; 
    lenbuf[0] = values[i+offset]; i++; 
    lenbuf[1] = values[i+offset]; i++; 
    lenbuf[2] = values[i+offset]; i++; 
    lenbuf[3] = values[i+offset]; i++; 
    
    int len = DictionaryMaker.bytes2Int(lenbuf, 0); 
    if (len < 0) 
      throw new IOException("wrong data length: "+len+" at offset: "+offset); 
    
    byte[] buf = new byte[len]; 
    for (int j=0; j < buf.length; j++) {
      buf[j] = values[i+j+offset]; 
    }
    
    return buf; 
  }

  private Item fetchItem(byte[] value, boolean fetchData) throws IOException {
    if (value == null || value.length == 0) 
      return null; 
    
    Item item = new Item(); 
    
    ByteArrayInputStream bais = new ByteArrayInputStream(value); 
    
    int len = DoubleArrayTrie.readVInt(bais); 
    if (len <= 0) 
      throw new IOException("wrong key data length: "+len); 
    
    item.key = new byte[len]; 
    int num = bais.read(item.key, 0, len); 
    
    int flag = bais.read(); 
    if (flag == 0) {
      // value is null
      
      fetchData = false; 
      
    } else if (flag == 1) {
      len = DoubleArrayTrie.readVInt(bais); 
      if (len <= 0) 
        throw new IOException("wrong value data length: "+len); 
      
      item.value = new byte[len]; 
      bais.read(item.value, 0, len); 
      
      fetchData = false; 
      
    } else if (flag == 2) {
      len = DoubleArrayTrie.readVInt(bais); 
      if (len <= 0) 
        throw new IOException("wrong value data length: "+len); 
      
      item.value = new byte[len]; 
      bais.read(item.value, 0, len); 
      
      item.offsetPart = DoubleArrayTrie.readVInt(bais); 
      item.offset = DoubleArrayTrie.readVInt(bais); 
      
    } else if (flag == 3) {
      item.offsetPart = DoubleArrayTrie.readVInt(bais); 
      item.offset = DoubleArrayTrie.readVInt(bais); 
      
    } else 
      throw new IOException("wrong key data flag "+flag); 
    
    if (item.offsetPart < 0) 
      throw new IOException("wrong value part offset "+item.offsetPart); 
      
    if (item.offset < 0) 
      throw new IOException("wrong value offset "+item.offset); 
    
    byte[] databuf = null; 
    
    if (fetchData) {
      RandomAccessFile raf = getRandomAccessFile(); 
      
      long offset = offsetData + item.offsetPart; 
      raf.seek(offset); 
      
      int ret = 0; 
      byte[] tmpsize = new byte[5]; 
      if ((ret = raf.read(tmpsize, 0, tmpsize.length)) != tmpsize.length) 
        throw new IOException("read data part length error: "+ret); 
      
      len = DictionaryMaker.bytes2Int(tmpsize) - 1; 
      if (len < 0) 
        throw new IOException("read data part length wrong: "+len); 
      
      byte[] tmp = new byte[len]; 
      if ((ret = raf.read(tmp, 0, tmp.length)) != tmp.length) 
        throw new IOException("read data part error: "+ret); 
      
      ByteArrayInputStream bis = new ByteArrayInputStream(tmp); 
      InflaterInputStream dis = new InflaterInputStream(bis); 
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      baos.write((byte)tmpsize[4]); 
      
      byte[] buf = new byte[10240]; 
      while (true) {
        num = dis.read(buf); 
        if (num < 0) break; 
        if (num > 0) baos.write(buf, 0, num); 
      }
      
      baos.flush(); 
      databuf = baos.toByteArray(); 
      
      try {
        bais.close(); 
      } catch (Exception e) {
        // ignore
      }
    }
    
    if (databuf != null && databuf.length > 0) {
      item.dataLength = DictionaryMaker.bytes2Int(databuf, item.offset); 
      if (item.dataLength < 0) 
        throw new IOException("data length error: "+item.dataLength); 
      
      item.data = new byte[item.dataLength]; 
      
      System.arraycopy(databuf, item.offset+4, item.data, 0, item.dataLength); 
    }
    
    return item; 
  }

  public boolean close() throws IOException {
    synchronized (this) {
      reset(); 
      
      closeFiles(); 
    }
    
    return true;
  }
  
  public int size() throws IOException {
    ensureOpen(); 
    return size; 
  }
  
  public byte[] get(byte[] key) throws IOException {
    ensureOpen(); 
    
    synchronized (this) {
      if (da == null || key == null) 
        return null; 
      
      int val = da.search(key); 
      if (val > 0) 
        return getValue(val); 
      
      return null; 
    }
  }
  
  public Item getItem(byte[] key) throws IOException {
    return getItem(key, false); 
  }
  
  public Item getItem(byte[] key, boolean fetchData) throws IOException {
    return fetchItem(get(key), fetchData); 
  }
  
  public int getItemCount() throws IOException {
    ensureOpen(); 
    return offsets != null ? offsets.length : 0; 
  }
  
  public void firstItem() { 
    synchronized (this) {
      this.indexValue = 0; 
    }
  }
  
  public boolean movetoItem(int index) { 
    synchronized (this) {
      if (offsets != null && index >= 0 && index < offsets.length) {
        this.indexValue = index; 
        return true; 
      }
      return false; 
    }
  }
  
  public boolean lastItem() { 
    synchronized (this) {
      this.indexValue = offsets != null ? offsets.length -1 : 0; 

      return indexValue >= 0 ? true : false; 
    }
  }
  
  public Item getItem(int index) throws IOException {
    return getItem(index, false); 
  }
  
  public Item getItem(int index, boolean fetchData) throws IOException {
    return nextItem0(index, fetchData, false); 
  }
  
  public Item nextItem() throws IOException {
    return nextItem(false); 
  }
  
  public Item nextItem(boolean fetchData) throws IOException {
    return nextItem0(indexValue, fetchData, true); 
  }
  
  private int getPositionValue(int indexValue) {
    if (offsets != null && indexValue >= 0 && indexValue < offsets.length) 
      return offsets[indexValue]; 
    else
      return -1; 
  }
  
  private Item nextItem0(int index, boolean fetchData, boolean moveNext) throws IOException {
    ensureOpen(); 
    
    synchronized (this) {
      if (da == null) 
        return null; 
    
      if (index < 0) index = 0; 
      
      int positionValue = getPositionValue(index); 
      if (positionValue < 0) 
        return null; 
      
      byte[] value = getValue(positionValue); 

      if (value == null) 
        return null; 
      
      if (moveNext) {
        //positionValue += value.length + 4; 
        indexValue = index + 1; 
      }
      
      return fetchItem(value, fetchData); 
    }
  }

  public static void main(String[] args) throws IOException {
    String key = "china"; 
    String filename = "oxford.dict.dat"; 
    boolean listall = false; 
    
    for (int i=0; args != null && i < args.length; i++) {
      String arg = args[i]; 
      if ("-f".equals(arg)) {
        filename = args[++i]; 
      } else if ("-list".equals(arg)) {
        listall = true; 
      } else 
        key = arg; 
    }
    
    Dictionary dict = new Dictionary(filename); 
    dict.firstItem(); 
    
    while (true) {
      Item item = null; 
      if (listall == false) 
        item = dict.getItem(key.getBytes(), true); 
      else 
        item = dict.nextItem(true); 
      
      if (item != null) {
        System.out.println(" key: "+new String(item.key, "UTF-8")); 
        //System.out.println(" "+item.offsetPart); 
        //System.out.println(" "+item.offset); 
        //System.out.println(" "+item.dataLength); 
        if (item.value != null) 
          System.out.println(" value: "+new String(item.value, "UTF-8")); 
        if (item.data != null) 
          System.out.println(" data: "+new String(item.data, "UTF-8")); 
        System.out.println(); 
      }
      
      if (listall == false || item == null) 
        break; 
    }
    
    dict.close(); 
  }
}