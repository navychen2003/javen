package org.javenstudio.raptor.paxos.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.io.Writable; 
import org.javenstudio.raptor.paxos.PaxosException;
import org.javenstudio.raptor.paxos.PaxosDefs;
import org.javenstudio.raptor.paxos.PaxosException.Code;
import org.javenstudio.raptor.paxos.PaxosException.SessionMovedException;
import org.javenstudio.raptor.paxos.PaxosDefs.OpCode;
import org.javenstudio.raptor.paxos.data.ACL;
import org.javenstudio.raptor.paxos.data.Stat;
import org.javenstudio.raptor.paxos.proto.CreateResponse;
import org.javenstudio.raptor.paxos.proto.ExistsRequest;
import org.javenstudio.raptor.paxos.proto.ExistsResponse;
import org.javenstudio.raptor.paxos.proto.GetACLRequest;
import org.javenstudio.raptor.paxos.proto.GetACLResponse;
import org.javenstudio.raptor.paxos.proto.GetChildren2Request;
import org.javenstudio.raptor.paxos.proto.GetChildren2Response;
import org.javenstudio.raptor.paxos.proto.GetChildrenRequest;
import org.javenstudio.raptor.paxos.proto.GetChildrenResponse;
import org.javenstudio.raptor.paxos.proto.GetDataRequest;
import org.javenstudio.raptor.paxos.proto.GetDataResponse;
import org.javenstudio.raptor.paxos.proto.ReplyHeader;
import org.javenstudio.raptor.paxos.proto.SetACLResponse;
import org.javenstudio.raptor.paxos.proto.SetDataResponse;
import org.javenstudio.raptor.paxos.proto.SetWatches;
import org.javenstudio.raptor.paxos.proto.SyncRequest;
import org.javenstudio.raptor.paxos.proto.SyncResponse;
import org.javenstudio.raptor.paxos.server.DataTree.ProcessTxnResult;
import org.javenstudio.raptor.paxos.server.NIOServerCnxn.CnxnStats;
import org.javenstudio.raptor.paxos.server.NIOServerCnxn.Factory;
import org.javenstudio.raptor.paxos.server.PaxosServer.ChangeRecord;
import org.javenstudio.raptor.paxos.txn.CreateSessionTxn;
import org.javenstudio.raptor.paxos.txn.ErrorTxn;

/**
 * This Request processor actually applies any transaction associated with a
 * request and services any queries. It is always at the end of a
 * RequestProcessor chain (hence the name), so it does not have a nextProcessor
 * member.
 *
 * This RequestProcessor counts on PaxosServer to populate the
 * outstandingRequests member of PaxosServer.
 */
public class FinalRequestProcessor implements RequestProcessor {
    private static final Logger LOG = Logger.getLogger(FinalRequestProcessor.class);

    PaxosServer zks;

    public FinalRequestProcessor(PaxosServer zks) {
        this.zks = zks;
    }

    public void processRequest(Request request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request:: " + request);
        }
        // request.addRQRec(">final");
        long traceMask = PaxosTrace.CLIENT_REQUEST_TRACE_MASK;
        if (request.type == OpCode.ping) {
            traceMask = PaxosTrace.SERVER_PING_TRACE_MASK;
        }
        if (LOG.isTraceEnabled()) {
            PaxosTrace.logRequest(LOG, traceMask, 'E', request, "");
        }
        ProcessTxnResult rc = null;
        synchronized (zks.outstandingChanges) {
            while (!zks.outstandingChanges.isEmpty()
                    && zks.outstandingChanges.get(0).zxid <= request.zxid) {
                ChangeRecord cr = zks.outstandingChanges.remove(0);
                if (cr.zxid < request.zxid) {
                    LOG.warn("Zxid outstanding "
                            + cr.zxid
                            + " is less than current " + request.zxid);
                }
                if (zks.outstandingChangesForPath.get(cr.path) == cr) {
                    zks.outstandingChangesForPath.remove(cr.path);
                }
            }
            if (request.hdr != null) {
                rc = zks.getPaxosDatabase().processTxn(request.hdr, request.txn);
                if (request.type == OpCode.createSession) {
                    if (request.txn instanceof CreateSessionTxn) {
                        CreateSessionTxn cst = (CreateSessionTxn) request.txn;
                        zks.sessionTracker.addSession(request.sessionId, cst
                                .getTimeOut());
                    } else {
                        LOG.warn("*****>>>>> Got "
                                + request.txn.getClass() + " "
                                + request.txn.toString());
                    }
                } else if (request.type == OpCode.closeSession) {
                    zks.sessionTracker.removeSession(request.sessionId);
                }
            }
            // do not add non quorum packets to the queue.
            if (Request.isQuorum(request.type)) {
                zks.getPaxosDatabase().addCommittedProposal(request);
            }
        }

