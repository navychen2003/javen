
module Shell
  module Commands
    class Dumppaxos < Command
      def help
        return <<-EOF
Dump status of BigDB cluster as seen by Paxos.
EOF
      end

      def command
        puts admin.paxosdump
      end
    end
  end
end
