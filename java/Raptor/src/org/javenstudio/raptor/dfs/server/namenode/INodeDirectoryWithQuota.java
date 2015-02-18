package org.javenstudio.raptor.dfs.server.namenode;

import org.javenstudio.raptor.fs.permission.PermissionStatus;
import org.javenstudio.raptor.dfs.protocol.DSQuotaExceededException;
import org.javenstudio.raptor.dfs.protocol.NSQuotaExceededException;
import org.javenstudio.raptor.dfs.protocol.QuotaExceededException;

/**
 * Directory INode class that has a quota restriction
 */
class INodeDirectoryWithQuota extends INodeDirectory {
  private long nsQuota; /// NameSpace quota
  private long nsCount;
  private long dsQuota; /// disk space quota
  private long diskspace;
  
  /** Convert an existing directory inode to one with the given quota
   * 
   * @param nsQuota Namespace quota to be assigned to this inode
   * @param dsQuota Diskspace quota to be assigned to this indoe
   * @param other The other inode from which all other properties are copied
   */
  INodeDirectoryWithQuota(long nsQuota, long dsQuota, INodeDirectory other)
  throws QuotaExceededException {
    super(other);
    INode.DirCounts counts = new INode.DirCounts();
    other.spaceConsumedInTree(counts);
    this.nsCount= counts.getNsCount();
    this.diskspace = counts.getDsCount();
    setQuota(nsQuota, dsQuota);
  }
  
  /** constructor with no quota verification */
  INodeDirectoryWithQuota(
      PermissionStatus permissions, long modificationTime, 
      long nsQuota, long dsQuota)
  {
    super(permissions, modificationTime);
    this.nsQuota = nsQuota;
    this.dsQuota = dsQuota;
    this.nsCount = 1;
  }
  
  /** constructor with no quota verification */
  INodeDirectoryWithQuota(String name, PermissionStatus permissions, 
                          long nsQuota, long dsQuota)
  {
    super(name, permissions);
    this.nsQuota = nsQuota;
    this.dsQuota = dsQuota;
    this.nsCount = 1;
  }
  
  /** Get this directory's namespace quota
   * @return this directory's namespace quota
   */
  long getNsQuota() {
    return nsQuota;
  }
  
  /** Get this directory's diskspace quota
   * @return this directory's diskspace quota
   */
  long getDsQuota() {
    return dsQuota;
  }
  
  /** Set this directory's quota
   * 
   * @param nsQuota Namespace quota to be set
   * @param dsQuota diskspace quota to be set
   *                                
   */
  void setQuota(long newNsQuota, long newDsQuota) throws QuotaExceededException {
    nsQuota = newNsQuota;
    dsQuota = newDsQuota;
  }
  
  
  @Override
  DirCounts spaceConsumedInTree(DirCounts counts) {
    counts.nsCount += nsCount;
    counts.dsCount += diskspace;
    return counts;
  }

  /** Get the number of names in the subtree rooted at this directory
   * @return the size of the subtree rooted at this directory
   */
  long numItemsInTree() {
    return nsCount;
  }
  
  long diskspaceConsumed() {
    return diskspace;
  }
  
  /** Update the size of the tree
   * 
   * @param nsDelta the change of the tree size
   * @param dsDelta change to disk space occupied
   * @throws QuotaExceededException if the changed size is greater 
   *                                than the quota
   */
  void updateNumItemsInTree(long nsDelta, long dsDelta) throws 
                            QuotaExceededException {
    long newCount = nsCount + nsDelta;
    long newDiskspace = diskspace + dsDelta;
    if (nsDelta>0 || dsDelta>0) {
      verifyQuota(nsQuota, newCount, dsQuota, newDiskspace);
    }
    nsCount = newCount;
    diskspace = newDiskspace;
  }
  
  /** 
   * Sets namespace and diskspace take by the directory rooted 
   * at this INode. This should be used carefully. It does not check 
   * for quota violations.
   * 
   * @param namespace size of the directory to be set
   * @param diskspace disk space take by all the nodes under this directory
   */
  void setSpaceConsumed(long namespace, long diskspace) {
    this.nsCount = namespace;
    this.diskspace = diskspace;
  }
  
  /** Verify if the namespace count disk space satisfies the quota restriction 
   * @throws QuotaExceededException if the given quota is less than the count
   */
  private static void verifyQuota(long nsQuota, long nsCount, 
                                  long dsQuota, long diskspace)
                                  throws QuotaExceededException {
    if (nsQuota >= 0 && nsQuota < nsCount) {
      throw new NSQuotaExceededException(nsQuota, nsCount);
    }
    if (dsQuota >= 0 && dsQuota < diskspace) {
      throw new DSQuotaExceededException(dsQuota, diskspace);
    }
  }
}

