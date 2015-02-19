
module Shell
  module Commands
    class Compact < Command
      def help
        return <<-EOF
          Compact all regions in passed table or pass a region row
          to compact an individual region. You can also compact a single column
          family within a region.
          Examples:
          Compact all regions in a table:
          bigdb> compact 't1'
          Compact an entire region:
          bigdb> compact 'r1'
          Compact only a column family within a region:
          bigdb> compact 'r1', 'c1'
          Compact a column family within a table:
          bigdb> compact 't1', 'c1'
        EOF
      end

      def command(table_or_region_name, family = nil)
        format_simple_command do
          admin.compact(table_or_region_name, family)
        end
      end
    end
  end
end
