
#java_import org.javenstudio.raptor.bigdb.filter.ParseFilter

module Shell
  module Commands
    class ShowFilters < Command
      def help
        return <<-EOF
Show all the filters in bigdb. Example:
  bigdb> show_filters

  Documentation on filters mentioned below can be found at: https://our.intern.facebook.com/intern/wiki/index.php/BigDB/Filter_Language
  ColumnPrefixFilter
  TimestampsFilter
  PageFilter
  .....
  KeyOnlyFilter
EOF
      end

      def command( )
        now = Time.now
        formatter.row(["Documentation on filters mentioned below can " +
                       "be found at: https://our.intern.facebook.com/intern/" +
                       "wiki/index.php/BigDB/Filter_Language"])

        #parseFilter = ParseFilter.new
        #supportedFilters = parseFilter.getSupportedFilters

        #supportedFilters.each do |filter|
        #  formatter.row([filter])
        #end
      end
    end
  end
end
