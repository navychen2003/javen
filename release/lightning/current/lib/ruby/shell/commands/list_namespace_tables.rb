
module Shell
  module Commands
    class ListNamespaceTables < Command
      def help
        return <<-EOF
List all tables that are members of the namespace.
Examples:

  bigdb> list_namespace_tables 'ns1'
EOF
      end

      def command(namespace)
        now = Time.now
        formatter.header([ "TABLE" ])

        list = admin.list_namespace_tables(namespace)
        list.each do |table|
          formatter.row([ table ])
        end

        formatter.footer(now, list.size)
      end
    end
  end
end
