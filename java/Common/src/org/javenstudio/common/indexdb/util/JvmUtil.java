package org.javenstudio.common.indexdb.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;

/**
 * Estimates the size (memory representation) of Java objects.
 * 
 * <p>NOTE: Starting with Indexdb 3.6, creating instances of this class
 * is deprecated. If you still do this, please note, that instances of
 * {@code RamUsageEstimator} are not thread-safe!
 * It is also deprecated to enable checking of String intern-ness,
 * the new static method no longer allow to do this. Interned strings
 * will be counted as any other object and count for memory usage.
 * 
 * <p>In Indexdb 3.6, custom {@code MemoryModel}s were completely
 * removed. The new implementation is now using Hotspot&trade; internals
 * to get the correct scale factors and offsets for calculating
 * memory usage.
 * 
 * @see #sizeOf(Object)
 * @see #shallowSizeOf(Object)
 * @see #shallowSizeOfInstance(Class)
 * 
 */
public class JvmUtil {
	private static Logger LOG = Logger.getLogger(JvmUtil.class);

	/** JVM vendor info. */
	public static final String JVM_VENDOR = System.getProperty("java.vm.vendor");
	public static final String JVM_VERSION = System.getProperty("java.vm.version");
	public static final String JVM_NAME = System.getProperty("java.vm.name");

	/** The value of <tt>System.getProperty("java.version")<tt>. **/
	public static final String JAVA_VERSION = System.getProperty("java.version");
	
	/** JVM info string for debugging and reports. */
	//public final static String JVM_INFO_STRING;
	
	/** The value of <tt>System.getProperty("os.name")<tt>. **/
	public static final String OS_NAME = System.getProperty("os.name");
	/** True iff running on Linux. */
	public static final boolean LINUX = OS_NAME.startsWith("Linux");
	/** True iff running on Windows. */
	public static final boolean WINDOWS = OS_NAME.startsWith("Windows");
	/** True iff running on SunOS. */
	public static final boolean SUN_OS = OS_NAME.startsWith("SunOS");
	/** True iff running on Mac OS X */
	public static final boolean MAC_OS_X = OS_NAME.startsWith("Mac OS X");

	public static final String OS_ARCH = System.getProperty("os.arch");
	public static final String OS_VERSION = System.getProperty("os.version");
	public static final String JAVA_VENDOR = System.getProperty("java.vendor");

	public static final boolean JRE_IS_MINIMUM_JAVA6;
	public static final boolean JRE_IS_MINIMUM_JAVA7;
	  
	/** True iff running on a 64bit JVM */
	public static final boolean JRE_IS_64BIT;
	
	/** One kilobyte bytes. */
	public static final long ONE_KB = 1024;
	  
	/** One megabyte bytes. */
	public static final long ONE_MB = ONE_KB * ONE_KB;
	  
	/** One gigabyte bytes.*/
	public static final long ONE_GB = ONE_KB * ONE_MB;
	
	public final static int NUM_BYTES_BOOLEAN = 1;
	public final static int NUM_BYTES_BYTE = 1;
	public final static int NUM_BYTES_CHAR = 2;
	public final static int NUM_BYTES_SHORT = 2;
	public final static int NUM_BYTES_INT = 4;
	public final static int NUM_BYTES_FLOAT = 4;
	public final static int NUM_BYTES_LONG = 8;
	public final static int NUM_BYTES_DOUBLE = 8;
	
	/** 
	 * Number of bytes this jvm uses to represent an object reference. 
	 */
	public final static int NUM_BYTES_OBJECT_REF;
	
	/**
	 * Number of bytes to represent an object header (no fields, no alignments).
	 */
	public final static int NUM_BYTES_OBJECT_HEADER;
	
	/**
	 * Number of bytes to represent an array header (no content, but with alignments).
	 */
	public final static int NUM_BYTES_ARRAY_HEADER;
	  
	/**
	 * A constant specifying the object alignment boundary inside the JVM. Objects will
	 * always take a full multiple of this constant, possibly wasting some space. 
	 */
	public final static int NUM_BYTES_OBJECT_ALIGNMENT;
	
	/**
	 * A handle to <code>sun.misc.Unsafe</code>.
	 */
	private final static Object sTheUnsafe;
  
	/**
	 * A handle to <code>sun.misc.Unsafe#fieldOffset(Field)</code>.
	 */
	private final static Method sObjectFieldOffsetMethod;
	
