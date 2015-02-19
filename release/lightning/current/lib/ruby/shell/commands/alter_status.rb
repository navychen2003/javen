
module Shell
  module Commands
    class AlterStatus < Command
      def help
        return <<-EOF
Get the status of the alter command. Indicates the number of regions of the
table that have received the updated schema
Pass table name.

bigdb> alter_status 't1'
EOF
      end
      def command(table)
        admin.alter_status(table)
      end
    end
  end
end
