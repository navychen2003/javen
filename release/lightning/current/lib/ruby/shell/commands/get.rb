
module Shell
  module Commands
    class Get < Command
      def help
        return <<-EOF
Get row or cell contents; pass table name, row, and optionally
a dictionary of column(s), timestamp, timerange and versions. Examples:

  bigdb> get 't1', 'r1'
  bigdb> get 't1', 'r1', {TIMERANGE => [ts1, ts2]}
  bigdb> get 't1', 'r1', {COLUMN => 'c1'}
  bigdb> get 't1', 'r1', {COLUMN => ['c1', 'c2', 'c3']}
  bigdb> get 't1', 'r1', {COLUMN => 'c1', TIMESTAMP => ts1}
  bigdb> get 't1', 'r1', {COLUMN => 'c1', TIMERANGE => [ts1, ts2], VERSIONS => 4}
  bigdb> get 't1', 'r1', {COLUMN => 'c1', TIMESTAMP => ts1, VERSIONS => 4}
  bigdb> get 't1', 'r1', {FILTER => "ValueFilter(=, 'binary:abc')"}
  bigdb> get 't1', 'r1', 'c1'
  bigdb> get 't1', 'r1', 'c1', 'c2'
  bigdb> get 't1', 'r1', ['c1', 'c2']

Besides the default 'toStringBinary' format, 'get' also supports custom formatting by
column.  A user can define a FORMATTER by adding it to the column name in the get
specification.  The FORMATTER can be stipulated: 

 1. either as a org.javenstudio.raptor.bigdb.util.Bytes method name (e.g, toInt, toString)
 2. or as a custom class followed by method name: e.g. 'c(MyFormatterClass).format'.

Example formatting cf:qualifier1 and cf:qualifier2 both as Integers: 
  bigdb> get 't1', 'r1' {COLUMN => ['cf:qualifier1:toInt',
    'cf:qualifier2:c(org.javenstudio.raptor.bigdb.util.Bytes).toInt'] } 

Note that you can specify a FORMATTER by column only (cf:qualifer).  You cannot specify
a FORMATTER for all columns of a column family.
    
The same commands also can be run on a reference to a table (obtained via get_table or
create_table). Suppose you had a reference t to table 't1', the corresponding commands
would be:

  bigdb> t.get 'r1'
  bigdb> t.get 'r1', {TIMERANGE => [ts1, ts2]}
  bigdb> t.get 'r1', {COLUMN => 'c1'}
  bigdb> t.get 'r1', {COLUMN => ['c1', 'c2', 'c3']}
  bigdb> t.get 'r1', {COLUMN => 'c1', TIMESTAMP => ts1}
  bigdb> t.get 'r1', {COLUMN => 'c1', TIMERANGE => [ts1, ts2], VERSIONS => 4}
  bigdb> t.get 'r1', {COLUMN => 'c1', TIMESTAMP => ts1, VERSIONS => 4}
  bigdb> t.get 'r1', {FILTER => "ValueFilter(=, 'binary:abc')"}
  bigdb> t.get 'r1', 'c1'
  bigdb> t.get 'r1', 'c1', 'c2'
  bigdb> t.get 'r1', ['c1', 'c2']
EOF
      end

      def command(table, row, *args)
        get(table(table), row, *args)
      end

      def get(table, row, *args)
        now = Time.now
        formatter.header(["COLUMN", "CELL"])

        table._get_internal(row, *args) do |column, value|
          formatter.row([ column, value ])
        end

        formatter.footer(now)
      end
    end
  end
end

#add get command to table
::Bigdb::Table.add_shell_command('get')
