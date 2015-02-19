
module Shell
  module Commands
    class Put < Command
      def help
        return <<-EOF
Put a cell 'value' at specified table/row/column and optionally
timestamp coordinates.  To put a cell value into table 't1' at
row 'r1' under column 'c1' marked with the time 'ts1', do:

  bigdb> put 't1', 'r1', 'c1', 'value', ts1

The same commands also can be run on a table reference. Suppose you had a reference
t to table 't1', the corresponding command would be:

  bigdb> t.put 'r1', 'c1', 'value', ts1
EOF
      end

      def command(table, row, column, value, timestamp = nil)
        put table(table), row, column, value, timestamp
      end

      def put(table, row, column, value, timestamp = nil)
        format_simple_command do
          table._put_internal(row, column, value, timestamp)
        end
      end
    end
  end
end

#Add the method table.put that calls Put.put
::Bigdb::Table.add_shell_command("put")