	private JvmUtil() {} // no instance
	
	/**
	 * Sizes of primitive classes.
	 */
	private static final Map<Class<?>,Integer> sPrimitiveSizes;
	static {
	    sPrimitiveSizes = new IdentityHashMap<Class<?>,Integer>();
	    sPrimitiveSizes.put(boolean.class, Integer.valueOf(NUM_BYTES_BOOLEAN));
	    sPrimitiveSizes.put(byte.class, Integer.valueOf(NUM_BYTES_BYTE));
	    sPrimitiveSizes.put(char.class, Integer.valueOf(NUM_BYTES_CHAR));
	    sPrimitiveSizes.put(short.class, Integer.valueOf(NUM_BYTES_SHORT));
	    sPrimitiveSizes.put(int.class, Integer.valueOf(NUM_BYTES_INT));
	    sPrimitiveSizes.put(float.class, Integer.valueOf(NUM_BYTES_FLOAT));
	    sPrimitiveSizes.put(double.class, Integer.valueOf(NUM_BYTES_DOUBLE));
	    sPrimitiveSizes.put(long.class, Integer.valueOf(NUM_BYTES_LONG));
	    
	    boolean is64Bit = false;
	    try {
	      final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
	      final Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
	      unsafeField.setAccessible(true);
	      final Object unsafe = unsafeField.get(null);
	      final int addressSize = ((Number) unsafeClass.getMethod("addressSize")
	        .invoke(unsafe)).intValue();
	      //System.out.println("Address size: " + addressSize);
	      is64Bit = addressSize >= 8;
	    } catch (Exception e) {
	      final String x = System.getProperty("sun.arch.data.model");
	      if (x != null) {
	        is64Bit = x.indexOf("64") != -1;
	      } else {
	        if (OS_ARCH != null && OS_ARCH.indexOf("64") != -1) {
	          is64Bit = true;
	        } else {
	          is64Bit = false;
	        }
	      }
	    }
	    JRE_IS_64BIT = is64Bit;

	    // this method only exists in Java 6:
	    boolean v6 = true;
	    try {
	      String.class.getMethod("isEmpty");
	    } catch (NoSuchMethodException nsme) {
	      v6 = false;
	    }
	    JRE_IS_MINIMUM_JAVA6 = v6;
	    
	    // this method only exists in Java 7:
	    boolean v7 = true;
	    try {
	      Throwable.class.getMethod("getSuppressed");
	    } catch (NoSuchMethodException nsme) {
	      v7 = false;
	    }
	    JRE_IS_MINIMUM_JAVA7 = v7;
	    
	    // Initialize empirically measured defaults. We'll modify them to the current
	    // JVM settings later on if possible.
	    int referenceSize = JRE_IS_64BIT ? 8 : 4;
	    int objectHeader = JRE_IS_64BIT ? 16 : 8;
	    // The following is objectHeader + NUM_BYTES_INT, but aligned (object alignment)
	    // so on 64 bit JVMs it'll be align(16 + 4, @8) = 24.
	    int arrayHeader = JRE_IS_64BIT ? 24 : 12;

	    Class<?> unsafeClass = null;
	    Object tempTheUnsafe = null;
	    try {
	      unsafeClass = Class.forName("sun.misc.Unsafe");
	      final Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
	      unsafeField.setAccessible(true);
	      tempTheUnsafe = unsafeField.get(null);
	    } catch (Exception e) {
	      // Ignore.
	    }
	    sTheUnsafe = tempTheUnsafe;

	    // get object reference size by getting scale factor of Object[] arrays:
	    try {
	      final Method arrayIndexScaleM = unsafeClass.getMethod("arrayIndexScale", Class.class);
	      referenceSize = ((Number) arrayIndexScaleM.invoke(sTheUnsafe, Object[].class)).intValue();
	    } catch (Exception e) {
	      // ignore.
	    }
	    
	    // "best guess" based on reference size. We will attempt to modify
	    // these to exact values if there is supported infrastructure.
	    objectHeader = JRE_IS_64BIT ? (8 + referenceSize) : 8;
	    arrayHeader =  JRE_IS_64BIT ? (8 + 2 * referenceSize) : 12;

	    // get the object header size:
	    // - first try out if the field offsets are not scaled (see warning in Unsafe docs)
	    // - get the object header size by getting the field offset of the first field of a dummy object
	    // If the scaling is byte-wise and unsafe is available, enable dynamic size measurement for
	    // estimateRamUsage().
	    Method tempObjectFieldOffsetMethod = null;
	    try {
	      final Method objectFieldOffsetM = unsafeClass.getMethod("objectFieldOffset", Field.class);
	      final Field dummy1Field = DummyTwoLongObject.class.getDeclaredField("dummy1");
	      final int ofs1 = ((Number) objectFieldOffsetM.invoke(sTheUnsafe, dummy1Field)).intValue();
	      final Field dummy2Field = DummyTwoLongObject.class.getDeclaredField("dummy2");
	      final int ofs2 = ((Number) objectFieldOffsetM.invoke(sTheUnsafe, dummy2Field)).intValue();
	      if (Math.abs(ofs2 - ofs1) == NUM_BYTES_LONG) {
	        final Field baseField = DummyOneFieldObject.class.getDeclaredField("base");
	        objectHeader = ((Number) objectFieldOffsetM.invoke(sTheUnsafe, baseField)).intValue();
	        tempObjectFieldOffsetMethod = objectFieldOffsetM;
	      }
	    } catch (Exception e) {
	      // Ignore.
	    }
	    sObjectFieldOffsetMethod = tempObjectFieldOffsetMethod;

	    // Get the array header size by retrieving the array base offset
	    // (offset of the first element of an array).
	    try {
	      final Method arrayBaseOffsetM = unsafeClass.getMethod("arrayBaseOffset", Class.class);
	      // we calculate that only for byte[] arrays, it's actually the same for all types:
	      arrayHeader = ((Number) arrayBaseOffsetM.invoke(sTheUnsafe, byte[].class)).intValue();
	    } catch (Exception e) {
	      // Ignore.
	    }
	    
	    NUM_BYTES_OBJECT_REF = referenceSize;
	    NUM_BYTES_OBJECT_HEADER = objectHeader;
	    NUM_BYTES_ARRAY_HEADER = arrayHeader;
	    
	    // Try to get the object alignment (the default seems to be 8 on Hotspot, 
	    // regardless of the architecture). Retrieval only works with Java 6.
	    int objectAlignment = 8;

	    NUM_BYTES_OBJECT_ALIGNMENT = objectAlignment;

	    String JVM_INFO_STRING = "[JVM: " +
	        JVM_NAME + ", " + JVM_VERSION + ", " + JVM_VENDOR + ", " + 
	        JAVA_VENDOR + ", " + JAVA_VERSION + 
	        ", Reference size is " + referenceSize + 
	        ", Object header is " + objectHeader + 
	        ", Array header is " + arrayHeader + 
	        "]";
	    
	    if (LOG.isDebugEnabled()) 
	    	LOG.debug("Java VM information: " + JVM_INFO_STRING);
	}
	
