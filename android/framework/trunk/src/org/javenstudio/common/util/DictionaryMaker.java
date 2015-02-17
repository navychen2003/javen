package org.javenstudio.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream; 
import java.io.BufferedOutputStream; 
import java.io.ByteArrayOutputStream; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.io.File; 
import java.util.ArrayList; 
import java.util.Collections; 
import java.util.Comparator; 
import java.util.zip.DeflaterOutputStream; 

public class DictionaryMaker {
  private static class ItemKey {
    public byte[] key = null; 
    public int offset = 0; 
    
    public ItemKey(byte[] key, int offset) {
      this.key = key; 
      this.offset = offset; 
    }
  }
  
  private ArrayList<ItemKey> items = new ArrayList<ItemKey>(); 
  private ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
  private ByteArrayOutputStream tmpbuf = null; 
  private DeflaterOutputStream tmpbufout = null; 
  private OutputStream tmpout = null; 
  private int tmpbuf_length = 0; 
  private int tmpout_length = 0; 
  private File file = null; 
  private File tmpfile = null; 
  private byte[] header = null; 
  private boolean finished = false; 

  public DictionaryMaker(String filename) {
    this(new File(filename)); 
  }
  
  public DictionaryMaker(File file) {
    this.file = file; 
  }
  
  public void setHeader(byte[] buf) {
    this.header = buf; 
  }
  
  public static int bytes2Int(byte[] b) {
    return bytes2Int(b, 0); 
  }
  
  public static int bytes2Int(byte[] b, int offset) {
    int mask = 0xff;
    int temp = 0;
    int res = 0;
    for (int i=0; i<4; i++) {
      res <<= 8;
      temp = b[i+offset]&mask;
      res |= temp;
    }
    return res;
  } 
  
  public static byte[] int2Bytes(int num) {
    byte[] b = new byte[4];
    //int mask = 0xff;
    for (int i=0; i<4; i++) {
      b[i] = (byte)(num>>>(24-i*8));
    }
    return b;
  }
  
  public static int compareItem(ItemKey o1, ItemKey o2) {
    if (o1 == null) {
      return o2 == null ? 0 : -1; 
    } else if (o2 == null) {
      return o1 == null ? 0 : 1; 
    } else {
      return compareBytes(o1.key, o2.key); 
    }
  }
  
  public static int compareBytes(byte[] b1, byte[] b2) {
    if (b1 == null) {
      return b2 == null ? 0 : -1; 
    } else if (b2 == null) {
      return b1 == null ? 0 : 1; 
    } else {
      for (int i=0; i < b1.length && i < b2.length; i++) {
        int c1 = b1[i] &0xff; 
        int c2 = b2[i] &0xff; 
        if (c1 < c2) return -1; 
        else if (c1 > c2) return 1; 
      }
      if (b1.length > b2.length) 
          return 1; 
      else if (b1.length < b2.length) 
          return -1; 
    }
    return 0; 
  }
  
  private DeflaterOutputStream getOutputStream() throws IOException {
    synchronized (this) {
      flushTempBuffer(false); 
      
      if (tmpout == null) {
        tmpfile = new File(file.getAbsolutePath() + ".tmp"); 
        
        FileOutputStream fos = new FileOutputStream(tmpfile); 
        BufferedOutputStream bos = new BufferedOutputStream(fos); 
        
        bos.write(new byte[]{'D','A','T'}, 0, 3); 
        
        tmpout = bos; 
        tmpout_length = 3; 
      }
      
      if (tmpbuf == null) {
        tmpbuf = new ByteArrayOutputStream(); 
        
        tmpbuf.write(new byte[]{'Z'}, 0, 1); 
        tmpbuf_length = 1; 
      }
      
      if (tmpbufout == null) {
        DeflaterOutputStream dos = new DeflaterOutputStream(tmpbuf); 
        
        tmpbufout = dos; 
      }
      
      return tmpbufout; 
    }
  }
  
  private void flushTempBuffer(boolean force) throws IOException {
    if (tmpbuf != null) {
      if (force || tmpbuf_length >= 1024 * 1024) {
        if (tmpout != null) {
          synchronized (this) {
            if (tmpbufout != null) tmpbufout.finish(); 
            tmpbufout.flush(); 
            
            byte[] tmp = tmpbuf.toByteArray(); 
            byte[] lenBytes = int2Bytes(tmp.length); 
            
            tmpout.write(lenBytes, 0, lenBytes.length); 
            tmpout_length += lenBytes.length; 
            
            tmpout.write(tmp, 0, tmp.length); 
            tmpout_length += tmp.length; 

            tmpbuf = null; 
            tmpbufout = null; 
          }
        }
      }
    }
  }
  
