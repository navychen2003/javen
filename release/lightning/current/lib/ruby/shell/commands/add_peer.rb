
module Shell
  module Commands
    class AddPeer< Command
      def help
        return <<-EOF
Add a peer cluster to replicate to, the id must be a short and
the cluster key is composed like this:
bigdb.zookeeper.quorum:bigdb.zookeeper.property.clientPort:zookeeper.znode.parent
This gives a full path for BigDB to connect to another cluster.
Examples:

  bigdb> add_peer '1', "server1.cie.com:2181:/bigdb"
  bigdb> add_peer '2', "zk1,zk2,zk3:2182:/bigdb-prod"
EOF
      end

      def command(id, cluster_key)
        format_simple_command do
          replication_admin.add_peer(id, cluster_key)
        end
      end
    end
  end
end