        if (request.hdr != null && request.hdr.getType() == OpCode.closeSession) {
            Factory scxn = zks.getServerCnxnFactory();
            // this might be possible since
            // we might just be playing diffs from the leader
            if (scxn != null && request.cnxn == null) {
                // calling this if we have the cnxn results in the client's
                // close session response being lost - we've already closed
                // the session/socket here before we can send the closeSession
                // in the switch block below
                scxn.closeSession(request.sessionId);
                return;
            }
        }

        if (request.cnxn == null) {
            return;
        }
        ServerCnxn cnxn = request.cnxn;

        String lastOp = "NA";
        zks.decInProcess();
        Code err = Code.OK;
        Writable rsp = null;
        boolean closeSession = false;
        try {
            if (request.hdr != null && request.hdr.getType() == OpCode.error) {
                throw PaxosException.create(PaxosException.Code.get((
                        (ErrorTxn) request.txn).getErr()));
            }

            PaxosException ke = request.getException();
            if (ke != null) {
                throw ke;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("request: " + request);
            }
            switch (request.type) {
            case OpCode.ping: {
                zks.serverStats().updateLatency(request.createTime);

                lastOp = "PING";
                ((CnxnStats)cnxn.getStats())
                .updateForResponse(request.cxid, request.zxid, lastOp,
                        request.createTime, System.currentTimeMillis());

                cnxn.sendResponse(new ReplyHeader(-2,
                        zks.getPaxosDatabase().getDataTreeLastProcessedZxid(), 0), null, "response");
                return;
            }
            case OpCode.createSession: {
                zks.serverStats().updateLatency(request.createTime);

                lastOp = "SESS";
                ((CnxnStats)cnxn.getStats())
                .updateForResponse(request.cxid, request.zxid, lastOp,
                        request.createTime, System.currentTimeMillis());

                cnxn.finishSessionInit(true);
                return;
            }
            case OpCode.create: {
                lastOp = "CREA";
                rsp = new CreateResponse(rc.path);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.delete: {
                lastOp = "DELE";
                err = Code.get(rc.err);
                break;
            }
            case OpCode.setData: {
                lastOp = "SETD";
                rsp = new SetDataResponse(rc.stat);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.setACL: {
                lastOp = "SETA";
                rsp = new SetACLResponse(rc.stat);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.closeSession: {
                lastOp = "CLOS";
                closeSession = true;
                err = Code.get(rc.err);
                break;
            }
            case OpCode.sync: {
                lastOp = "SYNC";
                SyncRequest syncRequest = new SyncRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        syncRequest);
                rsp = new SyncResponse(syncRequest.getPath());
                break;
            }
            case OpCode.exists: {
                lastOp = "EXIS";
                // TODO we need to figure out the security requirement for this!
                ExistsRequest existsRequest = new ExistsRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        existsRequest);
                String path = existsRequest.getPath();
                if (path.indexOf('\0') != -1) {
                    throw new PaxosException.BadArgumentsException();
                }
                Stat stat = zks.getPaxosDatabase().statNode(path, existsRequest
                        .getWatch() ? cnxn : null);
                rsp = new ExistsResponse(stat);
                break;
            }
            case OpCode.getData: {
                lastOp = "GETD";
                GetDataRequest getDataRequest = new GetDataRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        getDataRequest);
                DataNode n = zks.getPaxosDatabase().getNode(getDataRequest.getPath());
                if (n == null) {
                    throw new PaxosException.NoNodeException();
                }
                Long aclL;
                synchronized(n) {
                    aclL = n.acl;
                }
                PrepRequestProcessor.checkACL(zks, zks.getPaxosDatabase().convertLong(aclL),
                        PaxosDefs.Perms.READ,
                        request.authInfo);
                Stat stat = new Stat();
                byte b[] = zks.getPaxosDatabase().getData(getDataRequest.getPath(), stat,
                        getDataRequest.getWatch() ? cnxn : null);
                rsp = new GetDataResponse(b, stat);
                break;
            }
            case OpCode.setWatches: {
                lastOp = "SETW";
                SetWatches setWatches = new SetWatches();
                // XXX We really should NOT need this!!!!
                request.request.rewind();
                PaxosServer.byteBuffer2Writable(request.request, setWatches);
                long relativeZxid = setWatches.getRelativeZxid();
                zks.getPaxosDatabase().setWatches(relativeZxid, 
                        setWatches.getDataWatches(), 
                        setWatches.getExistWatches(),
                        setWatches.getChildWatches(), cnxn);
                break;
            }
            case OpCode.getACL: {
                lastOp = "GETA";
                GetACLRequest getACLRequest = new GetACLRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        getACLRequest);
                Stat stat = new Stat();
                List<ACL> acl = 
                    zks.getPaxosDatabase().getACL(getACLRequest.getPath(), stat);
                rsp = new GetACLResponse(acl, stat);
                break;
            }
            case OpCode.getChildren: {
                lastOp = "GETC";
                GetChildrenRequest getChildrenRequest = new GetChildrenRequest();
                PaxosServer.byteBuffer2Writable(request.request,
                        getChildrenRequest);
                DataNode n = zks.getPaxosDatabase().getNode(getChildrenRequest.getPath());
                if (n == null) {
                    throw new PaxosException.NoNodeException();
                }
                Long aclG;
                synchronized(n) {
                    aclG = n.acl;
                    
                }
                PrepRequestProcessor.checkACL(zks, zks.getPaxosDatabase().convertLong(aclG), 
                        PaxosDefs.Perms.READ,
                        request.authInfo);
                List<String> children = zks.getPaxosDatabase().getChildren(
                        getChildrenRequest.getPath(), null, getChildrenRequest
                                .getWatch() ? cnxn : null);
                rsp = new GetChildrenResponse(children);
                break;
            }
            case OpCode.getChildren2: {
                lastOp = "GETC";
                GetChildren2Request getChildren2Request = new GetChildren2Request();
                PaxosServer.byteBuffer2Writable(request.request,
                        getChildren2Request);
                Stat stat = new Stat();
                DataNode n = zks.getPaxosDatabase().getNode(getChildren2Request.getPath());
                if (n == null) {
                    throw new PaxosException.NoNodeException();
                }
                Long aclG;
                synchronized(n) {
                    aclG = n.acl;
                }
                PrepRequestProcessor.checkACL(zks, zks.getPaxosDatabase().convertLong(aclG), 
                        PaxosDefs.Perms.READ,
                        request.authInfo);
                List<String> children = zks.getPaxosDatabase().getChildren(
                        getChildren2Request.getPath(), stat, getChildren2Request
                                .getWatch() ? cnxn : null);
                rsp = new GetChildren2Response(children, stat);
                break;
            }
            }
        } catch (SessionMovedException e) {
            // session moved is a connection level error, we need to tear
            // down the connection otw ZOOKEEPER-710 might happen
            // ie client on slow follower starts to renew session, fails
            // before this completes, then tries the fast follower (leader)
            // and is successful, however the initial renew is then 
            // successfully fwd/processed by the leader and as a result
            // the client and leader disagree on where the client is most
            // recently attached (and therefore invalid SESSION MOVED generated)
            cnxn.sendCloseSession();
            return;
        } catch (PaxosException e) {
            err = e.code();
        } catch (Exception e) {
            // log at error level as we are returning a marshalling
            // error to the user
            LOG.error("Failed to process " + request, e);
            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.request;
            bb.rewind();
            while (bb.hasRemaining()) {
                sb.append(Integer.toHexString(bb.get() & 0xff));
            }
            LOG.error("Dumping request buffer: 0x" + sb.toString());
            err = Code.MARSHALLINGERROR;
        }

        ReplyHeader hdr =
            new ReplyHeader(request.cxid, request.zxid, err.intValue());

        zks.serverStats().updateLatency(request.createTime);
        ((CnxnStats)cnxn.getStats())
            .updateForResponse(request.cxid, request.zxid, lastOp,
                    request.createTime, System.currentTimeMillis());

        try {
            cnxn.sendResponse(hdr, rsp, "response");
            if (closeSession) {
                cnxn.sendCloseSession();
            }
        } catch (IOException e) {
            LOG.error("FIXMSG",e);
        }
    }

    public void shutdown() {
        // we are the final link in the chain
        LOG.info("shutdown of request processor complete");
    }

}