	// Object with just one field to determine the object header size by getting the offset of the dummy field:
	@SuppressWarnings("unused")
	private static final class DummyOneFieldObject {
		public byte base;
	}

	// Another test object for checking, if the difference in offsets of dummy1 and dummy2 is 8 bytes.
	// Only then we can be sure that those are real, unscaled offsets:
	@SuppressWarnings("unused")
	private static final class DummyTwoLongObject {
		public long dummy1, dummy2;
	}
	
	/** 
	 * Aligns an object size to be the next multiple of {@link #NUM_BYTES_OBJECT_ALIGNMENT}. 
	 */
	public static long alignObjectSize(long size) {
		size += (long) NUM_BYTES_OBJECT_ALIGNMENT - 1L;
		return size - (size % NUM_BYTES_OBJECT_ALIGNMENT);
	}
  
	/** Returns the size in bytes of the byte[] object. */
	public static long sizeOf(byte[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + arr.length);
	}
  
	/** Returns the size in bytes of the boolean[] object. */
	public static long sizeOf(boolean[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + arr.length);
	}
  
	/** Returns the size in bytes of the char[] object. */
	public static long sizeOf(char[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_CHAR * arr.length);
	}

	/** Returns the size in bytes of the short[] object. */
	public static long sizeOf(short[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_SHORT * arr.length);
	}
  
	/** Returns the size in bytes of the int[] object. */
	public static long sizeOf(int[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_INT * arr.length);
	}
  
