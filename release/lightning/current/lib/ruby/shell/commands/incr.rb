
module Shell
  module Commands
    class Incr < Command
      def help
        return <<-EOF
Increments a cell 'value' at specified table/row/column coordinates.
To increment a cell value in table 't1' at row 'r1' under column
'c1' by 1 (can be omitted) or 10 do:

  bigdb> incr 't1', 'r1', 'c1'
  bigdb> incr 't1', 'r1', 'c1', 1
  bigdb> incr 't1', 'r1', 'c1', 10

The same commands also can be run on a table reference. Suppose you had a reference
t to table 't1', the corresponding command would be:

  bigdb> t.incr 'r1', 'c1'
  bigdb> t.incr 'r1', 'c1', 1
  bigdb> t.incr 'r1', 'c1', 10
EOF
      end

      def command(table, row, column, value)
        incr(table(table), row, column, value)
      end

      def incr(table, row, column, value = nil)
        cnt = table._incr_internal(row, column, value)
        puts "COUNTER VALUE = #{cnt}"
      end
    end
  end
end

#add incr comamnd to Table
::Bigdb::Table.add_shell_command("incr")
