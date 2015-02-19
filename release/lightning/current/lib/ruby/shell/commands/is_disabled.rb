
module Shell
  module Commands
    class IsDisabled < Command
      def help
        return <<-EOF
Is named table disabled?: e.g. "bigdb> is_disabled 't1'"
EOF
      end

      def command(table)
        format_simple_command do
          formatter.row([
            admin.disabled?(table)? "true" : "false"
          ])
        end
      end
    end
  end
end