	/** Returns the size in bytes of the float[] object. */
	public static long sizeOf(float[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_FLOAT * arr.length);
	}
  
	/** Returns the size in bytes of the long[] object. */
	public static long sizeOf(long[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_LONG * arr.length);
	}
  
	/** Returns the size in bytes of the double[] object. */
	public static long sizeOf(double[] arr) {
		return alignObjectSize((long) NUM_BYTES_ARRAY_HEADER + (long) NUM_BYTES_DOUBLE * arr.length);
	}

	/** 
	 * Estimates the RAM usage by the given object. It will
	 * walk the object tree and sum up all referenced objects.
	 * 
	 * <p><b>Resource Usage:</b> This method internally uses a set of
	 * every object seen during traversals so it does allocate memory
	 * (it isn't side-effect free). After the method exits, this memory
	 * should be GCed.</p>
	 */
	public static long sizeOf(Object obj) {
		return measureObjectSize(obj);
	}

	/** 
	 * Estimates a "shallow" memory usage of the given object. For arrays, this will be the
	 * memory taken by array storage (no subreferences will be followed). For objects, this
	 * will be the memory taken by the fields.
	 * 
	 * JVM object alignments are also applied.
	 */
	public static long shallowSizeOf(Object obj) {
		if (obj == null) return 0;
		final Class<?> clz = obj.getClass();
		if (clz.isArray()) {
			return shallowSizeOfArray(obj);
		} else {
			return shallowSizeOfInstance(clz);
		}
	}

	/**
	 * Returns the shallow instance size in bytes an instance of the given class would occupy.
	 * This works with all conventional classes and primitive types, but not with arrays
	 * (the size then depends on the number of elements and varies from object to object).
	 * 
	 * @see #shallowSizeOf(Object)
	 * @throws IllegalArgumentException if {@code clazz} is an array class. 
	 */
	public static long shallowSizeOfInstance(Class<?> clazz) {
		if (clazz.isArray())
			throw new IllegalArgumentException("This method does not work with array classes.");
		if (clazz.isPrimitive())
			return sPrimitiveSizes.get(clazz);
    
		long size = NUM_BYTES_OBJECT_HEADER;

		// Walk type hierarchy
		for (; clazz != null; clazz = clazz.getSuperclass()) {
			final Field[] fields = clazz.getDeclaredFields();
			for (Field f : fields) {
				if (!Modifier.isStatic(f.getModifiers())) {
					size = adjustForField(size, f);
				}
			}
		}
		return alignObjectSize(size);    
	}

	/**
	 * Return shallow size of any <code>array</code>.
	 */
	private static long shallowSizeOfArray(Object array) {
		long size = NUM_BYTES_ARRAY_HEADER;
		final int len = Array.getLength(array);
		if (len > 0) {
			Class<?> arrayElementClazz = array.getClass().getComponentType();
			if (arrayElementClazz.isPrimitive()) {
				size += (long) len * sPrimitiveSizes.get(arrayElementClazz);
			} else {
				size += (long) NUM_BYTES_OBJECT_REF * len;
			}
		}
		return alignObjectSize(size);
	}

