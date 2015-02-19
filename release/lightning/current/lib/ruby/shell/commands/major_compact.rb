
module Shell
  module Commands
    class MajorCompact < Command
      def help
        return <<-EOF
          Run major compaction on passed table or pass a region row
          to major compact an individual region. To compact a single
          column family within a region specify the region name
          followed by the column family name.
          Examples:
          Compact all regions in a table:
          bigdb> major_compact 't1'
          Compact an entire region:
          bigdb> major_compact 'r1'
          Compact a single column family within a region:
          bigdb> major_compact 'r1', 'c1'
          Compact a single column family within a table:
          bigdb> major_compact 't1', 'c1'
        EOF
      end

      def command(table_or_region_name, family = nil)
        format_simple_command do
          admin.major_compact(table_or_region_name, family)
        end
      end
    end
  end
end
