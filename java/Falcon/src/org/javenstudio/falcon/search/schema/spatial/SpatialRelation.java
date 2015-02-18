package org.javenstudio.falcon.search.schema.spatial;

/**
 * The set of spatial relationships.  Naming is consistent with OGC spec
 * conventions as seen in SQL/MM and others.
 * <p/>
 * There is no equality case.  If two Shape instances are equal then the result
 * might be CONTAINS (preferred) or WITHIN.  Client logic may have to be aware
 * of this edge condition; Spatial4j testing certainly does.
 */
public enum SpatialRelation {
  WITHIN,
  CONTAINS,
  DISJOINT,
  INTERSECTS;
  //Don't have these: TOUCHES, CROSSES, OVERLAPS

  /**
   * Given the result of <code>shapeA.relate(shapeB)</code>, transposing that
   * result should yield the result of <code>shapeB.relate(shapeA)</code>. There
   * is a corner case is when the shapes are equal, in which case actually
   * flipping the relate() call will result in the same value -- either CONTAINS
   * or WITHIN.
   */
  public SpatialRelation transpose() {
    switch(this) {
      case CONTAINS: return SpatialRelation.WITHIN;
      case WITHIN: return SpatialRelation.CONTAINS;
      default: return this;
    }
  }

  /**
   * If you were to call aShape.relate(bShape) and aShape.relate(cShape), you
   * could call this to merge the intersect results as if bShape & cShape were
   * combined into {@link ShapeCollection}.
   */
  public SpatialRelation combine(SpatialRelation other) {
    // You can think of this algorithm as a state transition / automata.
    // 1. The answer must be the same no matter what the order is.
    // 2. If any INTERSECTS, then the result is INTERSECTS (done).
    // 3. A DISJOINT + WITHIN == INTERSECTS (done).
    // 4. A DISJOINT + CONTAINS == CONTAINS.
    // 5. A CONTAINS + WITHIN == INTERSECTS (done). (weird scenario)
    // 6. X + X == X.

    if (other == this)
      return this;
    if (this == DISJOINT && other == CONTAINS
        || this == CONTAINS && other == DISJOINT)
      return CONTAINS;
    return INTERSECTS;
  }

  /** Not DISJOINT, i.e. there is some sort of intersection. */
  public boolean intersects() {
    return this != DISJOINT;
  }

  /**
   * If <code>aShape.relate(bShape)</code> is r, then <code>r.inverse()</code>
   * is <code> inverse(aShape).relate(bShape)</code> whereas
   * <code>inverse(shape)</code> is theoretically the opposite area covered by a
   * shape, i.e. everywhere but where the shape is.
   * <p/>
   * Note that it's not commutative!  <code>WITHIN.inverse().inverse() !=
   * WITHIN</code>.
   */
  public SpatialRelation inverse() {
    switch(this) {
      case DISJOINT: return CONTAINS;
      case CONTAINS: return DISJOINT;
      case WITHIN: return INTERSECTS;//not commutative!
      default: break;
    }
    return INTERSECTS;
  }

}
