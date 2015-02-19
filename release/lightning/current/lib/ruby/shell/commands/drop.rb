
module Shell
  module Commands
    class Drop < Command
      def help
        return <<-EOF
Drop the named table. Table must first be disabled: e.g. "bigdb> drop 't1'"
EOF
      end

      def command(table)
        format_simple_command do
          admin.drop(table)
        end
      end
    end
  end
end
