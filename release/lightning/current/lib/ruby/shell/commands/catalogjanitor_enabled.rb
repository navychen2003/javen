
module Shell
  module Commands
    class CatalogjanitorEnabled < Command
      def help
        return <<-EOF
Query for the CatalogJanitor state (enabled/disabled?)
Examples:

  bigdb> catalogjanitor_enabled
EOF
      end

      def command()
        format_simple_command do
          formatter.row([
            admin.catalogjanitor_enabled()? "true" : "false"
          ])
        end
      end
    end
  end
end
