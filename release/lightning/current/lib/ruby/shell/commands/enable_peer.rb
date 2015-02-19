
module Shell
  module Commands
    class EnablePeer< Command
      def help
        return <<-EOF
Restarts the replication to the specified peer cluster,
continuing from where it was disabled.

Examples:

  bigdb> enable_peer '1'
EOF
      end

      def command(id)
        format_simple_command do
          replication_admin.enable_peer(id)
        end
      end
    end
  end
end
