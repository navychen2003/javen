
module Shell
  module Commands
    class DisablePeer< Command
      def help
        return <<-EOF
Stops the replication stream to the specified cluster, but still
keeps track of new edits to replicate.

Examples:

  bigdb> disable_peer '1'
EOF
      end

      def command(id)
        format_simple_command do
          replication_admin.disable_peer(id)
        end
      end
    end
  end
end
