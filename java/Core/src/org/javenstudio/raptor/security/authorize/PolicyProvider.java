package org.javenstudio.raptor.security.authorize;

import java.security.Policy;


/**
 * {@link PolicyProvider} provides the {@link Service} definitions to the
 * security {@link Policy} in effect for Hadoop.
 *
 */
public abstract class PolicyProvider {

  /**
   * Configuration key for the {@link PolicyProvider} implementation.
   */
  public static final String POLICY_PROVIDER_CONFIG = 
    "security.authorization.policyprovider";
  
  /**
   * A default {@link PolicyProvider} without any defined services.
   */
  public static final PolicyProvider DEFAULT_POLICY_PROVIDER =
    new PolicyProvider() {
    public Service[] getServices() {
      return null;
    }
  };
  
  /**
   * Get the {@link Service} definitions from the {@link PolicyProvider}.
   * @return the {@link Service} definitions
   */
  public abstract Service[] getServices();
}
