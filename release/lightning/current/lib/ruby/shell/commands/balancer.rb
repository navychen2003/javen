
module Shell
  module Commands
    class Balancer < Command
      def help
        return <<-EOF
Trigger the cluster balancer. Returns true if balancer ran and was able to
tell the region servers to unassign all the regions to balance  (the re-assignment itself is async). 
Otherwise false (Will not run if regions in transition).
EOF
      end

      def command()
        format_simple_command do
          formatter.row([
            admin.balancer()? "true": "false"
          ])
        end
      end
    end
  end
end
