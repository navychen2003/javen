
module Shell
  module Commands
    class DescribeNamespace < Command
      def help
        return <<-EOF
Describe the named namespace. For example:
  bigdb> describe_namespace 'ns1'
EOF
      end

      def command(namespace)
        now = Time.now

        desc = admin.describe_namespace(namespace)

        formatter.header([ "DESCRIPTION" ], [ 64 ])
        formatter.row([ desc ], true, [ 64 ])
        formatter.footer(now)
      end
    end
  end
end
