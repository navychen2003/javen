package org.javenstudio.raptor.security.authorize;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configurable;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.conf.ConfigurationFactory;
import org.javenstudio.raptor.security.Group;
import org.javenstudio.raptor.security.User;
import org.javenstudio.raptor.security.SecurityUtil.AccessControlList;


/**
 * A {@link Configuration} based security {@link Policy} for Hadoop.
 *
 * {@link ConfiguredPolicy} works in conjunction with a {@link PolicyProvider}
 * for providing service-level authorization for Hadoop.
 */
public class ConfiguredPolicy extends Policy implements Configurable {
  public static final String HADOOP_POLICY_FILE = "raptor-policy.xml";
  private static final Logger LOG = Logger.getLogger(ConfiguredPolicy.class);
      
  private Configuration conf;
  private PolicyProvider policyProvider;
  private volatile Map<Principal, Set<Permission>> permissions;
  private volatile Set<Permission> allowedPermissions;

  public ConfiguredPolicy(Configuration conf, PolicyProvider policyProvider) {
    this.conf = conf;      
    this.policyProvider = policyProvider;
    refresh();
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
    refresh();
  }

  @Override
  public boolean implies(ProtectionDomain domain, Permission permission) {
    // Only make checks for domains having principals 
    if(domain.getPrincipals().length == 0) {
      return true; 
    }

    return super.implies(domain, permission);
  }

  @Override
  public PermissionCollection getPermissions(ProtectionDomain domain) {
    PermissionCollection permissionCollection = super.getPermissions(domain);
    for (Principal principal : domain.getPrincipals()) {
      Set<Permission> principalPermissions = permissions.get(principal);
      if (principalPermissions != null) {
        for (Permission permission : principalPermissions) {
          permissionCollection.add(permission);
        }
      }

      for (Permission permission : allowedPermissions) {
        permissionCollection.add(permission);
      }
    }
    return permissionCollection;
  }

  @Override
  public void refresh() {
    // Get the system property 'raptor.policy.file'
    String policyFile = 
      System.getProperty("raptor.policy.file", HADOOP_POLICY_FILE);
    
    // Make a copy of the original config, and load the policy file
    Configuration policyConf = ConfigurationFactory.create(conf);
    policyConf.addResource(policyFile);
    
    Map<Principal, Set<Permission>> newPermissions = 
      new HashMap<Principal, Set<Permission>>();
    Set<Permission> newAllowPermissions = new HashSet<Permission>();

    // Parse the config file
    Service[] services = policyProvider.getServices();
    if (services != null) {
      for (Service service : services) {
        AccessControlList acl = 
          new AccessControlList(
              policyConf.get(service.getServiceKey(), 
                             AccessControlList.WILDCARD_ACL_VALUE)
              );
        
        if (acl.allAllowed()) {
          newAllowPermissions.add(service.getPermission());
          if (LOG.isDebugEnabled()) {
            LOG.debug("Policy - " + service.getPermission() + " * ");
          }
        } else {
          for (String user : acl.getUsers()) {
            addPermission(newPermissions, new User(user), service.getPermission());
          }

          for (String group : acl.getGroups()) {
            addPermission(newPermissions, new Group(group), service.getPermission());
          }
        }
      }
    }

    // Flip to the newly parsed permissions
    allowedPermissions = newAllowPermissions;
    permissions = newPermissions;
  }

  private void addPermission(Map<Principal, Set<Permission>> permissions,
                             Principal principal, Permission permission) {
    Set<Permission> principalPermissions = permissions.get(principal);
    if (principalPermissions == null) {
      principalPermissions = new HashSet<Permission>();
      permissions.put(principal, principalPermissions);
    }
    principalPermissions.add(permission);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Policy - Adding  " + permission + " to " + principal);
    }
  }
}
