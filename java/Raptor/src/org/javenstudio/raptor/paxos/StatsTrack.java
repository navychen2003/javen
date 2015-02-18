package org.javenstudio.raptor.paxos;

/**
 * a class that represents the stats associated with quotas
 */
public class StatsTrack {
    private int count;
    private long bytes;
    private String countStr = "count";
    private String byteStr = "bytes";

    /**
     * a default constructor for
     * stats
     */
    public StatsTrack() {
        this(null);
    }
    /**
     * the stat string should be of the form count=int,bytes=long
     * if stats is called with null the count and bytes are initialized
     * to -1.
     * @param stats the stat string to be intialized with
     */
    public StatsTrack(String stats) {
        if (stats == null) {
            stats = "count=-1,bytes=-1";
        }
        String[] split = stats.split(",");
        if (split.length != 2) {
            throw new IllegalArgumentException("invalid string " + stats);
        }
        count = Integer.parseInt(split[0].split("=")[1]);
        bytes = Long.parseLong(split[1].split("=")[1]);
    }


    /**
     * get the count of nodes allowed as part of quota
     *
     * @return the count as part of this string
     */
    public int getCount() {
        return this.count;
    }

    /**
     * set the count for this stat tracker.
     *
     * @param count
     *            the count to set with
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * get the count of bytes allowed as part of quota
     *
     * @return the bytes as part of this string
     */
    public long getBytes() {
        return this.bytes;
    }

    /**
     * set teh bytes for this stat tracker.
     *
     * @param bytes
     *            the bytes to set with
     */
    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    @Override
    /*
     * returns the string that maps to this stat tracking.
     */
    public String toString() {
        return countStr + "=" + count + "," + byteStr + "=" + bytes;
    }
}

