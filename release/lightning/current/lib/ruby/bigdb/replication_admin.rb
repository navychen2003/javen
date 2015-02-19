include Java

# Wrapper for org.javenstudio.raptor.bigdb.client.DBAdmin

module Bigdb
  class RepAdmin
    include BigDBConstants

    def initialize(configuration, formatter)
      @replication_admin = org.javenstudio.raptor.bigdb.client.replication.ReplicationAdmin.new(configuration)
      @formatter = formatter
    end

    #----------------------------------------------------------------------------------------------
    # Add a new peer cluster to replicate to
    def add_peer(id, cluster_key)
      @replication_admin.addPeer(id, cluster_key)
    end

    #----------------------------------------------------------------------------------------------
    # Remove a peer cluster, stops the replication
    def remove_peer(id)
      @replication_admin.removePeer(id)
    end


    #---------------------------------------------------------------------------------------------
    # Show replcated tables/column families, and their ReplicationType
    def list_replicated_tables
       @replication_admin.listReplicated()
    end

    #----------------------------------------------------------------------------------------------
    # List all peer clusters
    def list_peers
      @replication_admin.listPeers
    end

    #----------------------------------------------------------------------------------------------
    # Get peer cluster state
    def get_peer_state(id)
      @replication_admin.getPeerState(id) ? "ENABLED" : "DISABLED"
    end

    #----------------------------------------------------------------------------------------------
    # Restart the replication stream to the specified peer
    def enable_peer(id)
      @replication_admin.enablePeer(id)
    end

    #----------------------------------------------------------------------------------------------
    # Stop the replication stream to the specified peer
    def disable_peer(id)
      @replication_admin.disablePeer(id)
    end
  end
end
