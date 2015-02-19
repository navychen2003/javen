
module Shell
  module Commands
    class Enable < Command
      def help
        return <<-EOF
Start enable of named table: e.g. "bigdb> enable 't1'"
EOF
      end

      def command(table)
        format_simple_command do
          admin.enable(table)
        end
      end
    end
  end
end
