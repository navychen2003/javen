package org.javenstudio.raptor.paxos;

import java.util.ArrayList;
import java.util.Collections;

import org.javenstudio.raptor.paxos.data.ACL;
import org.javenstudio.raptor.paxos.data.Id;

public class PaxosDefs {
    public interface OpCode {
        public final int notification = 0;

        public final int create = 1;

        public final int delete = 2;

        public final int exists = 3;

        public final int getData = 4;

        public final int setData = 5;

        public final int getACL = 6;

        public final int setACL = 7;

        public final int getChildren = 8;

        public final int sync = 9;

        public final int ping = 11;

        public final int getChildren2 = 12;

        public final int auth = 100;

        public final int setWatches = 101;

        public final int createSession = -10;

        public final int closeSession = -11;

        public final int error = -1;
    }

    public interface Perms {
        int READ = 1 << 0;

        int WRITE = 1 << 1;

        int CREATE = 1 << 2;

        int DELETE = 1 << 3;

        int ADMIN = 1 << 4;

        int ALL = READ | WRITE | CREATE | DELETE | ADMIN;
    }

    public interface Ids {
        /**
         * This Id represents anyone.
         */
        public final Id ANYONE_ID_UNSAFE = new Id("world", "anyone");

        /**
         * This Id is only usable to set ACLs. It will get substituted with the
         * Id's the client authenticated with.
         */
        public final Id AUTH_IDS = new Id("auth", "");

        /**
         * This is a completely open ACL .
         */
        public final ArrayList<ACL> OPEN_ACL_UNSAFE = new ArrayList<ACL>(
                Collections.singletonList(new ACL(Perms.ALL, ANYONE_ID_UNSAFE)));

        /**
         * This ACL gives the creators authentication id's all permissions.
         */
        public final ArrayList<ACL> CREATOR_ALL_ACL = new ArrayList<ACL>(
                Collections.singletonList(new ACL(Perms.ALL, AUTH_IDS)));

        /**
         * This ACL gives the world the ability to read.
         */
        public final ArrayList<ACL> READ_ACL_UNSAFE = new ArrayList<ACL>(
                Collections
                        .singletonList(new ACL(Perms.READ, ANYONE_ID_UNSAFE)));
    }

    final public static String[] opNames = { "notification", "create",
            "delete", "exists", "getData", "setData", "getACL", "setACL",
            "getChildren", "getChildren2", "getMaxChildren", "setMaxChildren", "ping" };
}

