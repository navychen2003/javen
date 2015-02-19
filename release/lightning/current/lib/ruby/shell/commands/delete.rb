
module Shell
  module Commands
    class Delete < Command
      def help
        return <<-EOF
Put a delete cell value at specified table/row/column and optionally
timestamp coordinates.  Deletes must match the deleted cell's
coordinates exactly.  When scanning, a delete cell suppresses older
versions. To delete a cell from  't1' at row 'r1' under column 'c1'
marked with the time 'ts1', do:

  bigdb> delete 't1', 'r1', 'c1', ts1

The same command can also be run on a table reference. Suppose you had a reference
t to table 't1', the corresponding command would be:

  bigdb> t.delete 'r1', 'c1',  ts1
EOF
      end

      def command(table, row, column, timestamp = org.javenstudio.raptor.bigdb.DBConstants::LATEST_TIMESTAMP)
        delete(table(table), row, column, timestamp)
      end

      def delete(table, row, column, timestamp = org.javenstudio.raptor.bigdb.DBConstants::LATEST_TIMESTAMP)
        format_simple_command do
          table._delete_internal(row, column, timestamp)
        end
      end
    end
  end
end

#Add the method table.delete that calls delete.delete
::Bigdb::Table.add_shell_command("delete")
