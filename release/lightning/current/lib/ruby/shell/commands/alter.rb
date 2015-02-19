
module Shell
  module Commands
    class Alter < Command
      def help
        return <<-EOF
Alter a table. Depending on the BigDB setting ("bigdb.online.schema.update.enable"),
the table must be disabled or not to be altered (see help 'disable').
You can add/modify/delete column families, as well as change table 
configuration. Column families work similarly to create; column family 
spec can either be a name string, or a dictionary with NAME attribute.
Dictionaries are described on the main help command output.

For example, to change or add the 'f1' column family in table 't1' from 
current value to keep a maximum of 5 cell VERSIONS, do:

  bigdb> alter 't1', NAME => 'f1', VERSIONS => 5

You can operate on several column families:

  bigdb> alter 't1', 'f1', {NAME => 'f2', IN_MEMORY => true}, {NAME => 'f3', VERSIONS => 5}

To delete the 'f1' column family in table 't1', use one of:

  bigdb> alter 't1', NAME => 'f1', METHOD => 'delete'
  bigdb> alter 't1', 'delete' => 'f1'

You can also change table-scope attributes like MAX_FILESIZE, READONLY, 
MEMSTORE_FLUSHSIZE, DEFERRED_LOG_FLUSH, etc. These can be put at the end;
for example, to change the max size of a region to 128MB, do:

  bigdb> alter 't1', MAX_FILESIZE => '134217728'

You can add a table coprocessor by setting a table coprocessor attribute:

  bigdb> alter 't1',
    'coprocessor'=>'hdfs:///foo.jar|com.foo.FooRegionObserver|1001|arg1=1,arg2=2'

Since you can have multiple coprocessors configured for a table, a
sequence number will be automatically appended to the attribute name
to uniquely identify it.

The coprocessor attribute must match the pattern below in order for
the framework to understand how to load the coprocessor classes:

  [coprocessor jar file location] | class name | [priority] | [arguments]

You can also set configuration settings specific to this table or column family:

  bigdb> alter 't1', CONFIGURATION => {'bigdb.hregion.scan.loadColumnFamiliesOnDemand' => 'true'}
  bigdb> alter 't1', {NAME => 'f2', CONFIGURATION => {'bigdb.hstore.blockingStoreFiles' => '10'}}

You can also remove a table-scope attribute:

  bigdb> alter 't1', METHOD => 'table_att_unset', NAME => 'MAX_FILESIZE'

  bigdb> alter 't1', METHOD => 'table_att_unset', NAME => 'coprocessor$1'

There could be more than one alteration in one command:

  bigdb> alter 't1', { NAME => 'f1', VERSIONS => 3 }, 
   { MAX_FILESIZE => '134217728' }, { METHOD => 'delete', NAME => 'f2' },
   OWNER => 'johndoe', METADATA => { 'mykey' => 'myvalue' }
EOF
      end

      def command(table, *args)
        format_simple_command do
          admin.alter(table, true, *args)
        end
      end
    end
  end
end
