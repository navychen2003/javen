
module Shell
  module Commands
    class TruncatePreserve < Command
      def help
        return <<-EOF
  Disables, drops and recreates the specified table while still maintaing the previous region boundaries.
EOF
      end

      def command(table)
        format_simple_command do
          puts "Truncating '#{table}' table (it may take a while):"
          admin.truncate_preserve(table) { |log| puts " - #{log}" }
        end
      end

    end
  end
end