  private void appendValue(OutputStream baos, byte[] value, int offset) throws IOException {
    int len = value.length - offset; 
    
    int offset1 = 0, offset2 = 0; 
    
    if (len > 0) {
      OutputStream os = getOutputStream(); 
      
      offset1 = tmpout_length; 
      offset2 = tmpbuf_length; 
      
      byte[] lenBytes = int2Bytes(len); 
      
      os.write(lenBytes, 0, lenBytes.length); 
      tmpbuf_length += lenBytes.length; 
      
      os.write(value, offset, len); 
      tmpbuf_length += len; 
    }
    
    DoubleArrayTrie.writeVInt(baos, offset1); 
    DoubleArrayTrie.writeVInt(baos, offset2); 
  }
  
  private byte[] appendData(byte[] key, byte[] value, int headsize) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    
    DoubleArrayTrie.writeVInt(baos, key.length); 
    baos.write(key, 0, key.length); 
    
    if (value == null || value.length == 0) {
      baos.write((byte)0); 
      
    } else if (value.length <= 8 || value.length <= headsize) {
      baos.write((byte)1); 
      
      DoubleArrayTrie.writeVInt(baos, value.length); 
      
      baos.write(value, 0, value.length); 
      
    } else if (headsize > 0 && headsize < value.length) {
      baos.write((byte)2); 
      
      DoubleArrayTrie.writeVInt(baos, headsize); 
      
      baos.write(value, 0, headsize); 
      
      appendValue(baos, value, headsize); 
      
    } else {
      baos.write((byte)3); 
      
      appendValue(baos, value, 0); 
    }
    
