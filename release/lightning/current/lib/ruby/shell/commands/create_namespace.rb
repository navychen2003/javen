
module Shell
  module Commands
    class CreateNamespace < Command
      def help
        return <<-EOF
Create namespace; pass namespace name,
and optionally a dictionary of namespace configuration.
Examples:

  bigdb> create_namespace 'ns1'
  bigdb> create_namespace 'ns1', {'PROERTY_NAME'=>'PROPERTY_VALUE'}
EOF
      end

      def command(namespace, *args)
        format_simple_command do
          admin.create_namespace(namespace, *args)
        end
      end
    end
  end
end
