
module Shell
  module Commands
    class Whoami < Command
      def help
        return <<-EOF
Show the current bigdb user.
Syntax : whoami
For example:

    bigdb> whoami
EOF
      end

      def command()
        puts "#{org.javenstudio.raptor.bigdb.security.User.getCurrent().toString()}"
      end
    end
  end
end
