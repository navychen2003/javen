
module Shell
  module Commands
    class DisableAll < Command
      def help
        return <<-EOF
Disable all of tables matching the given regex:

bigdb> disable_all 't.*'
EOF
      end

      def command(regex)
        regex = /^#{regex}$/ unless regex.is_a?(Regexp)
        list = admin.list.grep(regex)
        count = list.size
        list.each do |table|
          formatter.row([ table ])
        end
        puts "\nDisable the above #{count} tables (y/n)?" unless count == 0
        answer = 'n'
        answer = gets.chomp unless count == 0
        puts "No tables matched the regex #{regex.to_s}" if count == 0
        return unless answer =~ /y.*/i
        failed = admin.disable_all(regex)
        puts "#{count - failed.size} tables successfully disabled"
        puts "#{failed.size} tables not disabled due to an exception: #{failed.join ','}" unless failed.size == 0
      end
    end
  end
end
