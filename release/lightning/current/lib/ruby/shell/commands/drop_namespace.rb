
module Shell
  module Commands
    class DropNamespace < Command
      def help
        return <<-EOF
Drop the named namespace. The namespace must be empty.
EOF
      end

      def command(namespace)
        format_simple_command do
          admin.drop_namespace(namespace)
        end
      end
    end
  end
end
