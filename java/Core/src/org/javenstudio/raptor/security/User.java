package org.javenstudio.raptor.security;

import java.security.Principal;


/**
 * The username of a user.
 */
public class User implements Principal {
  final String user;

  /**
   * Create a new <code>User</code> with the given username.
   * @param user user name
   */
  public User(String user) {
    this.user = user;
  }
  
  @Override
  public String getName() {
    return user;
  }

  @Override
  public String toString() {
    return user;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((user == null) ? 0 : user.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    User other = (User) obj;
    if (user == null) {
      if (other.user != null)
        return false;
    } else if (!user.equals(other.user))
      return false;
    return true;
  }
}
