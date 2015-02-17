package org.javenstudio.common.util;

import java.io.IOException;
import java.io.InputStream; 
import java.io.OutputStream; 
import java.util.ArrayList;

public class DoubleArrayTrie {

  //private final static int BUF_SIZE = 500000;
  private final static int DEFAULT_SIZE = 1024 * 10; 
  private final static float INCREMENT_RATE = 1.10f; 

  private int array[];
  private int used[];
  private int size;

  private int alloc_size;
  private byte str[][];
  private int str_size;
  private int val[];

  private int next_check_pos;
  @SuppressWarnings("unused")
  private int no_delete;

  private class Node {
    int code;
    int depth;
    int left;
    int right;
  };

  public DoubleArrayTrie() {
    array = null;
    used = null;
    size = 0;
    alloc_size = 0;
    no_delete = 0;
  }

  private int[] _resize(int ptr[], int n, int l, int v) {
    int tmp[] = new int[l];
    if (ptr != null) {
      l = ptr.length;
    } else {
      l = 0;
    }

    for (int i = 0; i < l; i++) tmp[i] = ptr[i];
    for (int i = l; i < l; i++) tmp[i] = v;
    ptr = null;
    
    return tmp;
  }

  private int resize(int new_size) {
    array = _resize(array, alloc_size << 1, new_size << 1, (int)0);
    used  = _resize(used, alloc_size, new_size, (int)0);

    alloc_size = new_size;

    return new_size;
  }

  private int fetch(Node parent, ArrayList<Node> siblings) {
    int prev = 0;

    for (int i = parent.left; i < parent.right; i++) {
      if (str[i].length < parent.depth) 
        continue;

      byte tmp[] = str[i];

      int cur = 0;
      if (str[i].length != parent.depth) {
        cur = (int)(tmp[parent.depth]&0xff) + 1;
      }

      if (prev > cur) {
        throw new RuntimeException("Fatal: given strings are not sorted(prev=" 
        		+ prev + " > cur=" + cur + ")."); 
      }

      if (cur != prev || siblings.size() == 0) {
        Node tmp_node = new Node();
        tmp_node.depth = parent.depth + 1;
        tmp_node.code = cur;
        tmp_node.left = i;
        if (siblings.size() != 0)
          ((Node)siblings.get(siblings.size()-1)).right = i;

        siblings.add(tmp_node);
      }

      prev = cur;
    }

    if (siblings.size() != 0)
      ((Node)siblings.get(siblings.size()-1)).right = parent.right;

    return siblings.size();
  }

