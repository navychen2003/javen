
module Shell
  module Commands
    class Unassign < Command
      def help
        return <<-EOF
Unassign a region. Unassign will close region in current location and then
reopen it again.  Pass 'true' to force the unassignment ('force' will clear
all in-memory state in master before the reassign. If results in
double assignment use hbck -fix to resolve. To be used by experts).
Use with caution.  For expert use only.  Examples:

  bigdb> unassign 'REGIONNAME'
  bigdb> unassign 'REGIONNAME', true
  bigdb> unassign 'ENCODED_REGIONNAME'
  bigdb> unassign 'ENCODED_REGIONNAME', true
EOF
      end

      def command(region_name, force = 'false')
        format_simple_command do
          admin.unassign(region_name, force)
        end
      end
    end
  end
end
