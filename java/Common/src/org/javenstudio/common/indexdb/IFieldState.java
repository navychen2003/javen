package org.javenstudio.common.indexdb;

/**
 * This class tracks the number and position / offset parameters of terms
 * being added to the index. The information collected in this class is
 * also used to calculate the normalization factor for a field.
 */
public interface IFieldState {

  	/**
  	 * Get the last processed term position.
  	 * @return the position
  	 */
  	public int getPosition();
	
  	/**
  	 * Get total number of terms in this field.
  	 * @return the length
  	 */
  	public int getLength();
  	
  	/**
  	 * Get the number of terms with <code>positionIncrement == 0</code>.
  	 * @return the numOverlap
  	 */
  	public int getNumOverlap();
  	
  	/**
  	 * Get end offset of the last processed term.
  	 * @return the offset
  	 */
  	public int getOffset();
  	
  	/**
  	 * Get boost value. This is the cumulative product of
  	 * document boost and field boost for all field instances
  	 * sharing the same field name.
  	 * @return the boost
  	 */
  	public float getBoost();
  	
  	/**
  	 * Get the maximum term-frequency encountered for any term in the field.  A
  	 * field containing "the quick brown fox jumps over the lazy dog" would have
  	 * a value of 2, because "the" appears twice.
  	 */
  	public int getMaxTermFrequency();
  	
  	/**
  	 * Return the number of unique terms encountered in this field.
  	 */
  	public int getUniqueTermCount();
  	
  	/**
  	 * Return the field's name
  	 */
  	public String getName();
  	
}
