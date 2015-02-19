
module Shell
  module Commands
    class Create < Command
      def help
        return <<-EOF
Creates a table. Pass a table name, and a set of column family
specifications (at least one), and, optionally, table configuration.
Column specification can be a simple string (name), or a dictionary
(dictionaries are described below in main help output), necessarily 
including NAME attribute. 
Examples:

  bigdb> create 't1', {NAME => 'f1', VERSIONS => 5}
  bigdb> create 't1', {NAME => 'f1'}, {NAME => 'f2'}, {NAME => 'f3'}
  bigdb> # The above in shorthand would be the following:
  bigdb> create 't1', 'f1', 'f2', 'f3'
  bigdb> create 't1', {NAME => 'f1', VERSIONS => 1, TTL => 2592000, BLOCKCACHE => true}
  bigdb> create 't1', {NAME => 'f1', CONFIGURATION => {'bigdb.hstore.blockingStoreFiles' => '10'}}
  
Table configuration options can be put at the end.
Examples:

  bigdb> create 't1', 'f1', SPLITS => ['10', '20', '30', '40']
  bigdb> create 't1', 'f1', SPLITS_FILE => 'splits.txt', OWNER => 'johndoe'
  bigdb> create 't1', {NAME => 'f1', VERSIONS => 5}, METADATA => { 'mykey' => 'myvalue' }
  bigdb> # Optionally pre-split the table into NUMREGIONS, using
  bigdb> # SPLITALGO ("HexStringSplit", "UniformSplit" or classname)
  bigdb> create 't1', 'f1', {NUMREGIONS => 15, SPLITALGO => 'HexStringSplit'}
  bigdb> create 't1', 'f1', {NUMREGIONS => 15, SPLITALGO => 'HexStringSplit', CONFIGURATION => {'bigdb.hregion.scan.loadColumnFamiliesOnDemand' => 'true'}}

You can also keep around a reference to the created table:

  bigdb> t1 = create 't1', 'f1'

Which gives you a reference to the table named 't1', on which you can then
call methods.
EOF
      end

      def command(table, *args)
        format_simple_command do
          ret = admin.create(table, *args)
        end
        #and then return the table you just created
        table(table)
      end
    end
  end
end
