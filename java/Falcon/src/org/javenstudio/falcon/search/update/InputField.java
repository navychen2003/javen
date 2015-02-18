package org.javenstudio.falcon.search.update;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class InputField implements Iterable<Object>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private String mName;
	private Object mValue = null; 
	private float mBoost = 1.0f;
  
	public InputField(String n) {
    	mName = n;
	}

	/**
	 * Set the value for a field.  Arrays will be converted to a collection. If
	 * a collection is given, then that collection will be used as the backing
	 * collection for the values.
	 */
	public void setValue(Object v, float b) {
		mBoost = b;

		if (v instanceof Object[]) {
			Object[] arr = (Object[])v;
			Collection<Object> c = new ArrayList<Object>( arr.length );
			for (Object o : arr) {
				c.add(o);
			}
			mValue = c;
		} else {
			mValue = v;
		}
	}

	/**
	 * Add values to a field.  If the added value is a collection, each value
	 * will be added individually.
	 */
	@SuppressWarnings("unchecked")
	public void addValue(Object v, float b) {
		if (mValue == null) {
			if (v instanceof Collection) {
				Collection<Object> c = new ArrayList<Object>( 3 );
				for (Object o : (Collection<Object>)v) {
					c.add(o);
				}
				setValue(c, b);
			} else {
				setValue(v, b);
			}

			return;
		}
    
		// The index API and input XML field specification make it possible to set boosts
		// on multi-value fields even though indexing does not support this.
		// To keep behavior consistent with what happens in the index, we accumulate
		// the product of all boosts specified for this field.
		mBoost *= b;
    
		Collection<Object> vals = null;
		if (mValue instanceof Collection) {
			vals = (Collection<Object>)mValue;
		} else {
			vals = new ArrayList<Object>(3);
			vals.add(mValue);
			mValue = vals;
		}
    
		// Add the new values to a collection
		if (v instanceof Iterable) {
			for (Object o : (Iterable<Object>)v) {
				vals.add(o);
			}
		} else if (v instanceof Object[]) {
			for (Object o : (Object[])v) {
				vals.add(o);
			}
		} else {
			vals.add(v);
		}
	}

	@SuppressWarnings("unchecked")
	public Object getFirstValue() {
		if (mValue instanceof Collection) {
			Collection<Object> c = (Collection<Object>)mValue;
			if (c.size() > 0) 
				return c.iterator().next();
      
			return null;
		}
		
		return mValue;
	}

	/**
	 * @return the value for this field.  If the field has multiple values, this
	 * will be a collection.
	 */
	public Object getValue() {
		return mValue;
	}

	/**
	 * @return the values for this field.  This will return a collection even
	 * if the field is not multi-valued
	 */
	@SuppressWarnings("unchecked")
	public Collection<Object> getValues() {
		if (mValue instanceof Collection) 
			return (Collection<Object>)mValue;
		
		if (mValue != null) {
			Collection<Object> vals = new ArrayList<Object>(1);
			vals.add(mValue);
			return vals;
		}
		
		return null;
	}

	/**
	 * @return the number of values for this field
	 */
	public int getValueCount() {
		if (mValue instanceof Collection) 
			return ((Collection<?>)mValue).size();
		
		return (mValue == null) ? 0 : 1;
	}
  
	public float getBoost() {
		return mBoost;
	}

	public void setBoost(float boost) {
		mBoost = boost;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	@SuppressWarnings("unchecked")
	public Iterator<Object> iterator() {
		if (mValue instanceof Collection) 
			return ((Collection<Object>)mValue).iterator();
		
		return new Iterator<Object>() {
				private boolean mNext = (mValue != null);
	      
				@Override
				public boolean hasNext() {
					return mNext;
				}
	
				@Override
				public Object next() {
					mNext = false;
					return mValue;
				}
	
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
	}

	@Override
	public String toString() {
		return mName + ((mBoost == 1.0) ? "=" : ("(" + mBoost + ")=")) + mValue;
	}

	@SuppressWarnings("unchecked")
	public InputField deepCopy() {
		InputField clone = new InputField(mName);
		clone.mBoost = mBoost;
		
		// We can't clone here, so we rely on simple primitives
		if (mValue instanceof Collection) {
			Collection<Object> values = (Collection<Object>) mValue;
			Collection<Object> cloneValues = new ArrayList<Object>(values.size());
			cloneValues.addAll(values);
			clone.mValue = cloneValues;
			
		} else {
			clone.mValue = mValue;
		}
		
		return clone;
	}
	
}
