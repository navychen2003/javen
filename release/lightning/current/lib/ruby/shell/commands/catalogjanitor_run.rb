
module Shell
  module Commands
    class CatalogjanitorRun < Command
      def help
        return <<-EOF
Catalog janitor command to run the (garbage collection) scan from command line.

  bigdb> catalogjanitor_run

EOF
      end
      def command()
        format_simple_command do
          admin.catalogjanitor_run()
        end
      end
    end
  end
end
