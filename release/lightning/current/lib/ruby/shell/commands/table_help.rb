
module Shell
  module Commands
    class TableHelp < Command
      def help
        return Bigdb::Table.help
      end

      #just print the help
      def command
        # call the shell to get the nice formatting there
        @shell.help_command 'table_help'
      end
    end
  end
end
