package org.javenstudio.raptor.dfs.server.namenode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.net.NetUtils;
import org.javenstudio.raptor.net.NodeBase;
import org.javenstudio.raptor.dfs.DFSClient;
import org.javenstudio.raptor.dfs.protocol.Block;
import org.javenstudio.raptor.dfs.protocol.DatanodeInfo;
import org.javenstudio.raptor.dfs.protocol.LocatedBlock;
import org.javenstudio.raptor.dfs.protocol.LocatedBlocks;
import org.javenstudio.raptor.dfs.protocol.FSConstants.DatanodeReportType;
import org.javenstudio.raptor.dfs.server.common.DfsConstants;
import org.javenstudio.raptor.fs.FileStatus;
import org.javenstudio.raptor.fs.permission.PermissionStatus;

/**
 * This class provides rudimentary checking of DFS volumes for errors and
 * sub-optimal conditions.
 * <p>The tool scans all files and directories, starting from an indicated
 *  root path. The following abnormal conditions are detected and handled:</p>
 * <ul>
 * <li>files with blocks that are completely missing from all datanodes.<br/>
 * In this case the tool can perform one of the following actions:
 *  <ul>
 *      <li>none ({@link #FIXING_NONE})</li>
 *      <li>move corrupted files to /lost+found directory on DFS
 *      ({@link #FIXING_MOVE}). Remaining data blocks are saved as a
 *      block chains, representing longest consecutive series of valid blocks.</li>
 *      <li>delete corrupted files ({@link #FIXING_DELETE})</li>
 *  </ul>
 *  </li>
 *  <li>detect files with under-replicated or over-replicated blocks</li>
 *  </ul>
 *  Additionally, the tool collects a detailed overall DFS statistics, and
 *  optionally can print detailed statistics on block locations and replication
 *  factors of each file.
 */
public class NamenodeFsck {
  public static final Logger LOG = Logger.getLogger(NameNode.class);
  
  // return string marking fsck status
  public static final String CORRUPT_STATUS = "is CORRUPT";
  public static final String HEALTHY_STATUS = "is HEALTHY";
  public static final String NONEXISTENT_STATUS = "does not exist";
  public static final String FAILURE_STATUS = "FAILED";
  
  /** Don't attempt any fixing . */
  public static final int FIXING_NONE = 0;
  /** Move corrupted files to /lost+found . */
  public static final int FIXING_MOVE = 1;
  /** Delete corrupted files. */
  public static final int FIXING_DELETE = 2;
  
  private NameNode nn;
  private String lostFound = null;
  private boolean lfInited = false;
  private boolean lfInitedOk = false;
  private boolean showFiles = false;
  private boolean showOpenFiles = false;
  private boolean showBlocks = false;
  private boolean showLocations = false;
  private boolean showRacks = false;
  private int fixing = FIXING_NONE;
  private String path = "/";
  
  private Configuration conf;
  private PrintWriter out;
  
  /**
   * Filesystem checker.
   * @param conf configuration (namenode config)
   * @param nn namenode that this fsck is going to use
   * @param pmap key=value[] map that is passed to the http servlet as url parameters
   * @param response the object into which  this servelet writes the url contents
   * @throws IOException
   */
  public NamenodeFsck(Configuration conf,
                      NameNode nn,
                      Map<String,String[]> pmap, 
                      PrintWriter writer) throws IOException {
    this.conf = conf;
    this.nn = nn;
    this.out = writer; //response.getWriter();
    for (Iterator<String> it = pmap.keySet().iterator(); it.hasNext();) {
      String key = it.next();
      if (key.equals("path")) { this.path = pmap.get("path")[0]; }
      else if (key.equals("move")) { this.fixing = FIXING_MOVE; }
      else if (key.equals("delete")) { this.fixing = FIXING_DELETE; }
      else if (key.equals("files")) { this.showFiles = true; }
      else if (key.equals("blocks")) { this.showBlocks = true; }
      else if (key.equals("locations")) { this.showLocations = true; }
      else if (key.equals("racks")) { this.showRacks = true; }
      else if (key.equals("openforwrite")) {this.showOpenFiles = true; }
    }
  }
  
  public static String fsckPath(NameNode namenode, 
	  String path, String params) throws IOException { 
	Map<String,String[]> pmap = parseParameterMap(params);
	pmap.put("path", new String[]{ path });
	
	StringWriter sbuf = new StringWriter();
	PrintWriter writer = new PrintWriter(sbuf);
	
	NamenodeFsck fsck = new NamenodeFsck(namenode.getConf(), 
			namenode, pmap, writer);
	
	fsck.fsck();
	writer.flush();
	
	return sbuf.toString();
  }
  
