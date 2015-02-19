include Java

include_class('java.lang.Integer') {|package,name| "J#{name}" }
include_class('java.lang.Long') {|package,name| "J#{name}" }
include_class('java.lang.Boolean') {|package,name| "J#{name}" }

module BigDBConstants
  COLUMN = "COLUMN"
  COLUMNS = "COLUMNS"
  TIMESTAMP = "TIMESTAMP"
  TIMERANGE = "TIMERANGE"
  NAME = org.javenstudio.raptor.bigdb.DBConstants::NAME
  VERSIONS = org.javenstudio.raptor.bigdb.DBConstants::VERSIONS
  IN_MEMORY = org.javenstudio.raptor.bigdb.DBConstants::IN_MEMORY
  METADATA = org.javenstudio.raptor.bigdb.DBConstants::METADATA
  STOPROW = "STOPROW"
  STARTROW = "STARTROW"
  ENDROW = STOPROW
  RAW = "RAW"
  LIMIT = "LIMIT"
  METHOD = "METHOD"
  MAXLENGTH = "MAXLENGTH"
  CACHE_BLOCKS = "CACHE_BLOCKS"
  REPLICATION_SCOPE = "REPLICATION_SCOPE"
  INTERVAL = 'INTERVAL'
  CACHE = 'CACHE'
  FILTER = 'FILTER'
  SPLITS = 'SPLITS'
  SPLITS_FILE = 'SPLITS_FILE'
  SPLITALGO = 'SPLITALGO'
  NUMREGIONS = 'NUMREGIONS'
  CONFIGURATION = org.javenstudio.raptor.bigdb.DBConstants::CONFIGURATION

  # Load constants from bigdb java API
  def self.promote_constants(constants)
    # The constants to import are all in uppercase
    constants.each do |c|
      next if c =~ /DEFAULT_.*/ || c != c.upcase
      next if eval("defined?(#{c})")
      eval("#{c} = '#{c}'")
    end
  end

  promote_constants(org.javenstudio.raptor.bigdb.DBColumnDescriptor.constants)
  promote_constants(org.javenstudio.raptor.bigdb.DBTableDescriptor.constants)
end

# Include classes definition
require 'bigdb/bigdb'
require 'bigdb/admin'
require 'bigdb/table'
require 'bigdb/replication_admin'
require 'bigdb/security'
