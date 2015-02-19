
require 'time'

module Shell
  module Commands
    class ListSnapshots < Command
      def help
        return <<-EOF
List all snapshots taken (by printing the names and relative information).
Optional regular expression parameter could be used to filter the output
by snapshot name.

Examples:
  bigdb> list_snapshots
  bigdb> list_snapshots 'abc.*'
EOF
      end

      def command(regex = ".*")
        now = Time.now
        formatter.header([ "SNAPSHOT", "TABLE + CREATION TIME"])

        regex = /#{regex}/ unless regex.is_a?(Regexp)
        list = admin.list_snapshot.select {|s| regex.match(s.getName)}
        list.each do |snapshot|
          creation_time = Time.at(snapshot.getCreationTime() / 1000).to_s
          formatter.row([ snapshot.getName, snapshot.getTable + " (" + creation_time + ")" ])
        end

        formatter.footer(now, list.size)
        return list.map { |s| s.getName() }
      end
    end
  end
end
