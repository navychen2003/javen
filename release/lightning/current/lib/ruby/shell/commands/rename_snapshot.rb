
module Shell
  module Commands
    class RenameSnapshot < Command
      def help
        return <<-EOF
Rename a specified snapshot. Examples:

  bigdb> rename_snapshot 'oldSnapshotName' 'newSnapshotName'
EOF
      end

      def command(old_snapshot_name, new_snapshot_name)
        format_simple_command do
          admin.rename_snapshot(old_snapshot_name, new_snapshot_name)
        end
      end
    end
  end
end
