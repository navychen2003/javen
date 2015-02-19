
module Shell
  module Commands
    class Split < Command
      def help
        return <<-EOF
Split entire table or pass a region to split individual region.  With the 
second parameter, you can specify an explicit split key for the region.  
Examples:
    split 'tableName'
    split 'regionName' # format: 'tableName,startKey,id'
    split 'tableName', 'splitKey'
    split 'regionName', 'splitKey'
EOF
      end

      def command(table_or_region_name, split_point = nil)
        format_simple_command do
          admin.split(table_or_region_name, split_point)
        end
      end
    end
  end
end
