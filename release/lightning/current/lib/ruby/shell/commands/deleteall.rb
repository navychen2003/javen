
module Shell
  module Commands
    class Deleteall < Command
      def help
        return <<-EOF
Delete all cells in a given row; pass a table name, row, and optionally
a column and timestamp. Examples:

  bigdb> deleteall 't1', 'r1'
  bigdb> deleteall 't1', 'r1', 'c1'
  bigdb> deleteall 't1', 'r1', 'c1', ts1

The same commands also can be run on a table reference. Suppose you had a reference
t to table 't1', the corresponding command would be:

  bigdb> t.deleteall 'r1'
  bigdb> t.deleteall 'r1', 'c1'
  bigdb> t.deleteall 'r1', 'c1', ts1
EOF
      end

      def command(table, row, column = nil,
                  timestamp = org.javenstudio.raptor.bigdb.DBConstants::LATEST_TIMESTAMP)
        deleteall(table(table), row, column, timestamp)
      end

      def deleteall(table, row, column = nil,
                    timestamp = org.javenstudio.raptor.bigdb.DBConstants::LATEST_TIMESTAMP)
        format_simple_command do
          table._deleteall_internal(row, column, timestamp)
        end
      end
    end
  end
end

#Add the method table.deleteall that calls deleteall.deleteall
::Bigdb::Table.add_shell_command("deleteall")
