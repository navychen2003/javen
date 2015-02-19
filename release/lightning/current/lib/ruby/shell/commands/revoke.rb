
module Shell
  module Commands
    class Revoke < Command
      def help
        return <<-EOF
Revoke a user's access rights.
Syntax : revoke <user> [<table> [<column family> [<column qualifier>]]
For example:

    bigdb> revoke 'bobsmith'
    bigdb> revoke 'bobsmith', 't1', 'f1', 'col1'
EOF
      end

      def command(user, table_name=nil, family=nil, qualifier=nil)
        format_simple_command do
          security_admin.revoke(user, table_name, family, qualifier)
        end
      end
    end
  end
end
