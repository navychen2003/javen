
module Shell
  module Commands
    class List < Command
      def help
        return <<-EOF
List all tables in bigdb. Optional regular expression parameter could
be used to filter the output. Examples:

  bigdb> list
  bigdb> list 'abc.*'
EOF
      end

      def command(regex = ".*")
        now = Time.now
        formatter.header([ "TABLE" ])

        list = admin.list(regex)
        list.each do |table|
          formatter.row([ table ])
        end

        formatter.footer(now, list.size)
        return list
      end
    end
  end
end
