
module Shell
  module Commands
    class MergeRegion < Command
      def help
        return <<-EOF
Merge two regions. Passing 'true' as the optional third parameter will force
a merge ('force' merges regardless else merge will fail unless passed
adjacent regions. 'force' is for expert use only).

NOTE: You must pass the encoded region name, not the full region name so
this command is a little different from other region operations.  The encoded
region name is the hash suffix on region names: e.g. if the region name were
TestTable,0094429456,1289497600452.527db22f95c8a9e0116f0cc13c680396. then
the encoded region name portion is 527db22f95c8a9e0116f0cc13c680396

Examples:

  bigdb> merge_region 'ENCODED_REGIONNAME', 'ENCODED_REGIONNAME'
  bigdb> merge_region 'ENCODED_REGIONNAME', 'ENCODED_REGIONNAME', true
EOF
      end

      def command(encoded_region_a_name, encoded_region_b_name, force = 'false')
        format_simple_command do
          admin.merge_region(encoded_region_a_name, encoded_region_b_name, force)
        end
      end
    end
  end
end