    return baos.toByteArray(); 
  }
  
  public boolean add(byte[] key, byte[] value) throws IOException {
    return add(key, value, 0); 
  }
  
  public boolean add(byte[] key, byte[] value, int headsize) throws IOException {
    if (key == null || key.length == 0) 
      return false; 
    
    synchronized (this) {
      if (finished) 
        throw new IOException("already finished"); 
      
      if (buffer.size() <= 0) 
        buffer.write(new byte[]{'K','E','Y'}, 0, 3); 
      
      value = appendData(key, value, headsize); 
      
      int offset = buffer.size(); 
      
      byte[] lenBytes = int2Bytes(value.length); 
      buffer.write(lenBytes, 0, lenBytes.length); 
      
      buffer.write(value, 0, value.length); 
      
      items.add(new ItemKey(key, offset)); 
      
      return true; 
    }
  }
  
  public void finish() throws IOException {
    synchronized (this) {
      if (finished) 
        throw new IOException("already finished"); 
      
      try {
        _build(); 
      } finally {
        finished = true; 
      }
    }
  }
  
  private void writeInts(OutputStream bos, int[] data) throws IOException {
    byte[] datasize = int2Bytes(data != null ? data.length : 0); 
    bos.write(datasize); 
    
    if (data != null && data.length > 0) {
      for (int i=0; i < data.length; i++) {
        DoubleArrayTrie.writeVInt(bos, data[i]); 
      }
    }
  }
  
  private void _build() throws IOException {
    if (tmpout != null) {
      flushTempBuffer(true); 
      
      tmpout.flush(); 
      tmpout.close(); 
      tmpout = null; 
    }
    
    DoubleArrayTrie da = new DoubleArrayTrie();
    int[] offsets = null; 
    
    if (items.size() > 0) {
      Collections.sort(items, new Comparator<ItemKey>() {
        public int compare(ItemKey o1, ItemKey o2) {
          return compareItem(o1, o2); 
        }
      }); 
      
      byte[][] keys = new byte[items.size()][]; 
      int[] vals = new int[items.size()]; 
      
      for (int i=0; i < items.size(); i++) {
        ItemKey item = items.get(i); 
        keys[i] = item.key; 
        vals[i] = item.offset; 
      }
      
      da.build(keys, vals); 
      
      offsets = vals; 
    }
    
    FileOutputStream fos = new FileOutputStream(file); 
    BufferedOutputStream bos = new BufferedOutputStream(fos); 
    
    writeHeader(bos); 
    
    da.saveTo(bos); 
    
    writeInts(bos, offsets); 
    
    byte[] datasize = int2Bytes(buffer.size()); 
    bos.write(datasize); 
    
    buffer.writeTo(bos); 
    
    if (tmpfile != null && tmpfile.exists()) {
      FileInputStream fis = new FileInputStream(tmpfile); 
      BufferedInputStream bis = new BufferedInputStream(fis); 
      
      byte[] tmp = new byte[10240]; 
      while (true) {
        int len = bis.read(tmp); 
        if (len < 0) break; 
        if (len > 0) {
          bos.write(tmp, 0, len); 
        }
      }
      
      try {
        fis.close(); 
      } catch (Exception e) {
        // ignore
      }
      
      try {
        tmpfile.delete(); 
      } catch (Exception e) {
        // ignore
      }
    }
    
    bos.flush(); 
    bos.close(); 
    fos.close(); 
  }
  
  private void writeHeader(OutputStream out) throws IOException {
    out.write(new byte[]{'D', 'I', 'C'}, 0, 3); 
    
    if (header == null || header.length == 0) {
      DoubleArrayTrie.writeVInt(out, 0); 
    } else {
      DoubleArrayTrie.writeVInt(out, header.length); 
      out.write(header, 0, header.length); 
    }
  }
  
  static String parseKey(String str) {
    if (str == null || str.length() == 0)
      return str; 
    
    StringBuffer sbuf = new StringBuffer(); 
    boolean flag = false; 
    for (int i=0; i < str.length(); i++) {
      char chr = str.charAt(i); 
      switch (chr) {
        case ' ': 
        case '\t': 
        case '\r': 
        case '\n': 
          if (flag == false) 
            sbuf.append(' '); 
          flag = true; 
          break; 
        
        default: 
          sbuf.append(chr); 
          flag = false; 
          break; 
      }
    }
    
    return sbuf.toString(); 
  }
  
  static String parseValue(String str) {
    if (str == null || str.length() == 0)
      return str; 
    
    StringBuffer sbuf = new StringBuffer(); 
    boolean flag = false; 
    for (int i=0; i < str.length(); i++) {
      char chr = str.charAt(i); 
      if (flag) {
        switch (chr) {
          case 'n': 
            sbuf.append('\n'); 
            break; 
          case 'r': 
            sbuf.append('\r'); 
            break; 
          case 't': 
            sbuf.append('\t'); 
            break; 
            
          default: 
            sbuf.append(chr); 
            break; 
        }
        flag = false; 
        continue; 
      }
      switch (chr) {
        case '\\': 
          flag = true; 
          break; 
        
        default: 
          sbuf.append(chr); 
          flag = false; 
          break; 
      }
    }
    
    return sbuf.toString(); 
  }
  
  private static void appendData(DictionaryMaker maker, String key, String val) throws IOException {
    if (key == null || val == null || key.length() == 0) 
      return; 
    
    byte[] keyBytes = key.getBytes("UTF-8"); 
    byte[] valBytes = val.getBytes("UTF-8"); 
    
    maker.add(keyBytes, valBytes); 
  }
  
  public static void buildDictionary(String inputfile, String outputfile) throws IOException {
    if (inputfile == null || outputfile == null) 
    	throw new IOException("input or output filename wrong"); 
	  
    DictionaryMaker maker = new DictionaryMaker(outputfile); 
    
    java.io.FileInputStream fis = new java.io.FileInputStream(inputfile); 
    
    buildDictionary(maker, fis); 
  }
  
  public static void buildDictionary(DictionaryMaker maker, InputStream fis) throws IOException {
	maker.build(fis); 
  }
  
  public void build(InputStream fis) throws IOException {
    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(fis, "UTF-8")); 
    
    String line = null, key = null, val = null; 
    while ((line = br.readLine()) != null) {
      int pos = line.indexOf('\t'); 
      if (pos > 0) {
        appendData(this, key, val); 
        
        key = parseKey(line.substring(0, pos)); 
        val = parseValue(line.substring(pos+1)); 
        
      } else {
        if (key != null) {
          String tmp = pos < 0 ? line : line.substring(pos+1); 
          tmp = parseValue(tmp); 
          if (tmp != null) {
            if (val == null) val = ""; 
            val += tmp; 
          }
        }
      }
    }
    appendData(this, key, val); 
    
    try { fis.close(); } catch (Exception ignore) {}
    
    this.finish(); 
  }
  
  public static void main(String[] args) throws IOException {
    if (args == null || args.length != 1) {
      System.out.println("Usage: java DictionaryMaker <filename>"); 
      return; 
    }
    
    String filename = args != null && args.length > 0 ? args[0] : "oxford.dict"; 
    
    buildDictionary(filename, filename+".dat"); 
  }
}