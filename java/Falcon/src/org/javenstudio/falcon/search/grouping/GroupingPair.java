package org.javenstudio.falcon.search.grouping;

/**
 * A simple data structure to hold a pair of typed objects.
 *
 */
public class GroupingPair<A, B> {

	private final A mA;
	private final B mB;

	public GroupingPair(A a, B b) {
		mA = a;
		mB = b;
	}

	public A getA() {
		return mA;
	}

	public B getB() {
		return mB;
	}
	
}
