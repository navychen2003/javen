package org.javenstudio.raptor.fs.permission;

import org.javenstudio.raptor.io.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * Store permission related information.
 */
public class PermissionStatus implements Writable {
  static final WritableFactory FACTORY = new WritableFactory() {
    public Writable newInstance() { return new PermissionStatus(); }
  };
  static {                                      // register a ctor
    WritableFactories.setFactory(PermissionStatus.class, FACTORY);
  }

  /** Create an immutable {@link PermissionStatus} object. */
  public static PermissionStatus createImmutable(
      String user, String group, FsPermission permission) {
    return new PermissionStatus(user, group, permission) {
      public PermissionStatus applyUMask(FsPermission umask) {
        throw new UnsupportedOperationException();
      }
      public void readFields(DataInput in) throws IOException {
        throw new UnsupportedOperationException();
      }
    };
  }

  private String username;
  private String groupname;
  private FsPermission permission;

  private PermissionStatus() {}

  /** Constructor */
  public PermissionStatus(String user, String group, FsPermission permission) {
    username = user;
    groupname = group;
    this.permission = permission;
  }

  /** Return user name */
  public String getUserName() {return username;}

  /** Return group name */
  public String getGroupName() {return groupname;}

  /** Return permission */
  public FsPermission getPermission() {return permission;}

  /**
   * Apply umask.
   * @see FsPermission#applyUMask(FsPermission)
   */
  public PermissionStatus applyUMask(FsPermission umask) {
    permission = permission.applyUMask(umask);
    return this;
  }

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    username = Text.readString(in);
    groupname = Text.readString(in);
    permission = FsPermission.read(in);
  }

  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    write(out, username, groupname, permission);
  }

  /**
   * Create and initialize a {@link PermissionStatus} from {@link DataInput}.
   */
  public static PermissionStatus read(DataInput in) throws IOException {
    PermissionStatus p = new PermissionStatus();
    p.readFields(in);
    return p;
  }

  /**
   * Serialize a {@link PermissionStatus} from its base components.
   */
  public static void write(DataOutput out,
                           String username, 
                           String groupname,
                           FsPermission permission) throws IOException {
    Text.writeString(out, username);
    Text.writeString(out, groupname);
    permission.write(out);
  }

  /** {@inheritDoc} */
  public String toString() {
    return username + ":" + groupname + ":" + permission;
  }
}
