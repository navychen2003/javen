
module Shell
  module Commands
    class Grant < Command
      def help
        return <<-EOF
Grant users specific rights.
Syntax : grant <user> <permissions> [<table> [<column family> [<column qualifier>]]

permissions is either zero or more letters from the set "RWXCA".
READ('R'), WRITE('W'), EXEC('X'), CREATE('C'), ADMIN('A')

For example:

    bigdb> grant 'bobsmith', 'RWXCA'
    bigdb> grant 'bobsmith', 'RW', 't1', 'f1', 'col1'
EOF
      end

      def command(user, rights, table_name=nil, family=nil, qualifier=nil)
        format_simple_command do
          security_admin.grant(user, rights, table_name, family, qualifier)
        end
      end
    end
  end
end
