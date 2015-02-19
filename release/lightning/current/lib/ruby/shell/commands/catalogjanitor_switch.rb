
module Shell
  module Commands
    class CatalogjanitorSwitch < Command
      def help
        return <<-EOF
Enable/Disable CatalogJanitor. Returns previous CatalogJanitor state.
Examples:

  bigdb> catalogjanitor_switch true
  bigdb> catalogjanitor_switch false
EOF
      end

      def command(enableDisable)
        format_simple_command do
          formatter.row([
            admin.catalogjanitor_switch(enableDisable)? "true" : "false"
          ])
        end
      end
    end
  end
end
