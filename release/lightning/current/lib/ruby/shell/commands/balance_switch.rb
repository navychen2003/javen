
module Shell
  module Commands
    class BalanceSwitch < Command
      def help
        return <<-EOF
Enable/Disable balancer. Returns previous balancer state.
Examples:

  bigdb> balance_switch true
  bigdb> balance_switch false
EOF
      end

      def command(enableDisable)
        format_simple_command do
          formatter.row([
            admin.balance_switch(enableDisable)? "true" : "false"
          ])
        end
      end
    end
  end
end
