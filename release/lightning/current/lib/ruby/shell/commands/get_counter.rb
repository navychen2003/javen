
module Shell
  module Commands
    class GetCounter < Command
      def help
        return <<-EOF
Return a counter cell value at specified table/row/column coordinates.
A cell cell should be managed with atomic increment function oh BigDB
and the data should be binary encoded. Example:

  bigdb> get_counter 't1', 'r1', 'c1'

The same commands also can be run on a table reference. Suppose you had a reference
t to table 't1', the corresponding command would be:

  bigdb> t.get_counter 'r1', 'c1'
EOF
      end

      def command(table, row, column, value)
        get_counter(table(table), row, column, value)
      end

      def get_counter(table, row, column, value = nil)
        if cnt = table._get_counter_internal(row, column)
          puts "COUNTER VALUE = #{cnt}"
        else
          puts "No counter found at specified coordinates"
        end
      end
    end
  end
end

::Bigdb::Table.add_shell_command('get_counter')
