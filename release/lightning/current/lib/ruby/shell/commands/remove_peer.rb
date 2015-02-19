
module Shell
  module Commands
    class RemovePeer< Command
      def help
        return <<-EOF
Stops the specified replication stream and deletes all the meta
information kept about it. Examples:

  bigdb> remove_peer '1'
EOF
      end

      def command(id)
        format_simple_command do
          replication_admin.remove_peer(id)
        end
      end
    end
  end
end
