package org.javenstudio.raptor.paxos.server.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInput; 
import java.io.DataOutput; 
import java.io.DataInputStream; 
import java.io.DataOutputStream; 
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.UTF8; 
import org.javenstudio.raptor.paxos.server.DataTree;
import org.javenstudio.raptor.paxos.server.util.SerializeUtils;

/**
 * This class implements the snapshot interface.
 * it is responsible for storing, serializing
 * and deserializing the right snapshot.
 * and provides access to the snapshots.
 */
@SuppressWarnings("deprecation")
public class FileSnap implements SnapShot {
    File snapDir;
    private volatile boolean close = false;
    private static final int VERSION=2;
    private static final long dbId=-1;
    private static final Logger LOG = Logger.getLogger(FileSnap.class);
    public final static int SNAP_MAGIC
        = ByteBuffer.wrap("ZKSN".getBytes()).getInt();
    public FileSnap(File snapDir) {
        this.snapDir = snapDir;
    }

    /**
     * deserialize a data tree from the most recent snapshot
     * @return the zxid of the snapshot
     */ 
    public long deserialize(DataTree dt, Map<Long, Integer> sessions)
            throws IOException {
        // we run through 100 snapshots (not all of them)
        // if we cannot get it running within 100 snapshots
        // we should  give up
        List<File> snapList = findNValidSnapshots(100);
        if (snapList.size() == 0) {
            return -1L;
        }
        File snap = null;
        boolean foundValid = false;
        for (int i = 0; i < snapList.size(); i++) {
            snap = snapList.get(i);
            InputStream snapIS = null;
            CheckedInputStream crcIn = null;
            try {
                LOG.info("Reading snapshot " + snap);
                snapIS = new BufferedInputStream(new FileInputStream(snap));
                crcIn = new CheckedInputStream(snapIS, new Adler32());
                DataInput in = new DataInputStream(crcIn); 
                deserialize(dt, sessions, in);
                long checkSum = crcIn.getChecksum().getValue();
                long val = in.readLong();
                if (val != checkSum) {
                    throw new IOException("CRC corruption in snapshot :  " + snap);
                }
                foundValid = true;
                break;
            } catch(IOException e) {
                LOG.warn("problem reading snap file " + snap, e);
            } finally {
                if (snapIS != null) 
                    snapIS.close();
                if (crcIn != null) 
                    crcIn.close();
            } 
        }
        if (!foundValid) {
            throw new IOException("Not able to find valid snapshots in " + snapDir);
        }
        dt.lastProcessedZxid = Util.getZxidFromName(snap.getName(), "snapshot");
        return dt.lastProcessedZxid;
    }

    /**
     * deserialize the datatree from an inputarchive
     * @param dt the datatree to be serialized into
     * @param sessions the sessions to be filled up
     * @param ia the input archive to restore from
     * @throws IOException
     */
    public void deserialize(DataTree dt, Map<Long, Integer> sessions, DataInput in) throws IOException {
        FileHeader header = FileHeader.read(in);
        if (header.getMagic() != SNAP_MAGIC) {
            throw new IOException("mismatching magic headers "
                    + header.getMagic() + 
                    " !=  " + FileSnap.SNAP_MAGIC);
        }
        SerializeUtils.deserializeSnapshot(dt, in, sessions);
    }

    /**
     * find the most recent snapshot in the database.
     * @return the file containing the most recent snapshot
     */
    public File findMostRecentSnapshot() throws IOException {
        List<File> files = findNValidSnapshots(1);
        if (files.size() == 0) {
            return null;
        }
        return files.get(0);
    }
    
    /**
     * find the last (maybe) valid n snapshots. this does some 
     * minor checks on the validity of the snapshots. It just
     * checks for / at the end of the snapshot. This does
     * not mean that the snapshot is truly valid but is
     * valid with a high probability. also, the most recent 
     * will be first on the list. 
     * @param n the number of most recent snapshots
     * @return the last n snapshots (the number might be
     * less than n in case enough snapshots are not available).
     * @throws IOException
     */
    private List<File> findNValidSnapshots(int n) throws IOException {
        List<File> files = Util.sortDataDir(snapDir.listFiles(),"snapshot", false);
        int count = 0;
        List<File> list = new ArrayList<File>();
        for (File f : files) {
            // we should catch the exceptions
            // from the valid snapshot and continue
            // until we find a valid one
            try {
                if (Util.isValidSnapshot(f)) {
                    list.add(f);
                    count++;
                    if (count == n) {
                        break;
                    }
                }
            } catch (IOException e) {
                LOG.info("invalid snapshot " + f, e);
            }
        }
        return list;
    }

    /**
     * find the last n snapshots. this does not have
     * any checks if the snapshot might be valid or not
     * @param the number of most recent snapshots 
     * @return the last n snapshots
     * @throws IOException
     */
    public List<File> findNRecentSnapshots(int n) throws IOException {
        List<File> files = Util.sortDataDir(snapDir.listFiles(), "snapshot", false);
        int i = 0;
        List<File> list = new ArrayList<File>();
        for (File f: files) {
            if (i==n)
                break;
            i++;
            list.add(f);
        }
        return list;
    }

    /**
     * serialize the datatree and sessions
     * @param dt the datatree to be serialized
     * @param sessions the sessions to be serialized
     * @param oa the output archive to serialize into
     * @param header the header of this snapshot
     * @throws IOException
     */
    protected void serialize(DataTree dt, Map<Long, Integer> sessions,
            DataOutput out, FileHeader header) throws IOException {
        // this is really a programmatic error and not something that can
        // happen at runtime
        if(header==null)
            throw new IllegalStateException(
                    "Snapshot's not open for writing: uninitialized header");
        header.write(out); 
        SerializeUtils.serializeSnapshot(dt, out, sessions);
    }

    /**
     * serialize the datatree and session into the file snapshot
     * @param dt the datatree to be serialized
     * @param sessions the sessions to be serialized
     * @param snapShot the file to store snapshot into
     */
    public synchronized void serialize(DataTree dt, Map<Long, Integer> sessions, File snapShot)
            throws IOException {
        if (!close) {
            OutputStream sessOS = new BufferedOutputStream(new FileOutputStream(snapShot));
            CheckedOutputStream crcOut = new CheckedOutputStream(sessOS, new Adler32());
            DataOutputStream out = new DataOutputStream(crcOut); 
            FileHeader header = new FileHeader(SNAP_MAGIC, VERSION, dbId);
            serialize(dt, sessions, out, header);
            long val = crcOut.getChecksum().getValue();
            out.writeLong(val);
            UTF8.writeString(out, "/");
            out.flush(); 
            sessOS.flush();
            crcOut.close();
            sessOS.close();
        }
    }

    /**
     * synchronized close just so that if serialize is in place
     * the close operation will block and will wait till serialize
     * is done and will set the close flag
     */
    @Override
    public synchronized void close() throws IOException {
        close = true;
    }

 }