  private int insert(ArrayList<Node> siblings) {
    int begin = 0;
    int pos = (((((Node)siblings.get(0)).code + 1) > ((int)next_check_pos)) ? 
      (((Node)siblings.get(0)).code + 1) : ((int)next_check_pos)) - 1;

    int nonzero_num = 0;
    int first = 0;

    while (true) {
      pos ++;
      { 
        int t = (int)(pos); 
        if (t > alloc_size) { 
          resize((int)(t*INCREMENT_RATE)); 
        }
      }

      if (array[(((int)pos) << 1) + 1] != 0) {
        nonzero_num ++;
        continue;
      } else if (first == 0) {
        next_check_pos = pos;
        first = 1;
      }

      begin = pos - ((Node)siblings.get(0)).code;

      { 
        int t = (int)(begin + ((Node)siblings.get(siblings.size()-1)).code); 
        if (t > alloc_size) { 
          resize((int)(t*INCREMENT_RATE)); 
        }
      }

      if (used[begin] != 0) continue;

      boolean flag = false;

      for (int i = 1; i < siblings.size(); i++) {
        int npos = (((int)begin + ((Node)siblings.get(i)).code) << 1) + 1; 
        
        if (npos > alloc_size) { 
          resize((int)(npos*INCREMENT_RATE)); 
        }
        
        if (array[npos] != 0) {
          flag = true;
          break;
        }
      }
      
      if (!flag) break;
    }

    if (1.0 * nonzero_num/(pos - next_check_pos + 1) >= 0.95) 
      next_check_pos = pos;
    
    used[begin] = 1;
    size = (((size)>((int)begin + ((Node)siblings.get(siblings.size()-1)).code + 1)) ? 
      (size) : ((int)begin + ((Node)siblings.get(siblings.size()-1)).code + 1));

    for (int i = 0; i < siblings.size(); i++) {
      array[(((int)begin + ((Node)siblings.get(i)).code) << 1) + 1] = begin;
    }

    for (int i = 0; i < siblings.size(); i++) {
      ArrayList<Node> new_siblings = new ArrayList<Node>();

      if (fetch(((Node)siblings.get(i)), new_siblings) == 0) {
        array[((int)begin + (int)((Node)siblings.get(i)).code) << 1] =
          (val != null) ?
          (int)(-val[((Node)siblings.get(i)).left]-1) : 
          (int)(-((Node)siblings.get(i)).left-1);

        if ((val != null) && ((int)(-val[((Node)siblings.get(i)).left]-1) >= 0)) {
          throw new RuntimeException("Fatal: negative value is assgined.");
        }

      } else {
        int ins = (int)insert(new_siblings);
        array[((int)begin + ((Node)siblings.get(i)).code) << 1] = ins;
      }
    }

    return begin;
  }

  public void clear() {
    array = null;
    used = null;
    alloc_size = 0;
    size = 0;
    no_delete = 0;
  }

  public int size() { 
    return size; 
  }

  public int build(byte _str[][], int _val[]) {
    return build(_str, _val, _str.length);
  }

  public int build(byte _str[][], int _val[], int size) {
    if (_str == null || _str.length != _val.length) 
      return 0;

    str = _str;
    str_size = size;
    val = _val;

    resize(DEFAULT_SIZE);

    array[((int)0) << 1] = 1;
    next_check_pos = 0;

    Node root_node = new Node();
    root_node.left = 0;
    root_node.right = str_size;
    root_node.depth = 0;

    ArrayList<Node> siblings = new ArrayList<Node>();
    fetch(root_node, siblings);
    insert(siblings);

    used = null;

    return size;
  }

  public int search(byte key[]) {
    return search(key, 0, 0); 
  }
  
  public int search(byte key[], int pos) {
    return search(key, pos, 0); 
  }
  
  public int search(byte key[], int pos, int len) {
    if (array == null || array.length == 0) 
      return -1; 
    
    if (len <= 0) len = key.length;

    int b = array[((int)0) << 1];
    int p;
    for (int i = pos; i < len; i++) {
      p = b + (int)(key[i]&0xff) + 1;
      if ((int)b == array[(((int)p) << 1) + 1]) 
        b = array[((int)p) << 1];
      else 
        return -1;
    }

    p = b;
    int n = array[((int)p) << 1];

    if ((int)b == array[(((int)p) << 1) + 1] && n < 0) 
      return (int)(-n-1);
    
    return -1;
  }
  
  public int commonPrefixSearch(byte key[], int result[]) {
    return commonPrefixSearch(key, result, 0, 0); 
  }
  
  public int commonPrefixSearch(byte key[], int result[], int pos) {
    return commonPrefixSearch(key, result, pos, 0); 
  }
  
  public int commonPrefixSearch(byte key[], int result[], int pos, int len) {
    if (array == null || array.length == 0) 
      return -1; 
    
    if (len <= 0) len = key.length;

    int b = array[((int)0) << 1];
    int num = 0;
    int n = 0;
    int p = 0;

    for (int i = pos; i < len; i++) {
      p = b;
      n = array[((int)p) << 1];
      
      if ((int) b == array[(((int)p) << 1) + 1] && n < 0) {
        if (num < result.length) {
          result[num] = -n-1;
        }
        num++;
      }

      p = b + (int)(key[i]&0xff) + 1;

      if ( (p<<1) > array.length) {
        return num;
      }

      if ((int) b == array[(((int)p) << 1) + 1]) {
        b = array[((int)p) << 1];
      } else {
        return num;
      }
      
    }

    p = b;
    n = array[((int)p) << 1];
    
    if ((int)b == array[(((int)p) << 1) + 1] && n < 0) {
      if (num < result.length) {
        result[num] = -n-1;
      }
      num++;
    }

    return num;
  }

