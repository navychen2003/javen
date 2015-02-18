package org.javenstudio.raptor.bigdb;


/**
 * LeaseListener is an interface meant to be implemented by users of the Leases
 * class.
 *
 * It receives events from the Leases class about the status of its accompanying
 * lease.  Users of the Leases class can use a LeaseListener subclass to, for
 * example, clean up resources after a lease has expired.
 */
public interface LeaseListener {
  /** When a lease expires, this method is called. */
  public void leaseExpired();
}

