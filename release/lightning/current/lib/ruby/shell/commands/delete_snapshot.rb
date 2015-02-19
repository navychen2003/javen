
module Shell
  module Commands
    class DeleteSnapshot < Command
      def help
        return <<-EOF
Delete a specified snapshot. Examples:

  bigdb> delete_snapshot 'snapshotName',
EOF
      end

      def command(snapshot_name)
        format_simple_command do
          admin.delete_snapshot(snapshot_name)
        end
      end
    end
  end
end
