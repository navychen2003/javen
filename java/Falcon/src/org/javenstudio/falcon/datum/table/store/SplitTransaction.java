package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;

/**
 * Executes region split as a "transaction".  Call {@link #prepare()} to setup
 * the transaction, {@link #execute(OnlineRegions)} to run the transaction and
 * {@link #rollback(OnlineRegions)} to cleanup if execute fails.
 *
 * <p>Here is an example of how you would use this class:
 * <pre>
 *  SplitTransaction st = new SplitTransaction(this.conf, parent, midKey)
 *  if (!st.prepare()) return;
 *  try {
 *    st.execute(myOnlineRegions);
 *  } catch (IOException ioe) {
 *    try {
 *      st.rollback(myOnlineRegions);
 *      return;
 *    } catch (RuntimeException e) {
 *      myAbortable.abort("Failed split, abort");
 *    }
 *  }
 * </Pre>
 * <p>This class is not thread safe.  Caller needs ensure split is run by
 * one thread only.
 */
class SplitTransaction {
  private static final Logger LOG = Logger.getLogger(SplitTransaction.class);
  
  private static final String SPLITDIR = "splits";

  /**
   * Region to split
   */
  private final DBRegion mParent;
  private DBRegionInfo mHriA;
  private DBRegionInfo mHriB;
  private Path mSplitdir;

  /**
   * Row to split around
   */
  private final byte[] mSplitrow;

  /**
   * Types to add to the transaction journal
   */
  enum JournalEntry {
    /**
     * We created the temporary split data directory.
     */
    CREATE_SPLIT_DIR,
    /**
     * Closed the parent region.
     */
    CLOSED_PARENT_REGION,
    /**
     * The parent has been taken out of the server's online regions list.
     */
    OFFLINED_PARENT,
    /**
     * Started in on creation of the first daughter region.
     */
    STARTED_REGION_A_CREATION,
    /**
     * Started in on the creation of the second daughter region.
     */
    STARTED_REGION_B_CREATION
  }

  /**
   * Journal of how far the split transaction has progressed.
   */
  private final List<JournalEntry> mJournal = new ArrayList<JournalEntry>();

  /**
   * Constructor
   * @param c Configuration to use running split
   * @param r Region to split
   * @param splitrow Row to split around
   */
  SplitTransaction(final DBRegion r, final byte[] splitrow) {
    this.mParent = r;
    this.mSplitrow = splitrow;
    this.mSplitdir = getSplitDir(this.mParent);
  }

