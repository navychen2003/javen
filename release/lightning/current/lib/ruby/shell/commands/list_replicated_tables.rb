
module Shell
  module Commands
    class ListReplicatedTables< Command
      def help
        return <<-EOF
List all the tables and column families replicated from this cluster

  bigdb> list_replicated_tables
  bigdb> list_replicated_tables 'abc.*'
EOF
      end

      def command(regex = ".*")
        now = Time.now

        formatter.header([ "TABLE:COLUMNFAMILY", "ReplicationType" ], [ 32 ])
        list = replication_admin.list_replicated_tables
        regex = /#{regex}/ unless regex.is_a?(Regexp)
        list = list.select {|s| regex.match(s.get(org.javenstudio.raptor.bigdb.client.replication.ReplicationAdmin::TNAME))}
        list.each do |e|
          if e.get(org.javenstudio.raptor.bigdb.client.replication.ReplicationAdmin::REPLICATIONTYPE) == org.javenstudio.raptor.bigdb.client.replication.ReplicationAdmin::REPLICATIONGLOBAL
             replicateType = "GLOBAL"
          else
             replicateType = "unknown"
          end
          formatter.row([e.get(org.javenstudio.raptor.bigdb.client.replication.ReplicationAdmin::TNAME) + ":" + e.get(org.javenstudio.raptor.bigdb.client.replication.ReplicationAdmin::CFNAME), replicateType], true, [32])
        end
        formatter.footer(now)
      end
    end
  end
end
