package org.javenstudio.raptor.paxos.server.util;

import java.io.DataInput; 
import java.io.DataOutput; 
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.PaxosDefs.OpCode;
import org.javenstudio.raptor.paxos.server.DataTree;
import org.javenstudio.raptor.paxos.server.PaxosTrace;
import org.javenstudio.raptor.paxos.txn.CreateSessionTxn;
import org.javenstudio.raptor.paxos.txn.CreateTxn;
import org.javenstudio.raptor.paxos.txn.DeleteTxn;
import org.javenstudio.raptor.paxos.txn.ErrorTxn;
import org.javenstudio.raptor.paxos.txn.SetACLTxn;
import org.javenstudio.raptor.paxos.txn.SetDataTxn;
import org.javenstudio.raptor.paxos.txn.TxnHeader;

public class SerializeUtils {
    private static final Logger LOG = Logger.getLogger(SerializeUtils.class);
    
    public static Writable deserializeTxn(DataInput in, TxnHeader hdr)
            throws IOException {
        hdr.readFields(in);
        Writable txn = null;
        switch (hdr.getType()) {
        case OpCode.createSession:
            // This isn't really an error txn; it just has the same
            // format. The error represents the timeout
            txn = new CreateSessionTxn();
            break;
        case OpCode.closeSession:
            return null;
        case OpCode.create:
            txn = new CreateTxn();
            break;
        case OpCode.delete:
            txn = new DeleteTxn();
            break;
        case OpCode.setData:
            txn = new SetDataTxn();
            break;
        case OpCode.setACL:
            txn = new SetACLTxn();
            break;
        case OpCode.error:
            txn = new ErrorTxn();
            break;
        }
        if (txn != null) {
            txn.readFields(in);
        }
        return txn;
    }

    public static void deserializeSnapshot(DataTree dt, DataInput in,
            Map<Long, Integer> sessions) throws IOException {
        int count = in.readInt();
        while (count > 0) {
            long id = in.readLong();
            int to = in.readInt();
            sessions.put(id, to);
            if (LOG.isTraceEnabled()) {
                PaxosTrace.logTraceMessage(LOG, PaxosTrace.SESSION_TRACE_MASK,
                        "loadData --- session in archive: " + id
                        + " with timeout: " + to);
            }
            count--;
        }
        dt.deserialize(in);
    }

    public static void serializeSnapshot(DataTree dt, DataOutput out,
            Map<Long, Integer> sessions) throws IOException {
        HashMap<Long, Integer> sessSnap = new HashMap<Long, Integer>(sessions);
        out.writeInt(sessSnap.size());
        for (Entry<Long, Integer> entry : sessSnap.entrySet()) {
            out.writeLong(entry.getKey().longValue());
            out.writeInt(entry.getValue().intValue());
        }
        dt.serialize(out);
    }

}

