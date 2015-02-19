
module Shell
  module Commands
    class ListNamespace < Command
      def help
        return <<-EOF
List all namespaces in bigdb. Optional regular expression parameter could
be used to filter the output. Examples:

  bigdb> list_namespace
  bigdb> list_namespace 'abc.*'
EOF
      end

      def command(regex = ".*")
        now = Time.now
        formatter.header([ "NAMESPACE" ])

        regex = /#{regex}/ unless regex.is_a?(Regexp)
        list = admin.list_namespace.grep(regex)
        list.each do |table|
          formatter.row([ table ])
        end

        formatter.footer(now, list.size)
      end
    end
  end
end
