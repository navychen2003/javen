
module Shell
  module Commands
    class GetTable < Command
      def help
        return <<-EOF
Get the given table name and return it as an actual object to
be manipulated by the user. See table.help for more information
on how to use the table.
Eg.

  bigdb> t1 = get_table 't1'

returns the table named 't1' as a table object. You can then do

  bigdb> t1.help

which will then print the help for that table.
EOF
      end

      def command(table, *args)
        format_and_return_simple_command do
          table(table)
        end
      end
    end
  end
end