	/**
	 * Non-recursive version of object descend. This consumes more memory than recursive in-depth 
	 * traversal but prevents stack overflows on long chains of objects
	 * or complex graphs (a max. recursion depth on my machine was ~5000 objects linked in a chain
	 * so not too much).  
	 */
	private static long measureObjectSize(Object root) {
		// Objects seen so far.
		final IdentityHashSet<Object> seen = new IdentityHashSet<Object>();
		// Class cache with reference Field and precalculated shallow size. 
		final IdentityHashMap<Class<?>, ClassCache> classCache = new IdentityHashMap<Class<?>, ClassCache>();
		// Stack of objects pending traversal. Recursion caused stack overflows. 
		final ArrayList<Object> stack = new ArrayList<Object>();
		stack.add(root);

		long totalSize = 0;
		while (!stack.isEmpty()) {
			final Object ob = stack.remove(stack.size() - 1);
			if (ob == null || seen.contains(ob)) {
				continue;
			}
			seen.add(ob);

			final Class<?> obClazz = ob.getClass();
			if (obClazz.isArray()) {
				/**
				 * Consider an array, possibly of primitive types. Push any of its references to
				 * the processing stack and accumulate this array's shallow size. 
				 */
				long size = NUM_BYTES_ARRAY_HEADER;
				final int len = Array.getLength(ob);
				if (len > 0) {
					Class<?> componentClazz = obClazz.getComponentType();
					if (componentClazz.isPrimitive()) {
						size += (long) len * sPrimitiveSizes.get(componentClazz);
					} else {
						size += (long) NUM_BYTES_OBJECT_REF * len;

						// Push refs for traversal later.
						for (int i = len; --i >= 0 ;) {
							final Object o = Array.get(ob, i);
							if (o != null && !seen.contains(o)) {
								stack.add(o);
							}
						}            
					}
				}
				totalSize += alignObjectSize(size);
				
			} else {
				/**
				 * Consider an object. Push any references it has to the processing stack
				 * and accumulate this object's shallow size. 
				 */
				try {
					ClassCache cachedInfo = classCache.get(obClazz);
					if (cachedInfo == null) {
						classCache.put(obClazz, cachedInfo = createCacheEntry(obClazz));
					}

					for (Field f : cachedInfo.mReferenceFields) {
						// Fast path to eliminate redundancies.
						final Object o = f.get(ob);
						if (o != null && !seen.contains(o)) {
							stack.add(o);
						}
					}

					totalSize += cachedInfo.mAlignedShallowInstanceSize;
				} catch (IllegalAccessException e) {
					// this should never happen as we enabled setAccessible().
					throw new RuntimeException("Reflective field access failed?", e);
				}
			}
		}

		// Help the GC (?).
		seen.clear();
		stack.clear();
		classCache.clear();

		return totalSize;
	}
	
	/**
	 * Create a cached information about shallow size and reference fields for 
	 * a given class.
	 */
	private static ClassCache createCacheEntry(final Class<?> clazz) {
		long shallowInstanceSize = NUM_BYTES_OBJECT_HEADER;
		final ArrayList<Field> referenceFields = new ArrayList<Field>(32);
		
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			final Field[] fields = c.getDeclaredFields();
			for (final Field f : fields) {
				if (!Modifier.isStatic(f.getModifiers())) {
					shallowInstanceSize = adjustForField(shallowInstanceSize, f);

					if (!f.getType().isPrimitive()) {
						f.setAccessible(true);
						referenceFields.add(f);
					}
				}
			}
		}

		return new ClassCache(
				alignObjectSize(shallowInstanceSize), 
				referenceFields.toArray(new Field[referenceFields.size()]));
	}
  
	/**
	 * This method returns the maximum representation size of an object. <code>sizeSoFar</code>
	 * is the object's size measured so far. <code>f</code> is the field being probed.
	 * 
	 * <p>The returned offset will be the maximum of whatever was measured so far and 
	 * <code>f</code> field's offset and representation size (unaligned).
	 */
	private static long adjustForField(long sizeSoFar, final Field f) {
		final Class<?> type = f.getType();
		final int fsize = type.isPrimitive() ? sPrimitiveSizes.get(type) : NUM_BYTES_OBJECT_REF;
		if (sObjectFieldOffsetMethod != null) {
			try {
				final long offsetPlusSize =
						((Number) sObjectFieldOffsetMethod.invoke(sTheUnsafe, f)).longValue() + fsize;
				return Math.max(sizeSoFar, offsetPlusSize);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException("Access problem with sun.misc.Unsafe", ex);
			} catch (InvocationTargetException ite) {
				final Throwable cause = ite.getCause();
				if (cause instanceof RuntimeException)
					throw (RuntimeException) cause;
				if (cause instanceof Error)
					throw (Error) cause;
				// this should never happen (Unsafe does not declare
				// checked Exceptions for this method), but who knows?
				throw new RuntimeException("Call to Unsafe's objectFieldOffset() throwed "+
						"checked Exception when accessing field " +
						f.getDeclaringClass().getName() + "#" + f.getName(), cause);
			}
		} else {
			// TODO: No alignments based on field type/ subclass fields alignments?
			return sizeSoFar + fsize;
		}
	}

	/**
	 * Cached information about a given class.   
	 */
	private static final class ClassCache {
		public final long mAlignedShallowInstanceSize;
		public final Field[] mReferenceFields;

		public ClassCache(long alignedShallowInstanceSize, Field[] referenceFields) {
			mAlignedShallowInstanceSize = alignedShallowInstanceSize;
			mReferenceFields = referenceFields;
		}
	}

}
