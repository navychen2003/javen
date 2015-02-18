package org.javenstudio.falcon.search.schema;

/**
 * <code>CopyField</code> contains all the information of a valid copy fields in an index.
 * 
 */
public class CopyField {

	public static final int UNLIMITED = 0;
	
	private final SchemaField mSource;
	private final SchemaField mDestination;
	private final int mMaxChars;
  
	public CopyField(final SchemaField source, final SchemaField destination) {
		this(source, destination, UNLIMITED);
	}

	/**
	 * @param source The SchemaField of the source field.
	 * @param destination The SchemaField of the destination field.
	 * @param maxChars Maximum number of chars in source field to copy to destination field.
	 * If equal to 0, there is no limit.
	 */
	public CopyField(final SchemaField source, final SchemaField destination,
			final int maxChars) {
		if (source == null || destination == null) 
			throw new IllegalArgumentException("Source or Destination SchemaField can't be NULL.");
		
		if (maxChars < 0) 
			throw new IllegalArgumentException("Attribute maxChars can't have a negative value.");
		
		mSource = source;
		mDestination = destination;
		mMaxChars = maxChars;
	}
  
	public String getLimitedValue( final String val ){
		return mMaxChars == UNLIMITED || val.length() < mMaxChars ?
				val : val.substring(0, mMaxChars);
	}

	/**
	 * @return source SchemaField
	 */
	public SchemaField getSource() {
		return mSource;
	}

	/**
	 * @return destination SchemaField
	 */
	public SchemaField getDestination() {
		return mDestination;
	}

	/**
	 * @return the maximum number of chars in source field to copy to destination field.
	 */
	public int getMaxChars() {
		return mMaxChars;
	}
	
}
