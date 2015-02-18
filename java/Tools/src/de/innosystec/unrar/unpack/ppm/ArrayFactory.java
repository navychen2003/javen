package de.innosystec.unrar.unpack.ppm;

/**
 * <p> This class holds factories to produces arrays of variable length.
 *     It allows for object recycling, pre-allocation and {@link StackContext
 *     stack} allocations:[code]
 *     // Primitive types.
 *     char[] buffer = ArrayFactory.CHARS_FACTORY.array(1024); // Possibly recycled.
 *     for (int i = reader.read(buffer, 0, buffer.length); i > 0;) {
 *         ...
 *     }
 *     ArrayFactory.CHARS_FACTORY.recycle(buffer); //  
 *
 *     // Custom types.
 *     static ArrayFactory<Vertex[]> VERTICES_FACTORY = new ArrayFactory<Vertex[]> {
 *         protected Vertex[] create(int size) {
 *             return new Vertex[size];
 *         }
 *     };
 *     ...
 *     Vertex[] vertices = VERTICES_FACTORY.array(256);
 *     [/code]</p>
 *          
 * @author  <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 5.0, May 5, 2007
 */
@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
public abstract class ArrayFactory<T> {

    /**
     * Holds factory for <code>boolean</code> arrays.
     */
    public static final ArrayFactory<boolean[]> BOOLEANS_FACTORY = new ArrayFactory() {

        protected boolean[] create(int size) {
            return new boolean[size];
        }

        public void recycle(boolean[] array) {
            recycle(array, ((boolean[]) array).length);
        }
    };
    /**
     * Holds factory for <code>byte</code> arrays.
     */
    public static final ArrayFactory<byte[]> BYTES_FACTORY = new ArrayFactory() {

        protected byte[] create(int size) {
            return new byte[size];
        }

        public void recycle(byte[] array) {
            recycle(array, ((byte[]) array).length);
        }
    };
    /**
     * Holds factory for <code>char</code> arrays.
     */
    public static final ArrayFactory<char[]> CHARS_FACTORY = new ArrayFactory() {

        protected char[] create(int size) {
            return new char[size];
        }

        public void recycle(char[] array) {
            recycle(array, ((char[]) array).length);
        }
    };
    /**
     * Holds factory for <code>short</code> arrays.
     */
    public static final ArrayFactory<short[]> SHORTS_FACTORY = new ArrayFactory() {

        protected short[] create(int size) {
            return new short[size];
        }

        public void recycle(short[] array) {
            recycle(array, ((short[]) array).length);
        }
    };
    /**
     * Holds factory for <code>int</code> arrays.
     */
    public static final ArrayFactory<int[]> INTS_FACTORY = new ArrayFactory() {

        protected int[] create(int size) {
            return new int[size];
        }

        public void recycle(int[] array) {
            recycle(array, ((int[]) array).length);
        }
    };
    /**
     * Holds factory for <code>long</code> arrays.
     */
    public static final ArrayFactory<long[]> LONGS_FACTORY = new ArrayFactory() {

        protected long[] create(int size) {
            return new long[size];
        }

        public void recycle(long[] array) {
            recycle(array, ((long[]) array).length);
        }
    };
    /**
     * Holds factory for <code>float</code> arrays.
     */
    public static final ArrayFactory<float[]> FLOATS_FACTORY = new ArrayFactory() {

        protected float[] create(int size) {
            return new float[size];
        }

        public void recycle(float[] array) {
            recycle(array, ((float[]) array).length);
        }
    };
    /**
     * Holds factory for <code>double</code> arrays.
     */
    public static final ArrayFactory<double[]> DOUBLES_FACTORY = new ArrayFactory() {

        protected double[] create(int size) {
            return new double[size];
        }

        public void recycle(double[] array) {
            recycle(array, ((double[]) array).length);
        }
    };
    /**
     * Holds factory for generic <code>Object</code> arrays.
     */
    public static final ArrayFactory<Object[]> OBJECTS_FACTORY = new ArrayFactory() {

        protected Object[] create(int size) {
            return new Object[size];
        }

        public void recycle(Object[] array) {
            recycle(array, ((Object[]) array).length);
        }
    };


    // Above 65536 we use the heap exclusively. 
    /**
     * Default constructor.
     */
    public ArrayFactory() {
    }

    /**
     * Returns an array possibly recycled or preallocated of specified 
     * minimum size.
     * 
     * @param capacity the minimum size of the array to be returned.
     * @return a recycled, pre-allocated or new factory array.
     */
    public final T array(int capacity) { // Short to be inlined.
        return largeArray(capacity);
    }

    private final T largeArray(int capacity) {
        return create(capacity); // Default allocation for very large arrays.
    }

    /**
     * Recycles the specified arrays.
     * 
     * @param array the array to be recycled.
     */
    public void recycle(T array) { // Short to be inlined.
        int length = ((Object[])array).length;
        recycle(array, length);
    }

    final void recycle(Object array, int length) {
    }

    /**
     * Constructs a new array of specified size from this factory 
     * (using the <code>new</code> keyword).
     *
     * @param size the size of the array.
     * @return a new factory array.
     */
    protected abstract T create(int size);
}
