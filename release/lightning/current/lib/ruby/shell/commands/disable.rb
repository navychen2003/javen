
module Shell
  module Commands
    class Disable < Command
      def help
        return <<-EOF
Start disable of named table: e.g. "bigdb> disable 't1'"
EOF
      end

      def command(table)
        format_simple_command do
          admin.disable(table)
        end
      end
    end
  end
end
