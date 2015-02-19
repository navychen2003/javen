
module Shell
  module Commands
    class AlterNamespace < Command
      def help
        return <<-EOF
Alter namespace properties.

To add/modify a property:

  bigdb> alter_namespace 'ns1', {METHOD => 'set', 'PROERTY_NAME' => 'PROPERTY_VALUE'}

To delete a property:

  bigdb> alter_namespace 'ns1', {METHOD => 'unset', NAME=>'PROERTY_NAME'}
EOF
      end

      def command(namespace, *args)
        format_simple_command do
          admin.alter_namespace(namespace, *args)
        end
      end
    end
  end
end
