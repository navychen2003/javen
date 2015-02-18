package org.javenstudio.raptor.paxos;

import java.util.List;

//import jline.Completor;

class JLineZNodeCompletor /*implements Completor*/ {
    private Paxos paxos;

    public JLineZNodeCompletor(Paxos paxos) {
        this.paxos = paxos;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int complete(String buffer, int cursor, List candidates) {
        // Guarantee that the final token is the one we're expanding
        buffer = buffer.substring(0,cursor);
        String token = "";
        if (!buffer.endsWith(" ")) {
            String[] tokens = buffer.split(" ");
            if (tokens.length != 0) {
                token = tokens[tokens.length-1] ;
            }
        }

        if (token.startsWith("/")){
            return completeZNode( buffer, token, candidates);
        }
        return completeCommand(buffer, token, candidates);
    }

    private int completeCommand(String buffer, String token,
            List<String> candidates)
    {
        for (String cmd : PaxosMain.getCommands()) {
            if (cmd.startsWith( token )) {
                candidates.add(cmd);
            }
        }
        return buffer.lastIndexOf(" ")+1;
    }

    private int completeZNode( String buffer, String token,
            List<String> candidates)
    {
        String path = token;
        int idx = path.lastIndexOf("/") + 1;
        String prefix = path.substring(idx);
        try {
            // Only the root path can end in a /, so strip it off every other prefix
            String dir = idx == 1 ? "/" : path.substring(0,idx-1);
            List<String> children = paxos.getChildren(dir, false);
            for (String child : children) {
                if (child.startsWith(prefix)) {
                    candidates.add( child );
                }
            }
        } catch( InterruptedException e) {
            return 0;
        }
        catch( PaxosException e) {
            return 0;
        }
        return candidates.size() == 0 ? buffer.length() : buffer.lastIndexOf("/") + 1;
    }
}

