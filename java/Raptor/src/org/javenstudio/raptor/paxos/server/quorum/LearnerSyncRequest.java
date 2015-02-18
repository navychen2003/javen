package org.javenstudio.raptor.paxos.server.quorum;

import java.nio.ByteBuffer;
import java.util.List;

import org.javenstudio.raptor.paxos.data.Id;
import org.javenstudio.raptor.paxos.server.Request;

public class LearnerSyncRequest extends Request {
	LearnerHandler fh;
	public LearnerSyncRequest(LearnerHandler fh, long sessionId, int xid, int type,
			ByteBuffer bb, List<Id> authInfo) {
		super(null, sessionId, xid, type, bb, authInfo);
		this.fh = fh;
	}
}