  /**
   * Does checks on split inputs.
   * @return <code>true</code> if the region is splittable else
   * <code>false</code> if it is not (e.g. its already closed, etc.).
   */
  public boolean prepare() {
    if (this.mParent.isClosed() || this.mParent.isClosing()) 
      return false;
    
    DBRegionInfo hri = this.mParent.getRegionInfo();
    
    // Check splitrow.
    byte[] startKey = hri.getStartKey();
    byte[] endKey = hri.getEndKey();
    
    if (Bytes.equals(startKey, mSplitrow) ||
        !this.mParent.getRegionInfo().containsRow(mSplitrow)) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Split row is not inside region key range or is equal to " +
          "startkey: " + Bytes.toString(this.mSplitrow));
      }
      return false;
    }
    
    long rid = getDaughterRegionIdTimestamp(hri);
    
    this.mHriA = new DBRegionInfo(hri.getTableDesc(), startKey, this.mSplitrow,
      false, rid);
    this.mHriB = new DBRegionInfo(hri.getTableDesc(), this.mSplitrow, endKey,
      false, rid);
    
    return true;
  }

  /**
   * Calculate daughter regionid to use.
   * @param hri Parent {@link DBRegionInfo}
   * @return Daughter region id (timestamp) to use.
   */
  private static long getDaughterRegionIdTimestamp(final DBRegionInfo hri) {
    long rid = System.currentTimeMillis(); 
    
    // Regionid is timestamp.  Can't be less than that of parent else will insert
    // at wrong location in .META. (See HBASE-710).
    if (rid < hri.getRegionId()) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Clock skew; parent regions id is " + hri.getRegionId() +
          " but current time here is " + rid);
      }
      rid = hri.getRegionId() + 1;
    }
    
    return rid;
  }

  /**
   * Run the transaction.
   * @param or Object that can online/offline parent region.
   * @throws IOException If thrown, transaction failed. Call {@link #rollback(OnlineRegions)}
   * @return Regions created
   * @see #rollback(OnlineRegions)
   */
  public PairOfSameType<DBRegion> execute(final OnlineRegions or) throws IOException {
    return execute(or, or != null);
  }

  /**
   * Run the transaction.
   * @param or Object that can online/offline parent region.  Can be null (Tests
   * will pass null).
   * @param If <code>true</code>, update meta (set to false when testing).
   * @throws IOException If thrown, transaction failed. Call {@link #rollback(OnlineRegions)}
   * @return Regions created
   * @see #rollback(OnlineRegions)
   */
  protected PairOfSameType<DBRegion> execute(final OnlineRegions or, 
		  final boolean updateMeta) throws IOException {
	if (LOG.isInfoEnabled())
      LOG.info("Starting split of region " + this.mParent);
	
    assert !this.mParent.mLock.writeLock().isHeldByCurrentThread() : "Unsafe to hold write lock while performing RPCs";

    // We'll need one of these later but get it now because if we fail there
    // is nothing to undo.
    DBTableInterface t = null;
    if (updateMeta) t = getTable(this.mParent.getConf());

    createSplitDir(this.mParent.getFilesystem(), this.mSplitdir);
    this.mJournal.add(JournalEntry.CREATE_SPLIT_DIR);

    List<StoreFile> hstoreFilesToSplit = this.mParent.close(false);
    this.mJournal.add(JournalEntry.CLOSED_PARENT_REGION);

    if (or != null) or.removeFromOnlineRegions(this.mParent.getRegionInfo());
    this.mJournal.add(JournalEntry.OFFLINED_PARENT);

    splitStoreFiles(this.mSplitdir, hstoreFilesToSplit);
    
    // splitStoreFiles creates daughter region dirs under the parent splits dir
    // Nothing to unroll here if failure -- clean up of CREATE_SPLIT_DIR will
    // clean this up.

    // Log to the journal that we are creating region A, the first daughter
    // region.  We could fail halfway through.  If we do, we could have left
    // stuff in fs that needs cleanup -- a storefile or two.  Thats why we
    // add entry to journal BEFORE rather than AFTER the change.
    this.mJournal.add(JournalEntry.STARTED_REGION_A_CREATION);
    DBRegion a = createDaughterRegion(this.mHriA);

    // Ditto
    this.mJournal.add(JournalEntry.STARTED_REGION_B_CREATION);
    DBRegion b = createDaughterRegion(this.mHriB);

    Put editParentPut = createOfflineParentPut();
    if (t != null) t.put(editParentPut);

    // The is the point of no return.  We are committed to the split now.  Up to
    // a failure editing parent in meta or a crash of the hosting regionserver,
    // we could rollback (or, if crash, we could cleanup on redeploy) but now
    // meta has been changed, we can only go forward.  If the below last steps
    // do not complete, repair has to be done by another agent.  For example,
    // basescanner, at least up till master rewrite, would add daughter rows if
    // missing from meta.  It could do this because the parent edit includes the
    // daughter specs.  In Bigtable paper, they have another mechanism where
    // some feedback to the master somehow flags it that split is incomplete and
    // needs fixup.  Whatever the mechanism, its a TODO that we have some fixup.
    
    // I looked at writing the put of the parent edit above out to the WAL log
    // before changing meta with the notion that should we fail, then on replay
    // the offlining of the parent and addition of daughters up into meta could
    // be reinserted.  The edits would have to be 'special' and given how our
    // splits work, splitting by region, I think the replay would have to happen
    // inside in the split code -- as soon as it saw one of these special edits,
    // rather than write the edit out a file for the .META. region to replay or
    // somehow, write it out to this regions edits file for it to handle on
    // redeploy -- this'd be whacky, we'd be telling meta about a split during
    // the deploy of the parent -- instead we'd have to play the edit inside
    // in the split code somehow; this would involve a stop-the-splitting till
    // meta had been edited which might hold up splitting a good while.

    // Finish up the meta edits.  If these fail, another agent needs to do fixup
    DBRegionInfo hri = this.mHriA;
    try {
      if (t != null) t.put(createDaughterPut(hri));
      hri = this.mHriB;
      if (t != null) t.put(createDaughterPut(hri));
    } catch (IOException e) {
      // Don't let this out or we'll run rollback.
      if (LOG.isWarnEnabled())
        LOG.warn("Failed adding daughter " + hri.toString());
    }
    
    // This should not fail because the DBTable instance we are using is not
    // running a buffer -- its immediately flushing its puts.
    if (t != null) t.close();

    // Leaving here, the splitdir with its dross will be in place but since the
    // split was successful, just leave it; it'll be cleaned when parent is
    // deleted and cleaned up.
    return new PairOfSameType<DBRegion>(a, b);
  }

  private static Path getSplitDir(final DBRegion r) {
    return new Path(r.getRegionDir(), SPLITDIR);
  }

  /**
   * @param fs Filesystem to use
   * @param splitdir Directory to store temporary split data in
   * @throws IOException If <code>splitdir</code> already exists or we fail
   * to create it.
   * @see #cleanupSplitDir(FileSystem, Path)
   */
  private static void createSplitDir(final FileSystem fs, final Path splitdir)
      throws IOException {
    if (fs.exists(splitdir)) throw new IOException("Splitdir already exits? " + splitdir);
    if (!fs.mkdirs(splitdir)) throw new IOException("Failed create of " + splitdir);
  }

  private static void cleanupSplitDir(final FileSystem fs, final Path splitdir)
      throws IOException {
    // Splitdir may have been cleaned up by reopen of the parent dir.
    deleteDir(fs, splitdir, false);
  }

  /**
   * @param fs Filesystem to use
   * @param dir Directory to delete
   * @param mustPreExist If true, we'll throw exception if <code>dir</code>
   * does not preexist, else we'll just pass.
   * @throws IOException Thrown if we fail to delete passed <code>dir</code>
   */
  private static void deleteDir(final FileSystem fs, final Path dir,
      final boolean mustPreExist) throws IOException {
    if (!fs.exists(dir)) {
      if (mustPreExist) throw new IOException(dir.toString() + " does not exist!");
    } else if (!fs.delete(dir, true)) {
      throw new IOException("Failed delete of " + dir);
    }
  }

  private void splitStoreFiles(final Path splitdir,
    final List<StoreFile> hstoreFilesToSplit) throws IOException {
    if (hstoreFilesToSplit == null) {
      // Could be null because close didn't succeed -- for now consider it fatal
      throw new IOException("Close returned empty list of StoreFiles");
    }

    // Split each store file.
    for (StoreFile sf : hstoreFilesToSplit) {
      splitStoreFile(sf, splitdir);
    }
  }

  private void splitStoreFile(final StoreFile sf, final Path splitdir)
      throws IOException {
    FileSystem fs = this.mParent.getFilesystem();
    byte[] family = sf.getFamily();
    String encoded = this.mHriA.getEncodedName();
    Path storedir = Store.getStoreHomedir(splitdir, encoded, family);
    StoreFile.split(fs, storedir, sf, this.mSplitrow, Reference.Range.bottom);
    encoded = this.mHriB.getEncodedName();
    storedir = Store.getStoreHomedir(splitdir, encoded, family);
    StoreFile.split(fs, storedir, sf, this.mSplitrow, Reference.Range.top);
  }

  /**
   * @param hri
   * @return Created daughter DBRegion.
   * @throws IOException
   * @see #cleanupDaughterRegion(FileSystem, Path, DBRegionInfo)
   */
  protected DBRegion createDaughterRegion(final DBRegionInfo hri)
      throws IOException {
    // Package private so unit tests have access.
    FileSystem fs = this.mParent.getFilesystem();
    Path regionDir = getSplitDirForDaughter(this.mParent.getFilesystem(),
      this.mSplitdir, hri);
    DBRegion r = DBRegion.newDBRegion(this.mParent.getTableDir(),
      this.mParent.getLog(), fs, this.mParent.getConf(),
      hri, null);
    DBRegion.moveInitialFilesIntoPlace(fs, regionDir, r.getRegionDir());
    return r;
  }

  private static void cleanupDaughterRegion(final FileSystem fs,
    final Path tabledir, final String encodedName) throws IOException {
    Path regiondir = DBRegion.getRegionDir(tabledir, encodedName);
    // Dir may not preexist.
    deleteDir(fs, regiondir, false);
  }

  /**
   * Get the daughter directories in the splits dir.  The splits dir is under
   * the parent regions' directory.
   * @param fs
   * @param splitdir
   * @param hri
   * @return Path to daughter split dir.
   * @throws IOException
   */
  private static Path getSplitDirForDaughter(final FileSystem fs,
      final Path splitdir, final DBRegionInfo hri) throws IOException {
    return new Path(splitdir, hri.getEncodedName());
  }

  /**
   * @param r Parent region we want to edit.
   * @return An DBTable instance against the meta table that holds passed
   * <code>r</code>; it has autoFlush enabled so we immediately send puts (No
   * buffering enabled).
   * @throws IOException
   */
  private DBTableInterface getTable(final Configuration conf) throws IOException {
    // When a region is split, the META table needs to updated if we're
    // splitting a 'normal' region, and the ROOT table needs to be
    // updated if we are splitting a META region.
    //DBTableInterface t = null;
    if (this.mParent.getRegionInfo().isMetaTable()) {
      //t = new DBTable(conf, DBConstants.ROOT_TABLE_NAME);
    } else {
      //t = new DBTable(conf, DBConstants.META_TABLE_NAME);
    }
    // Flush puts as we send them -- no buffering.
    //t.setAutoFlush(true);
    throw new NullPointerException();
    //return t;
  }

  private Put createOfflineParentPut() throws IOException  {
    DBRegionInfo editedParentRegionInfo =
      new DBRegionInfo(this.mParent.getRegionInfo());
    editedParentRegionInfo.setOffline(true);
    editedParentRegionInfo.setSplit(true);
    Put put = new Put(editedParentRegionInfo.getRegionName());
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER,
      Writables.getBytes(editedParentRegionInfo));
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.SERVER_QUALIFIER,
        DBConstants.EMPTY_BYTE_ARRAY);
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.STARTCODE_QUALIFIER,
        DBConstants.EMPTY_BYTE_ARRAY);
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.SPLITA_QUALIFIER,
      Writables.getBytes(this.mHriA));
    put.add(DBConstants.CATALOG_FAMILY, DBConstants.SPLITB_QUALIFIER,
      Writables.getBytes(this.mHriB));
    return put;
  }

  private Put createDaughterPut(final DBRegionInfo daughter)
      throws IOException {
    Put p = new Put(daughter.getRegionName());
    p.add(DBConstants.CATALOG_FAMILY, DBConstants.REGIONINFO_QUALIFIER,
      Writables.getBytes(daughter));
    return p;
  }

  /**
   * @param or Object that can online/offline parent region.  Can be passed null
   * by unit tests.
   * @return The region we were splitting
   * @throws IOException If thrown, rollback failed.  Take drastic action.
   */
  public void rollback(final OnlineRegions or) throws IOException {
    FileSystem fs = this.mParent.getFilesystem();
    ListIterator<JournalEntry> iterator =
      this.mJournal.listIterator(this.mJournal.size());
    
    while (iterator.hasPrevious()) {
      JournalEntry je = iterator.previous();
      switch(je) {
      case CREATE_SPLIT_DIR:
        cleanupSplitDir(fs, this.mSplitdir);
        break;

      case CLOSED_PARENT_REGION:
        // So, this returns a seqid but if we just closed and then reopened, we
        // should be ok. On close, we flushed using sequenceid obtained from
        // hosting regionserver so no need to propagate the sequenceid returned
        // out of initialize below up into regionserver as we normally do.
        // TODO: Verify.
        this.mParent.initialize();
        break;

      case STARTED_REGION_A_CREATION:
        cleanupDaughterRegion(fs, this.mParent.getTableDir(),
          this.mHriA.getEncodedName());
        break;

      case STARTED_REGION_B_CREATION:
        cleanupDaughterRegion(fs, this.mParent.getTableDir(),
          this.mHriB.getEncodedName());
        break;

      case OFFLINED_PARENT:
        if (or != null) or.addToOnlineRegions(this.mParent);
        break;

      default:
        throw new RuntimeException("Unhandled journal entry: " + je);
      }
    }
  }

  protected DBRegionInfo getFirstDaughter() {
    return mHriA;
  }

  protected DBRegionInfo getSecondDaughter() {
    return mHriB;
  }

  // For unit testing.
  protected Path getSplitDir() {
    return this.mSplitdir;
  }

  /**
   * Clean up any split detritus that may have been left around from previous
   * split attempts.
   * Call this method on initial region deploy.  Cleans up any mess
   * left by previous deploys of passed <code>r</code> region.
   * @param r
   * @throws IOException 
   */
  static void cleanupAnySplitDetritus(final DBRegion r) throws IOException {
    Path splitdir = getSplitDir(r);
    FileSystem fs = r.getFilesystem();
    if (!fs.exists(splitdir)) return;
    
    // Look at the splitdir.  It could have the encoded names of the daughter
    // regions we tried to make.  See if the daughter regions actually got made
    // out under the tabledir.  If here under splitdir still, then the split did
    // not complete.  Try and do cleanup.  This code WILL NOT catch the case
    // where we successfully created daughter a but regionserver crashed during
    // the creation of region b.  In this case, there'll be an orphan daughter
    // dir in the filesystem.  TOOD: Fix.
    FileStatus [] daughters = fs.listStatus(splitdir, new DBFile.DirFilter(fs));
    
    for (int i = 0; i < daughters.length; i++) {
      cleanupDaughterRegion(fs, r.getTableDir(),
        daughters[i].getPath().getName());
    }
    
    cleanupSplitDir(r.getFilesystem(), splitdir);
    
    if (LOG.isInfoEnabled())
      LOG.info("Cleaned up old failed split transaction detritus: " + splitdir);
  }
}
