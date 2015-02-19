include Java

require 'bigdb/admin'
require 'bigdb/table'
require 'bigdb/security'

module Bigdb
  class Bigdb
    attr_accessor :configuration

    def initialize(config = nil)
      # Create configuration
      if config
        self.configuration = config
      else
        self.configuration = org.javenstudio.lightning.util.SimpleShell.getConf
        # Turn off retries in bigdb and ipc.  Human doesn't want to wait on N retries.
        configuration.setInt("bigdb.client.retries.number", 7)
        configuration.setInt("ipc.client.connect.max.retries", 3)
      end
    end

    def admin(formatter)
      ::Bigdb::Admin.new(configuration, formatter)
    end

    # Create new one each time
    def table(table, shell)
      ::Bigdb::Table.new(configuration, table, shell)
    end

    def replication_admin(formatter)
      ::Bigdb::RepAdmin.new(configuration, formatter)
    end

    def security_admin(formatter)
      ::Bigdb::SecurityAdmin.new(configuration, formatter)
    end
  end
end
