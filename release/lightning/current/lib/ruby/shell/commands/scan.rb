
module Shell
  module Commands
    class Scan < Command
      def help
        return <<-EOF
Scan a table; pass table name and optionally a dictionary of scanner
specifications.  Scanner specifications may include one or more of:
TIMERANGE, FILTER, LIMIT, STARTROW, STOPROW, TIMESTAMP, MAXLENGTH,
or COLUMNS, CACHE

If no columns are specified, all columns will be scanned.
To scan all members of a column family, leave the qualifier empty as in
'col_family:'.

The filter can be specified in two ways:
1. Using a filterString - more information on this is available in the
Filter Language document attached to the BIGDB-4176 JIRA
2. Using the entire package name of the filter.

Some examples:

  bigdb> scan 'bigdb:meta'
  bigdb> scan 'bigdb:meta', {COLUMNS => 'info:regioninfo'}
  bigdb> scan 't1', {COLUMNS => ['c1', 'c2'], LIMIT => 10, STARTROW => 'xyz'}
  bigdb> scan 't1', {COLUMNS => 'c1', TIMERANGE => [1303668804, 1303668904]}
  bigdb> scan 't1', {FILTER => "(PrefixFilter ('row2') AND
    (QualifierFilter (>=, 'binary:xyz'))) AND (TimestampsFilter ( 123, 456))"}
  bigdb> scan 't1', {FILTER =>
    org.javenstudio.raptor.bigdb.filter.ColumnPaginationFilter.new(1, 0)}

For experts, there is an additional option -- CACHE_BLOCKS -- which
switches block caching for the scanner on (true) or off (false).  By
default it is enabled.  Examples:

  bigdb> scan 't1', {COLUMNS => ['c1', 'c2'], CACHE_BLOCKS => false}

Also for experts, there is an advanced option -- RAW -- which instructs the
scanner to return all cells (including delete markers and uncollected deleted
cells). This option cannot be combined with requesting specific COLUMNS.
Disabled by default.  Example:

  bigdb> scan 't1', {RAW => true, VERSIONS => 10}

Besides the default 'toStringBinary' format, 'scan' supports custom formatting
by column.  A user can define a FORMATTER by adding it to the column name in
the scan specification.  The FORMATTER can be stipulated: 

 1. either as a org.javenstudio.raptor.bigdb.util.Bytes method name (e.g, toInt, toString)
 2. or as a custom class followed by method name: e.g. 'c(MyFormatterClass).format'.

Example formatting cf:qualifier1 and cf:qualifier2 both as Integers: 
  bigdb> scan 't1', {COLUMNS => ['cf:qualifier1:toInt',
    'cf:qualifier2:c(org.javenstudio.raptor.bigdb.util.Bytes).toInt'] } 

Note that you can specify a FORMATTER by column only (cf:qualifer).  You cannot
specify a FORMATTER for all columns of a column family.

Scan can also be used directly from a table, by first getting a reference to a
table, like such:

  bigdb> t = get_table 't'
  bigdb> t.scan

Note in the above situation, you can still provide all the filtering, columns,
options, etc as described above.

EOF
      end

      def command(table, args = {})
        scan(table(table), args)
      end

      #internal command that actually does the scanning
      def scan(table, args = {})
        now = Time.now
        formatter.header(["ROW", "COLUMN+CELL"])

        #actually do the scanning
        count = table._scan_internal(args) do |row, cells|
          formatter.row([ row, cells ])
        end

        formatter.footer(now, count)
      end
    end
  end
end

#Add the method table.scan that calls Scan.scan
::Bigdb::Table.add_shell_command("scan")
