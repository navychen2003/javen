package org.javenstudio.raptor.io; 

import java.lang.reflect.Array;
import java.io.Serializable; 
import java.io.DataInput; 
import java.io.DataOutput; 
import java.io.IOException; 
import java.io.ByteArrayOutputStream; 
import java.io.ByteArrayInputStream; 
import java.io.DataOutputStream; 
import java.io.DataInputStream; 
import java.io.ObjectOutputStream; 
import java.io.ObjectInputStream; 
import java.util.Date; 
import java.util.Map; 
import java.util.HashMap; 

import org.javenstudio.raptor.io.Writable;
import org.javenstudio.raptor.io.UTF8;
import org.javenstudio.raptor.io.WritableFactories;
import org.javenstudio.raptor.io.WritableUtils;


@SuppressWarnings("deprecation")
public class ObjectUtils {
  //private static final Logger LOG = Logger.getLogger(ObjectUtils.class);

  public static final int BUFFER_SIZE = 40960; 

  protected static final Map<String, Class<?>> PRIMITIVE_NAMES = new HashMap<String, Class<?>>();
  protected static final Map<String, String>   CLASS_NAMES = new HashMap<String, String>();
  protected static final Map<String, String>   CLASS_KEYS  = new HashMap<String, String>();
  static {
    PRIMITIVE_NAMES.put("boolean", Boolean.TYPE);
    PRIMITIVE_NAMES.put("byte", Byte.TYPE);
    PRIMITIVE_NAMES.put("char", Character.TYPE);
    PRIMITIVE_NAMES.put("short", Short.TYPE);
    PRIMITIVE_NAMES.put("int", Integer.TYPE);
    PRIMITIVE_NAMES.put("long", Long.TYPE);
    PRIMITIVE_NAMES.put("float", Float.TYPE);
    PRIMITIVE_NAMES.put("double", Double.TYPE);
    PRIMITIVE_NAMES.put("void", Void.TYPE);

    putClassNameKey("boolean", "b");
    putClassNameKey("byte", "z");
    putClassNameKey("char", "c");
    putClassNameKey("short", "n");
    putClassNameKey("int", "i");
    putClassNameKey("long", "l");
    putClassNameKey("float", "f");
    putClassNameKey("double", "d");
    putClassNameKey("void", "v");

    putClassNameKey("java.lang.String", "S");
    putClassNameKey("java.util.Date", "T");
    putClassNameKey("java.lang.Boolean", "B");
    putClassNameKey("java.lang.Byte", "Z");
    putClassNameKey("java.lang.Character", "C");
    putClassNameKey("java.lang.Short", "N");
    putClassNameKey("java.lang.Integer", "I");
    putClassNameKey("java.lang.Long", "L");
    putClassNameKey("java.lang.Float", "F");
    putClassNameKey("java.lang.Double", "D");
  }

  static void putClassNameKey(String className, String keyName) {
    if (className == null || keyName == null) return;
    if (className.equals(keyName)) return;

    CLASS_NAMES.put(className, keyName);
    CLASS_KEYS.put(keyName, className);
  }

  public static String getClassNameByKey(String keyName) {
    if (keyName == null) return keyName;

    String className = CLASS_KEYS.get(keyName);
    return className == null ? keyName : className;
  }

  public static String getKeyByClassName(String className) {
    if (className == null) return className;

    String keyName = CLASS_NAMES.get(className);
    return keyName == null ? className : keyName;
  }


