module Shell
  module Commands
    class Command

      def initialize(shell)
        @shell = shell
      end

      #wrap an execution of cmd to catch bigdb exceptions
      # cmd - command name to execture
      # args - arguments to pass to the command
      def command_safe(debug, cmd = :command, *args)
        # send is internal ruby method to call 'cmd' with *args
        #(everything is a message, so this is just the formal semantics to support that idiom)
        translate_bigdb_exceptions(*args) { send(cmd,*args) }
      rescue => e
        rootCause = e
        while rootCause != nil && rootCause.respond_to?(:cause) && rootCause.cause != nil
          rootCause = rootCause.cause
        end
        puts
        puts "ERROR: #{rootCause}"
        puts "Backtrace: #{rootCause.backtrace.join("\n           ")}" if debug
        puts
        puts "Here is some help for this command:"
        puts help
        puts
      end

      def admin
        @shell.bigdb_admin
      end

      def table(name)
        @shell.bigdb_table(name)
      end

      def replication_admin
        @shell.bigdb_replication_admin
      end

      def security_admin
        @shell.bigdb_security_admin
      end

      #----------------------------------------------------------------------

      def formatter
        @shell.formatter
      end

      def format_simple_command
        now = Time.now
        yield
        formatter.header
        formatter.footer(now)
      end

      def format_and_return_simple_command
        now = Time.now
        ret = yield
        formatter.header
        formatter.footer(now)
        return ret
      end

      def translate_bigdb_exceptions(*args)
        yield
      rescue org.javenstudio.raptor.bigdb.TableNotFoundException
        raise "Unknown table #{args.first}!"
      rescue org.javenstudio.raptor.bigdb.regionserver.NoSuchColumnFamilyException
        valid_cols = table(args.first).get_all_columns.map { |c| c + '*' }
        raise "Unknown column family! Valid column names: #{valid_cols.join(", ")}"
      rescue org.javenstudio.raptor.bigdb.TableExistsException => e
        raise "Table already exists: #{args.first}!"
      end
    end
  end
end