  private static Map<String,String[]> parseParameterMap(String params) { 
	Map<String,String[]> map = new HashMap<String,String[]>();
	if (params != null) { 
		StringTokenizer st = new StringTokenizer(params, "&");
		while (st.hasMoreTokens()) { 
			String param = st.nextToken();
			if (param != null && param.length() > 0) { 
				int pos = param.indexOf('=');
				if (pos > 0) { 
					String name = param.substring(0, pos); 
					String value = param.substring(pos + 1);
					
					if (name != null && name.length() > 0) {
						String[] values = map.get(name);
						if (values == null) { 
							map.put(name, new String[]{value});
						} else { 
							String[] values2 = new String[values.length+1];
							System.arraycopy(values2, 0, values2, 0, values.length);
							values2[values2.length-1] = value;
							map.put(name, values2);
						}
					}
				}
			}
		}
	}
	return map;
  }
  
  /**
   * Check files on DFS, starting from the indicated path.
   * @throws Exception
   */
  public void fsck() throws IOException {
    try {
      FileStatus[] files = nn.getNamesystem().dir.getListing(path);
      FsckResult res = new FsckResult();
      res.totalRacks = nn.getNetworkTopology().getNumOfRacks();
      res.totalDatanodes = nn.getNamesystem().getNumberOfDatanodes(
          DatanodeReportType.LIVE);
      res.setReplication((short) conf.getInt("dfs.replication", 3));
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          check(files[i], res);
        }
        out.println(res);
        // DFSck client scans for the string HEALTHY/CORRUPT to check the status
        // of file system and return appropriate code. Changing the output string
        // might break testcases. 
        if (res.isHealthy()) {
          out.print("\n\nThe filesystem under path '" + path + "' " + HEALTHY_STATUS);
        }  else {
          out.print("\n\nThe filesystem under path '" + path + "' " + CORRUPT_STATUS);
        }
      } else {
        out.print("\n\nPath '" + path + "' " + NONEXISTENT_STATUS);
      }
    } catch (Exception e) {
      String errMsg = "Fsck on path '" + path + "' " + FAILURE_STATUS;
      LOG.warn(errMsg, e);
      out.println(e.getMessage());
      out.print("\n\n"+errMsg);
    } finally {
      out.close();
    }
  }
  
  private void check(FileStatus file, FsckResult res) throws IOException {
    int minReplication = nn.getNamesystem().getMinReplication();
    String path = file.getPath().toString();
    boolean isOpen = false;

    if (file.isDir()) {
      FileStatus[] files = nn.getNamesystem().dir.getListing(path);
      if (files == null) {
        return;
      }
      if (showFiles) {
        out.println(path + " <dir>");
      }
      res.totalDirs++;
      for (int i = 0; i < files.length; i++) {
        check(files[i], res);
      }
      return;
    }
    long fileLen = file.getLen();
    LocatedBlocks blocks = nn.getNamesystem().getBlockLocations(path, 0, fileLen);
    if (blocks == null) { // the file is deleted
      return;
    }
    isOpen = blocks.isUnderConstruction();
    if (isOpen && !showOpenFiles) {
      // We collect these stats about open files to report with default options
      res.totalOpenFilesSize += fileLen;
      res.totalOpenFilesBlocks += blocks.locatedBlockCount();
      res.totalOpenFiles++;
      return;
    }
    res.totalFiles++;
    res.totalSize += fileLen;
    res.totalBlocks += blocks.locatedBlockCount();
    if (showOpenFiles && isOpen) {
      out.print(path + " " + fileLen + " bytes, " +
        blocks.locatedBlockCount() + " block(s), OPENFORWRITE: ");
    } else if (showFiles) {
      out.print(path + " " + fileLen + " bytes, " +
        blocks.locatedBlockCount() + " block(s): ");
    } else {
      out.print('.');
    }
    if (res.totalFiles % 100 == 0) { out.println(); out.flush(); }
    int missing = 0;
    int corrupt = 0;
    long missize = 0;
    int underReplicatedPerFile = 0;
    int misReplicatedPerFile = 0;
    StringBuffer report = new StringBuffer();
    int i = 0;
    for (LocatedBlock lBlk : blocks.getLocatedBlocks()) {
      Block block = lBlk.getBlock();
      boolean isCorrupt = lBlk.isCorrupt();
      String blkName = block.toString();
      DatanodeInfo[] locs = lBlk.getLocations();
      res.totalReplicas += locs.length;
      short targetFileReplication = file.getReplication();
      if (locs.length > targetFileReplication) {
        res.excessiveReplicas += (locs.length - targetFileReplication);
        res.numOverReplicatedBlocks += 1;
      }
      // Check if block is Corrupt
      if (isCorrupt) {
        corrupt++;
        res.corruptBlocks++;
        out.print("\n" + path + ": CORRUPT block " + block.getBlockName()+"\n");
      }
      if (locs.length >= minReplication)
        res.numMinReplicatedBlocks++;
      if (locs.length < targetFileReplication && locs.length > 0) {
        res.missingReplicas += (targetFileReplication - locs.length);
        res.numUnderReplicatedBlocks += 1;
        underReplicatedPerFile++;
        if (!showFiles) {
          out.print("\n" + path + ": ");
        }
        out.println(" Under replicated " + block +
                    ". Target Replicas is " +
                    targetFileReplication + " but found " +
                    locs.length + " replica(s).");
      }
      // verify block placement policy
      int missingRacks = ReplicationTargetChooser.verifyBlockPlacement(
                    lBlk, targetFileReplication, nn.getNetworkTopology());
      if (missingRacks > 0) {
        res.numMisReplicatedBlocks++;
        misReplicatedPerFile++;
        if (!showFiles) {
          if(underReplicatedPerFile == 0)
            out.println();
          out.print(path + ": ");
        }
        out.println(" Replica placement policy is violated for " + 
                    block +
                    ". Block should be additionally replicated on " + 
                    missingRacks + " more rack(s).");
      }
      report.append(i + ". " + blkName + " len=" + block.getNumBytes());
      if (locs.length == 0) {
        report.append(" MISSING!");
        res.addMissing(block.toString(), block.getNumBytes());
        missing++;
        missize += block.getNumBytes();
      } else {
        report.append(" repl=" + locs.length);
        if (showLocations || showRacks) {
          StringBuffer sb = new StringBuffer("[");
          for (int j = 0; j < locs.length; j++) {
            if (j > 0) { sb.append(", "); }
            if (showRacks)
              sb.append(NodeBase.getPath(locs[j]));
            else
              sb.append(locs[j]);
          }
          sb.append(']');
          report.append(" " + sb.toString());
        }
      }
      report.append('\n');
      i++;
    }
    if ((missing > 0) || (corrupt > 0)) {
      if (!showFiles && (missing > 0)) {
        out.print("\n" + path + ": MISSING " + missing
            + " blocks of total size " + missize + " B.");
      }
      res.corruptFiles++;
      switch(fixing) {
      case FIXING_NONE:
        break;
      case FIXING_MOVE:
        if (!isOpen)
          lostFoundMove(file, blocks);
        break;
      case FIXING_DELETE:
        if (!isOpen)
          nn.getNamesystem().deleteInternal(path, false);
      }
    }
    if (showFiles) {
      if (missing > 0) {
        out.print(" MISSING " + missing + " blocks of total size " + missize + " B\n");
      }  else if (underReplicatedPerFile == 0 && misReplicatedPerFile == 0) {
        out.print(" OK\n");
      }
      if (showBlocks) {
        out.print(report.toString() + "\n");
      }
    }
  }
  
  private void lostFoundMove(FileStatus file, LocatedBlocks blocks)
    throws IOException {
    final DFSClient dfs = new DFSClient(NameNode.getAddress(conf), conf);
    try {
    if (!lfInited) {
      lostFoundInit(dfs);
    }
    if (!lfInitedOk) {
      return;
    }
    String target = lostFound + file.getPath();
    String errmsg = "Failed to move " + file.getPath() + " to /lost+found";
    try {
      PermissionStatus ps = new PermissionStatus(
          file.getOwner(), file.getGroup(), file.getPermission()); 
      if (!nn.getNamesystem().dir.mkdirs(target, ps, false, FSNamesystem.now())) {
        LOG.warn(errmsg);
        return;
      }
      // create chains
      int chain = 0;
      OutputStream fos = null;
      for (LocatedBlock lBlk : blocks.getLocatedBlocks()) {
        LocatedBlock lblock = lBlk;
        DatanodeInfo[] locs = lblock.getLocations();
        if (locs == null || locs.length == 0) {
          if (fos != null) {
            fos.flush();
            fos.close();
            fos = null;
          }
          continue;
        }
        if (fos == null) {
          fos = dfs.create(target + "/" + chain, true);
          if (fos != null) chain++;
          else {
            LOG.warn(errmsg + ": could not store chain " + chain);
            // perhaps we should bail out here...
            // return;
            continue;
          }
        }
        
        // copy the block. It's a pity it's not abstracted from DFSInputStream ...
        try {
          copyBlock(dfs, lblock, fos);
        } catch (Exception e) {
          e.printStackTrace();
          // something went wrong copying this block...
          LOG.warn(" - could not copy block " + lblock.getBlock() + " to " + target);
          fos.flush();
          fos.close();
          fos = null;
        }
      }
      if (fos != null) fos.close();
      LOG.warn("\n - moved corrupted file " + file.getPath() + " to /lost+found");
      dfs.delete(file.getPath().toString(), true);
    }  catch (Exception e) {
      e.printStackTrace();
      LOG.warn(errmsg + ": " + e.getMessage());
    }
    } finally {
      dfs.close();
    }
  }
      
  /**
   * XXX (ab) Bulk of this method is copied verbatim from {@link DFSClient}, which is
   * bad. Both places should be refactored to provide a method to copy blocks
   * around.
   */
  private void copyBlock(DFSClient dfs, LocatedBlock lblock,
                         OutputStream fos) throws Exception {
    int failures = 0;
    InetSocketAddress targetAddr = null;
    TreeSet<DatanodeInfo> deadNodes = new TreeSet<DatanodeInfo>();
    Socket s = null;
    DFSClient.BlockReader blockReader = null; 
    Block block = lblock.getBlock(); 

    while (s == null) {
      DatanodeInfo chosenNode;
      
      try {
        chosenNode = bestNode(dfs, lblock.getLocations(), deadNodes);
        targetAddr = NetUtils.createSocketAddr(chosenNode.getName());
      }  catch (IOException ie) {
        if (failures >= DFSClient.MAX_BLOCK_ACQUIRE_FAILURES) {
          throw new IOException("Could not obtain block " + lblock);
        }
        LOG.info("Could not obtain block from any node:  " + ie);
        try {
          Thread.sleep(10000);
        }  catch (InterruptedException iex) {
        }
        deadNodes.clear();
        failures++;
        continue;
      }
      try {
        s = new Socket();
        s.connect(targetAddr, DfsConstants.READ_TIMEOUT);
        s.setSoTimeout(DfsConstants.READ_TIMEOUT);
        
        blockReader = 
          DFSClient.BlockReader.newBlockReader(s, targetAddr.toString() + ":" + 
                                               block.getBlockId(), 
                                               block.getBlockId(), 
                                               block.getGenerationStamp(), 
                                               0, -1,
                                               conf.getInt("io.file.buffer.size", 4096));
        
      }  catch (IOException ex) {
        // Put chosen node into dead list, continue
        LOG.info("Failed to connect to " + targetAddr + ":" + ex);
        deadNodes.add(chosenNode);
        if (s != null) {
          try {
            s.close();
          } catch (IOException iex) {
          }
        }
        s = null;
      }
    }
    if (blockReader == null) {
      throw new Exception("Could not open data stream for " + lblock.getBlock());
    }
    byte[] buf = new byte[1024];
    int cnt = 0;
    boolean success = true;
    long bytesRead = 0;
    try {
      while ((cnt = blockReader.read(buf, 0, buf.length)) > 0) {
        fos.write(buf, 0, cnt);
        bytesRead += cnt;
      }
      if ( bytesRead != block.getNumBytes() ) {
        throw new IOException("Recorded block size is " + block.getNumBytes() + 
                              ", but datanode returned " +bytesRead+" bytes");
      }
    } catch (Exception e) {
      e.printStackTrace();
      success = false;
    } finally {
      try {s.close(); } catch (Exception e1) {}
    }
    if (!success)
      throw new Exception("Could not copy block data for " + lblock.getBlock());
  }
      
  /*
   * XXX (ab) See comment above for copyBlock().
   *
   * Pick the best node from which to stream the data.
   * That's the local one, if available.
   */
  Random r = new Random();
  private DatanodeInfo bestNode(DFSClient dfs, DatanodeInfo[] nodes,
                                TreeSet<DatanodeInfo> deadNodes) throws IOException {
    if ((nodes == null) ||
        (nodes.length - deadNodes.size() < 1)) {
      throw new IOException("No live nodes contain current block");
    }
    DatanodeInfo chosenNode;
    do {
      chosenNode = nodes[r.nextInt(nodes.length)];
    } while (deadNodes.contains(chosenNode));
    return chosenNode;
  }
  
  @SuppressWarnings("deprecation")
  private void lostFoundInit(DFSClient dfs) {
    lfInited = true;
    try {
      String lfName = "/lost+found";
      // check that /lost+found exists
      if (!dfs.exists(lfName)) {
        lfInitedOk = dfs.mkdirs(lfName);
        lostFound = lfName;
      } else        if (!dfs.isDirectory(lfName)) {
        LOG.warn("Cannot use /lost+found : a regular file with this name exists.");
        lfInitedOk = false;
      }  else { // exists and isDirectory
        lostFound = lfName;
        lfInitedOk = true;
      }
    }  catch (Exception e) {
      e.printStackTrace();
      lfInitedOk = false;
    }
    if (lostFound == null) {
      LOG.warn("Cannot initialize /lost+found .");
      lfInitedOk = false;
    }
  }
  
  /**
   * @param args
   */
  public int run(String[] args) throws Exception {
    
    return 0;
  }
  
  /**
   * FsckResult of checking, plus overall DFS statistics.
   *
   */
  public static class FsckResult {
    private ArrayList<String> missingIds = new ArrayList<String>();
    private long missingSize = 0L;
    private long corruptFiles = 0L;
    private long corruptBlocks = 0L;
    private long excessiveReplicas = 0L;
    private long missingReplicas = 0L;
    private long numOverReplicatedBlocks = 0L;
    private long numUnderReplicatedBlocks = 0L;
    private long numMisReplicatedBlocks = 0L;  // blocks that do not satisfy block placement policy
    private long numMinReplicatedBlocks = 0L;  // minimally replicatedblocks
    private int replication = 0;
    private long totalBlocks = 0L;
    private long totalOpenFilesBlocks = 0L;
    private long totalFiles = 0L;
    private long totalOpenFiles = 0L;
    private long totalDirs = 0L;
    private long totalSize = 0L;
    private long totalOpenFilesSize = 0L;
    private long totalReplicas = 0L;
    private int totalDatanodes = 0;
    private int totalRacks = 0;
    
    /**
     * DFS is considered healthy if there are no missing blocks.
     */
    public boolean isHealthy() {
      return ((missingIds.size() == 0) && (corruptBlocks == 0));
    }
    
    /** Add a missing block name, plus its size. */
    public void addMissing(String id, long size) {
      missingIds.add(id);
      missingSize += size;
    }
    
    /** Return a list of missing block names (as list of Strings). */
    public ArrayList<String> getMissingIds() {
      return missingIds;
    }
    
    /** Return total size of missing data, in bytes. */
    public long getMissingSize() {
      return missingSize;
    }

    public void setMissingSize(long missingSize) {
      this.missingSize = missingSize;
    }
    
    /** Return the number of over-replicated blocks. */
    public long getExcessiveReplicas() {
      return excessiveReplicas;
    }
    
    public void setExcessiveReplicas(long overReplicatedBlocks) {
      this.excessiveReplicas = overReplicatedBlocks;
    }
    
    /** Return the actual replication factor. */
    public float getReplicationFactor() {
      if (totalBlocks == 0)
        return 0.0f;
      return (float) (totalReplicas) / (float) totalBlocks;
    }
    
    /** Return the number of under-replicated blocks. Note: missing blocks are not counted here.*/
    public long getMissingReplicas() {
      return missingReplicas;
    }
    
    public void setMissingReplicas(long underReplicatedBlocks) {
      this.missingReplicas = underReplicatedBlocks;
    }
    
    /** Return total number of directories encountered during this scan. */
    public long getTotalDirs() {
      return totalDirs;
    }
    
    public void setTotalDirs(long totalDirs) {
      this.totalDirs = totalDirs;
    }
    
    /** Return total number of files encountered during this scan. */
    public long getTotalFiles() {
      return totalFiles;
    }
    
    public void setTotalFiles(long totalFiles) {
      this.totalFiles = totalFiles;
    }
    
    /** Return total number of files opened for write encountered during this scan. */
    public long getTotalOpenFiles() {
      return totalOpenFiles;
    }

    /** Set total number of open files encountered during this scan. */
    public void setTotalOpenFiles(long totalOpenFiles) {
      this.totalOpenFiles = totalOpenFiles;
    }
    
    /** Return total size of scanned data, in bytes. */
    public long getTotalSize() {
      return totalSize;
    }
    
    public void setTotalSize(long totalSize) {
      this.totalSize = totalSize;
    }
    
    /** Return total size of open files data, in bytes. */
    public long getTotalOpenFilesSize() {
      return totalOpenFilesSize;
    }
    
    public void setTotalOpenFilesSize(long totalOpenFilesSize) {
      this.totalOpenFilesSize = totalOpenFilesSize;
    }
    
    /** Return the intended replication factor, against which the over/under-
     * replicated blocks are counted. Note: this values comes from the current
     * Configuration supplied for the tool, so it may be different from the
     * value in DFS Configuration.
     */
    public int getReplication() {
      return replication;
    }
    
    public void setReplication(int replication) {
      this.replication = replication;
    }
    
    /** Return the total number of blocks in the scanned area. */
    public long getTotalBlocks() {
      return totalBlocks;
    }
    
    public void setTotalBlocks(long totalBlocks) {
      this.totalBlocks = totalBlocks;
    }
    
    /** Return the total number of blocks held by open files. */
    public long getTotalOpenFilesBlocks() {
      return totalOpenFilesBlocks;
    }
    
    public void setTotalOpenFilesBlocks(long totalOpenFilesBlocks) {
      this.totalOpenFilesBlocks = totalOpenFilesBlocks;
    }
    
    public String toString() {
      StringBuffer res = new StringBuffer();
      res.append("Status: " + (isHealthy() ? "HEALTHY" : "CORRUPT"));
      res.append("\n Total size:\t" + totalSize + " B");
      if (totalOpenFilesSize != 0) 
        res.append(" (Total open files size: " + totalOpenFilesSize + " B)");
      res.append("\n Total dirs:\t" + totalDirs);
      res.append("\n Total files:\t" + totalFiles);
      if (totalOpenFiles != 0)
        res.append(" (Files currently being written: " + 
                   totalOpenFiles + ")");
      res.append("\n Total blocks (validated):\t" + totalBlocks);
      if (totalBlocks > 0) res.append(" (avg. block size "
                                      + (totalSize / totalBlocks) + " B)");
      if (totalOpenFilesBlocks != 0)
        res.append(" (Total open file blocks (not validated): " + 
                   totalOpenFilesBlocks + ")");
      if (corruptFiles > 0) { 
        res.append("\n  ********************************");
        res.append("\n  CORRUPT FILES:\t" + corruptFiles);
        if (missingSize > 0) {
          res.append("\n  MISSING BLOCKS:\t" + missingIds.size());
          res.append("\n  MISSING SIZE:\t\t" + missingSize + " B");
        }
        if (corruptBlocks > 0) {
          res.append("\n  CORRUPT BLOCKS: \t" + corruptBlocks);
        }
        res.append("\n  ********************************");
      }
      res.append("\n Minimally replicated blocks:\t" + numMinReplicatedBlocks);
      if (totalBlocks > 0)        res.append(" (" + ((float) (numMinReplicatedBlocks * 100) / (float) totalBlocks) + " %)");
      res.append("\n Over-replicated blocks:\t" + numOverReplicatedBlocks);
      if (totalBlocks > 0)        res.append(" (" + ((float) (numOverReplicatedBlocks * 100) / (float) totalBlocks) + " %)");
      res.append("\n Under-replicated blocks:\t" + numUnderReplicatedBlocks);
      if (totalBlocks > 0)        res.append(" (" + ((float) (numUnderReplicatedBlocks * 100) / (float) totalBlocks) + " %)");
      res.append("\n Mis-replicated blocks:\t\t" + numMisReplicatedBlocks);
      if (totalBlocks > 0)        res.append(" (" + ((float) (numMisReplicatedBlocks * 100) / (float) totalBlocks) + " %)");
      res.append("\n Default replication factor:\t" + replication);
      res.append("\n Average block replication:\t" + getReplicationFactor());
      res.append("\n Corrupt blocks:\t\t" + corruptBlocks);
      res.append("\n Missing replicas:\t\t" + missingReplicas);
      if (totalReplicas > 0)        res.append(" (" + ((float) (missingReplicas * 100) / (float) totalReplicas) + " %)");
      res.append("\n Number of data-nodes:\t\t" + totalDatanodes);
      res.append("\n Number of racks:\t\t" + totalRacks);
      return res.toString();
    }
    
    /** Return the number of currupted files. */
    public long getCorruptFiles() {
      return corruptFiles;
    }
    
    public void setCorruptFiles(long corruptFiles) {
      this.corruptFiles = corruptFiles;
    }
  }
}

