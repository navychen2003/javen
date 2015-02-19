
module Shell
  module Commands
    class HlogRoll < Command
      def help
        return <<-EOF
Roll the log writer. That is, start writing log messages to a new file.
The name of the regionserver should be given as the parameter.  A
'server_name' is the host, port plus startcode of a regionserver. For
example: host187.example.com,60020,1289493121758 (find servername in
master ui or when you do detailed status in shell)
EOF
      end

      def command(server_name)
        format_simple_command do
          admin.hlog_roll(server_name)
        end
      end
    end
  end
end
