
module Shell
  module Commands
    class CloneSnapshot < Command
      def help
        return <<-EOF
Create a new table by cloning the snapshot content. 
There're no copies of data involved.
And writing on the newly created table will not influence the snapshot data.

Examples:
  bigdb> clone_snapshot 'snapshotName', 'tableName'
EOF
      end

      def command(snapshot_name, table)
        format_simple_command do
          admin.clone_snapshot(snapshot_name, table)
        end
      end
    end
  end
end
