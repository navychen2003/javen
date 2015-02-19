
module Shell
  module Commands
    class Assign < Command
      def help
        return <<-EOF
Assign a region. Use with caution. If region already assigned,
this command will do a force reassign. For experts only.
Examples:

  bigdb> assign 'REGIONNAME'
  bigdb> assign 'ENCODED_REGIONNAME'
EOF
      end

      def command(region_name)
        format_simple_command do
          admin.assign(region_name)
        end
      end
    end
  end
end
