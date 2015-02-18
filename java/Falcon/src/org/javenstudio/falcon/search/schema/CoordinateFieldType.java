package org.javenstudio.falcon.search.schema;

/**
 * A CoordinateFieldType is the base class for {@link SchemaFieldType}s that have semantics
 * related to items in a coordinate system.
 * <br/>
 * Implementations depend on a delegating work to a sub {@link SchemaFieldType}, specified by
 * either the {@link #SUB_FIELD_SUFFIX} or the {@link #SUB_FIELD_TYPE} 
 * (the latter is used if both are defined.
 * <br/>
 * Example:
 * <pre>&lt;fieldType name="xy" class="lightning.PointFieldType" dimension="2" subFieldType="double"/&gt;
 * </pre>
 * In theory, classes deriving from this should be able to do things like represent 
 * a point, a polygon, a line, etc.
 * <br/>
 * NOTE: There can only be one sub Field Type.
 *
 */
public abstract class CoordinateFieldType extends AbstractSubTypeFieldType {
	
	/**
	 * 2 dimensional by default
	 */
	public static final int DEFAULT_DIMENSION = 2;
	public static final String DIMENSION = "dimension";

	/**
	 * The dimension of the coordinate system
	 */
	protected int mDimension;
	
	public int getDimension() {
		return mDimension;
	}
	
}
