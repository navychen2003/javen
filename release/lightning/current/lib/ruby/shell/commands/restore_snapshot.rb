
module Shell
  module Commands
    class RestoreSnapshot < Command
      def help
        return <<-EOF
Restore a specified snapshot.
The restore will replace the content of the original table,
bringing back the content to the snapshot state.
The table must be disabled.

Examples:
  bigdb> restore_snapshot 'snapshotName'
EOF
      end

      def command(snapshot_name)
        format_simple_command do
          admin.restore_snapshot(snapshot_name)
        end
      end
    end
  end
end
