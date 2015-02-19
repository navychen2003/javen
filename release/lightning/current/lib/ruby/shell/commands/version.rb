
module Shell
  module Commands
    class Version < Command
      def help
        return <<-EOF
Output this BigDB version
EOF
      end

      def command
        # Output version.
        puts "#{org.javenstudio.lightning.Constants.getVersion()}, " +
             "r#{org.javenstudio.lightning.Constants.getRevision()}, " +
             "#{org.javenstudio.lightning.Constants.getDate()}"
      end
    end
  end
end
