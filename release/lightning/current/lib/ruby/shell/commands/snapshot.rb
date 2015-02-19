
module Shell
  module Commands
    class Snapshot < Command
      def help
        return <<-EOF
Take a snapshot of specified table. Examples:

  bigdb> snapshot 'sourceTable', 'snapshotName'
EOF
      end

      def command(table, snapshot_name)
        format_simple_command do
          admin.snapshot(table, snapshot_name)
        end
      end
    end
  end
end
