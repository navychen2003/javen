
module Shell
  module Commands
    class UserPermission < Command
      def help
        return <<-EOF
Show all permissions for the particular user.
Syntax : user_permission <table>
For example:

    bigdb> user_permission
    bigdb> user_permission 'table1'
EOF
      end

      def command(table=nil)
        #format_simple_command do
        #admin.user_permission(table)
        now = Time.now
        formatter.header(["User", "Table,Family,Qualifier:Permission"])

        count = security_admin.user_permission(table) do |user, permission|
          formatter.row([ user, permission])
        end

        formatter.footer(now, count)
      end
    end
  end
end
