
module Shell
  module Commands
    class ListPeers< Command
      def help
        return <<-EOF
List all replication peer clusters.

  bigdb> list_peers
EOF
      end

      def command()
        now = Time.now
        peers = replication_admin.list_peers

        formatter.header(["PEER_ID", "CLUSTER_KEY", "STATE"])

        peers.entrySet().each do |e|
          state = replication_admin.get_peer_state(e.key)
          formatter.row([ e.key, e.value, state ])
        end

        formatter.footer(now)
      end
    end
  end
end
