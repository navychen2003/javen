
module Shell
  module Commands
    class Count < Command
      def help
        return <<-EOF
Count the number of rows in a table.  Return value is the number of rows.
This operation may take a LONG time (Run '$HADOOP_HOME/bin/hadoop jar
bigdb.jar rowcount' to run a counting mapreduce job). Current count is shown
every 1000 rows by default. Count interval may be optionally specified. Scan
caching is enabled on count scans by default. Default cache size is 10 rows.
If your rows are small in size, you may want to increase this
parameter. Examples:

 bigdb> count 't1'
 bigdb> count 't1', INTERVAL => 100000
 bigdb> count 't1', CACHE => 1000
 bigdb> count 't1', INTERVAL => 10, CACHE => 1000

The same commands also can be run on a table reference. Suppose you had a reference
t to table 't1', the corresponding commands would be:

 bigdb> t.count
 bigdb> t.count INTERVAL => 100000
 bigdb> t.count CACHE => 1000
 bigdb> t.count INTERVAL => 10, CACHE => 1000
EOF
      end

      def command(table, params = {})
        count(table(table), params)
      end

      def count(table, params = {})
        # If the second parameter is an integer, then it is the old command syntax
        params = { 'INTERVAL' => params } if params.kind_of?(Fixnum)

        # Merge params with defaults
        params = {
          'INTERVAL' => 1000,
          'CACHE' => 10
        }.merge(params)

        # Call the counter method
        now = Time.now
        formatter.header
        count = table._count_internal(params['INTERVAL'].to_i, params['CACHE'].to_i) do |cnt, row|
          formatter.row([ "Current count: #{cnt}, row: #{row}" ])
        end
        formatter.footer(now, count)
        return count
      end
    end
  end
end

#Add the method table.count that calls count.count
::Bigdb::Table.add_shell_command("count")