  public int prefixSearch(byte key[], int result[]) {
    return prefixSearch(key, result, 0, 0); 
  }
  
  public int prefixSearch(byte key[], int result[], int pos) {
    return prefixSearch(key, result, pos, 0); 
  }
  
  // not implement yet
  public int prefixSearch(byte key[], int result[], int pos, int len) {
    if (array == null || array.length == 0) 
      return -1; 
    
    if (len <= 0) len = key.length;

    int b = array[((int)0) << 1];
    int num = 0;
    int n = 0;
    int p = 0;

    for (int i = pos; i < len; i++) {
      p = b;
      n = array[((int)p) << 1];
      
      //if ((int) b == array[(((int)p) << 1) + 1] && n < 0) {
      //  if (num < result.length) {
      //    result[num] = -n-1;
      //  }
      //  num++;
      //}

      p = b + (int)(key[i]&0xff) + 1;

      if ( (p<<1) > array.length) {
        return num;
      }

      if ((int) b == array[(((int)p) << 1) + 1]) {
        b = array[((int)p) << 1];
      } else {
        return num;
      }
      
    }

    p = b;
    n = array[((int)p) << 1];
    
    if ((int)b == array[(((int)p) << 1) + 1] && n < 0) {
      if (num < result.length) {
        result[num] = -n-1;
      }
      num++;
    }

    return num;
  }

  public void load(InputStream in) throws IOException {
    try {
      int len = size = readVInt(in); 
      array = new int[(int)(len)];
      int zeroCount = 0; 
      for (int i = 0 ; i < array.length ; i++) {
        int val = 0; 
        if (zeroCount > 0) {
          zeroCount --; 
        } else {
          val = readVInt(in); 
          if (val == 0) {
            zeroCount = readVInt(in); 
            zeroCount --; 
          }
        }
        array[i] = val; 
      }
    } finally {
      //if (in != null)
      //  in.close();
    }
  }

  public void saveTo(OutputStream out) throws IOException {
    try {
      int dsize = alloc_size << 1;
      writeVInt(out, dsize); 
      int zeroCount = 0; 
      for (int i=0; i < dsize; i++) {
        int val = array[i]; 
        if (val == 0) {
          zeroCount ++; 
        } else {
          if (zeroCount > 0) {
            writeVInt(out, 0); 
            writeVInt(out, zeroCount); 
            zeroCount = 0; 
          }
          writeVInt(out, val); 
        }
      }
      if (zeroCount > 0) {
        writeVInt(out, 0); 
        writeVInt(out, zeroCount); 
        zeroCount = 0; 
      }
      out.flush();
    } finally {
      if (out != null)
        out.flush();
    }
  }
  
  /** 
   * Writes an int in a variable-length format.  Writes between one and
   * five bytes.  Smaller values take fewer bytes.  Negative numbers are not
   * supported.
   */
  static void writeVInt(OutputStream out, int i) throws IOException {
    while ((i & ~0x7F) != 0) {
      out.write((byte)((i & 0x7f) | 0x80));
      i >>>= 7;
    }
    out.write((byte)i);
  }
  
  /** 
   * Reads an int stored in variable-length format.  Reads between one and
   * five bytes.  Smaller values take fewer bytes.  Negative numbers are not
   * supported.
   */
  static int readVInt(InputStream in) throws IOException {
    byte b = (byte)in.read();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = (byte)in.read();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }
}