  private static class NullInstance implements Writable {
    private Class<?> declaredClass;
    @SuppressWarnings("unused")
	public NullInstance() { }
    @SuppressWarnings("rawtypes")
	public NullInstance(Class declaredClass) {
      this.declaredClass = declaredClass;
      if (this.declaredClass == null) 
        this.declaredClass = Object.class; 
    }
    public void readFields(DataInput in) throws IOException {
      String className = getClassNameByKey(UTF8.readString(in));
      declaredClass = PRIMITIVE_NAMES.get(className);
      if (declaredClass == null) {
        try {
          declaredClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e.toString());
        }
      }
    }
    public void write(DataOutput out) throws IOException {
      UTF8.writeString(out, getKeyByClassName(declaredClass.getName()));
    }
  }

  @SuppressWarnings({ "unchecked", "unused", "rawtypes" })
  public static Object cloneObject(Object obj) {
    if (obj == null) return null; 

    String className = obj.getClass().getName();
    Class<?> declaredClass = PRIMITIVE_NAMES.get(className);
    if (declaredClass == null) {
      try {
        declaredClass = Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("cloneObject can't find class", e);
      }
    }

    Object instance;

    if (declaredClass.isPrimitive()) {            // primitive types

      if (declaredClass == Boolean.TYPE) {             // boolean
        instance = new Boolean(((Boolean)obj).booleanValue());
      } else if (declaredClass == Character.TYPE) {    // char
        instance = new Character(((Character)obj).charValue());
      } else if (declaredClass == Byte.TYPE) {         // byte
        instance = new Byte(((Byte)obj).byteValue());
      } else if (declaredClass == Short.TYPE) {        // short
        instance = new Short(((Short)obj).shortValue());
      } else if (declaredClass == Integer.TYPE) {      // int
        instance = new Integer(((Integer)obj).intValue());
      } else if (declaredClass == Long.TYPE) {         // long
        instance = new Long(((Long)obj).longValue());
      } else if (declaredClass == Float.TYPE) {        // float
        instance = new Float(((Float)obj).floatValue());
      } else if (declaredClass == Double.TYPE) {       // double
        instance = new Double(((Double)obj).doubleValue());
      } else if (declaredClass == Void.TYPE) {         // void
        instance = null;
      } else {
        throw new IllegalArgumentException("Not a primitive: "+declaredClass);
      }

    } else if (declaredClass.isArray()) {              // array
      int length = Array.getLength(obj);
      instance = Array.newInstance(declaredClass.getComponentType(), length);
      for (int i = 0; i < length; i++) {
        Array.set(instance, i, cloneObject(Array.get(obj, i)));
      }

    } else if (declaredClass == Date.class) {          // Date
      instance = new Date(((Date)obj).getTime());

    } else if (declaredClass == String.class) {        // String
      instance = new String((String)obj);

    } else if (declaredClass == Boolean.class) {       // boolean
      instance = Boolean.valueOf(((Boolean)obj).booleanValue());

    } else if (declaredClass == Character.class) {     // char
      instance = new Character(((Character)obj).charValue());

    } else if (declaredClass == Byte.class) {    // byte
      instance = new Byte(((Byte)obj).byteValue());

    } else if (declaredClass == Short.class) {   // short
      instance = new Short(((Short)obj).shortValue());

    } else if (declaredClass == Integer.class) { // int
      instance = new Integer(((Integer)obj).intValue());

    } else if (declaredClass == Long.class) {    // long
      instance = new Long(((Long)obj).longValue());

    } else if (declaredClass == Float.class) {   // float
      instance = new Float(((Float)obj).floatValue());

    } else if (declaredClass == Double.class) {  // double
      instance = new Double(((Double)obj).doubleValue());

    } else if (declaredClass.isEnum()) {         // enum
      instance = Enum.valueOf((Class<? extends Enum>) declaredClass, ((Enum)obj).name());

    } else {                                      // Writable
      Writable objw = (Writable)obj; 
      if (objw == null) throw new RuntimeException("cloneObject can't know this object: "+className); 

      try {
        byte[] buf = getWritableBytes(objw); 
        ByteArrayInputStream bais = new ByteArrayInputStream(buf); 
        DataInputStream in = new DataInputStream(bais); 

        Class instanceClass = null;
        try {
          instanceClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("cloneObject can't find class", e);
        }

        Writable writable = WritableFactories.newInstance(instanceClass);
        writable.readFields(in);
        instance = writable;

        in.close(); 
        bais.close(); 

        if (instanceClass == NullInstance.class) {  // null
          declaredClass = ((NullInstance)instance).declaredClass;
          instance = null;
        }
      } catch (Exception e) {
        throw new RuntimeException("cloneObject can't clone writable: "+e); 
      }
    }

    return instance;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Object readObject(DataInput in) throws IOException { 
    String className = getClassNameByKey(UTF8.readString(in));
    Class<?> declaredClass = PRIMITIVE_NAMES.get(className);
    if (declaredClass == null) {
      try {
        declaredClass = Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("readObject can't find class", e);
      }
    }

    Object instance;

    if (declaredClass.isPrimitive()) {            // primitive types

      if (declaredClass == Boolean.TYPE) {             // boolean
        instance = Boolean.valueOf(in.readBoolean());
      } else if (declaredClass == Character.TYPE) {    // char
        instance = new Character(in.readChar());
      } else if (declaredClass == Byte.TYPE) {         // byte
        instance = new Byte(in.readByte());
      } else if (declaredClass == Short.TYPE) {        // short
        instance = new Short(in.readShort());
      } else if (declaredClass == Integer.TYPE) {      // int
        instance = new Integer(in.readInt());
      } else if (declaredClass == Long.TYPE) {         // long
        instance = new Long(in.readLong());
      } else if (declaredClass == Float.TYPE) {        // float
        instance = new Float(in.readFloat());
      } else if (declaredClass == Double.TYPE) {       // double
        instance = new Double(in.readDouble());
      } else if (declaredClass == Void.TYPE) {         // void
        instance = null;
      } else {
        throw new IllegalArgumentException("Not a primitive: "+declaredClass);
      }

    } else if (declaredClass.isArray()) {              // array
      int length = in.readInt();
      instance = Array.newInstance(declaredClass.getComponentType(), length);
      for (int i = 0; i < length; i++) {
        Array.set(instance, i, readObject(in));
      }

    } else if (declaredClass == Date.class) {          // Date
      instance = new Date(in.readLong());

    } else if (declaredClass == String.class) {        // String
      //instance = UTF8.readString(in);
      instance = WritableUtils.readCompressedString(in);

    } else if (declaredClass == Boolean.class) {       // boolean
      instance = Boolean.valueOf(in.readBoolean());

    } else if (declaredClass == Character.class) {     // char
      instance = new Character(in.readChar());

    } else if (declaredClass == Byte.class) {    // byte
      instance = new Byte(in.readByte());

    } else if (declaredClass == Short.class) {   // short
      instance = new Short(in.readShort());

    } else if (declaredClass == Integer.class) { // int
      instance = new Integer(in.readInt());

    } else if (declaredClass == Long.class) {    // long
      instance = new Long(in.readLong());

    } else if (declaredClass == Float.class) {   // float
      instance = new Float(in.readFloat());

    } else if (declaredClass == Double.class) {  // double
      instance = new Double(in.readDouble());

    } else if (declaredClass.isEnum()) {         // enum
      instance = Enum.valueOf((Class<? extends Enum>) declaredClass, UTF8.readString(in));

    } else if (declaredClass == Serializable.class) {  // Serializable
      instance = ObjectUtils.readSerializable(in);

    } else {                                      // Writable
      Class instanceClass = null;
      try {
        instanceClass = Class.forName(getClassNameByKey(UTF8.readString(in)));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("readObject can't find class", e);
      }

      Writable writable = WritableFactories.newInstance(instanceClass);
      writable.readFields(in);
      instance = writable;

      if (instanceClass == NullInstance.class) {  // null
        declaredClass = ((NullInstance)instance).declaredClass;
        instance = null;
      }
    }

    return instance;
  }

  @SuppressWarnings("rawtypes")
  public static void writeObject(DataOutput out, Object instance, 
	  Class declaredClass) throws IOException {
    if (instance == null) {                       // null
      instance = new NullInstance(declaredClass);
      declaredClass = Writable.class;
    }

    UTF8.writeString(out, getKeyByClassName(declaredClass.getName())); // always write declared

    if (declaredClass.isArray()) {                // array
      int length = Array.getLength(instance);
      out.writeInt(length);
      for (int i = 0; i < length; i++) {
        writeObject(out, Array.get(instance, i),
                    declaredClass.getComponentType());
      }

    } else if (declaredClass == Date.class) {     // Date
      out.writeLong(((Date)instance).getTime());

    } else if (declaredClass == String.class) {   // String
      //UTF8.writeString(out, (String)instance);
      WritableUtils.writeCompressedString(out, (String)instance);

    } else if (declaredClass == Boolean.class) {  // boolean
      out.writeBoolean(((Boolean)instance).booleanValue());

    } else if (declaredClass == Character.class) { // char
      out.writeChar(((Character)instance).charValue());

    } else if (declaredClass == Byte.class) {    // byte
      out.writeByte(((Byte)instance).byteValue());

    } else if (declaredClass == Short.class) {   // short
      out.writeShort(((Short)instance).shortValue());

    } else if (declaredClass == Integer.class) { // int
      out.writeInt(((Integer)instance).intValue());

    } else if (declaredClass == Long.class) {    // long
      out.writeLong(((Long)instance).longValue());

    } else if (declaredClass == Float.class) {   // float
      out.writeFloat(((Float)instance).floatValue());

    } else if (declaredClass == Double.class) {  // double
      out.writeDouble(((Double)instance).doubleValue());

    } else if (declaredClass.isPrimitive()) {     // primitive type

      if (declaredClass == Boolean.TYPE) {        // boolean
        out.writeBoolean(((Boolean)instance).booleanValue());
      } else if (declaredClass == Character.TYPE) { // char
        out.writeChar(((Character)instance).charValue());
      } else if (declaredClass == Byte.TYPE) {    // byte
        out.writeByte(((Byte)instance).byteValue());
      } else if (declaredClass == Short.TYPE) {   // short
        out.writeShort(((Short)instance).shortValue());
      } else if (declaredClass == Integer.TYPE) { // int
        out.writeInt(((Integer)instance).intValue());
      } else if (declaredClass == Long.TYPE) {    // long
        out.writeLong(((Long)instance).longValue());
      } else if (declaredClass == Float.TYPE) {   // float
        out.writeFloat(((Float)instance).floatValue());
      } else if (declaredClass == Double.TYPE) {  // double
        out.writeDouble(((Double)instance).doubleValue());
      } else if (declaredClass == Void.TYPE) {    // void
      } else {
        throw new IllegalArgumentException("Not a primitive: "+declaredClass);
      }
    } else if (declaredClass.isEnum()) {         // enum
      UTF8.writeString(out, ((Enum)instance).name());

    } else if (Writable.class.isAssignableFrom(declaredClass)) { // Writable
      UTF8.writeString(out, getKeyByClassName(instance.getClass().getName()));
      ((Writable)instance).write(out);

    } else if (Serializable.class.isAssignableFrom(declaredClass)) { // Serializable
      UTF8.writeString(out, Serializable.class.getName());
      ObjectUtils.writeSerializable(out, instance);

    } else {
      throw new IOException("Can't write: "+instance+" as "+declaredClass);
    }
  }

  public static Object readSerializable(DataInput in) throws IOException {
    try {
      int len = in.readInt(); 
      byte[] bytebuf = new byte[len]; 
      in.readFully(bytebuf, 0, len); 
      ByteArrayInputStream inbuf = new ByteArrayInputStream(bytebuf); 
      ObjectInputStream inobj = new ObjectInputStream(inbuf); 
      Object instance = inobj.readObject(); 
      inobj.close(); inbuf.close(); 
      return instance; 
    } catch (ClassNotFoundException e) {
      throw new IOException(e.toString()); 
    }
  }

  public static void writeSerializable(DataOutput out, Object instance) throws IOException {
    ByteArrayOutputStream  outbuf = new ByteArrayOutputStream(ObjectUtils.BUFFER_SIZE);
    ObjectOutputStream outobj = new ObjectOutputStream(outbuf);
    outobj.writeObject(instance);
    outobj.flush(); outbuf.close();
    byte[] bytebuf = outbuf.toByteArray(); 
    out.writeInt(bytebuf.length); 
    out.write(bytebuf); 
  }

  private static byte[] getWritableBytes(Writable instance) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    instance.write(dos);
    dos.flush();
    byte[] buf = baos.toByteArray();
    dos.close(); baos.close();
    return buf; 
  }

}
