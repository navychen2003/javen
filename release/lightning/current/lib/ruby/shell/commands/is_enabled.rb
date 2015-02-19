
module Shell
  module Commands
    class IsEnabled < Command
      def help
        return <<-EOF
Is named table enabled?: e.g. "bigdb> is_enabled 't1'"
EOF
      end

      def command(table)
        format_simple_command do
          formatter.row([
            admin.enabled?(table)? "true" : "false"
          ])
        end
      end
    end
  end
end
