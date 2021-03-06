include Java

# Wrapper for org.javenstudio.raptor.bigdb.client.DBAdmin

module Bigdb
  class SecurityAdmin
    include BigDBConstants

    def initialize(configuration, formatter)
      @config = configuration
      @admin = org.javenstudio.raptor.bigdb.client.DBAdmin.new(configuration)
      @formatter = formatter
    end

    #----------------------------------------------------------------------------------------------
    def grant(user, permissions, table_name=nil, family=nil, qualifier=nil)
      security_available?

      # TODO: need to validate user name

      begin
        meta_table = org.javenstudio.raptor.bigdb.client.DBTable.new(@config,
          org.javenstudio.raptor.bigdb.security.access.AccessControlLists::ACL_TABLE_NAME)
        service = meta_table.coprocessorService(
          org.javenstudio.raptor.bigdb.DBConstants::EMPTY_START_ROW)

        protocol = org.javenstudio.raptor.bigdb.protobuf.generated.AccessControlProtos::
          AccessControlService.newBlockingStub(service)
        perm = org.javenstudio.raptor.bigdb.security.access.Permission.new(
          permissions.to_java_bytes)

        # Verify that the specified permission is valid
        if (permissions == nil || permissions.length == 0)
          raise(ArgumentError, "Invalid permission: no actions associated with user")
        end

        if (table_name != nil)
          tablebytes=table_name.to_java_bytes
          #check if the tablename passed is actually a namespace
          if (isNamespace?(table_name))
            # Namespace should exist first.
            namespace_name = table_name[1...table_name.length]
            raise(ArgumentError, "Can't find a namespace: #{namespace_name}") unless namespace_exists?(namespace_name)

            #We pass the namespace name along with "@" so that we can differentiate a namespace from a table.
            # invoke cp endpoint to perform access controlse
            org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.grant(
              protocol, user, tablebytes, perm.getActions())
          else
            # Table should exist
            raise(ArgumentError, "Can't find a table: #{table_name}") unless exists?(table_name)

            tableName = org.javenstudio.raptor.bigdb.TableName.valueOf(table_name.to_java_bytes)
            htd = @admin.getTableDescriptor(tablebytes)

            if (family != nil)
             raise(ArgumentError, "Can't find a family: #{family}") unless htd.hasFamily(family.to_java_bytes)
            end

            fambytes = family.to_java_bytes if (family != nil)
            qualbytes = qualifier.to_java_bytes if (qualifier != nil)

            # invoke cp endpoint to perform access controlse
            org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.grant(
              protocol, user, tableName, fambytes,
              qualbytes, perm.getActions())
          end
        else
          # invoke cp endpoint to perform access controlse
          org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.grant(
            protocol, user, perm.getActions())
        end

      ensure
        meta_table.close()
      end
    end

    #----------------------------------------------------------------------------------------------
    def revoke(user, table_name=nil, family=nil, qualifier=nil)
      security_available?

      # TODO: need to validate user name

      begin
        meta_table = org.javenstudio.raptor.bigdb.client.DBTable.new(@config,
          org.javenstudio.raptor.bigdb.security.access.AccessControlLists::ACL_TABLE_NAME)
        service = meta_table.coprocessorService(
          org.javenstudio.raptor.bigdb.DBConstants::EMPTY_START_ROW)

        protocol = org.javenstudio.raptor.bigdb.protobuf.generated.AccessControlProtos::
          AccessControlService.newBlockingStub(service)

        if (table_name != nil)
          #check if the tablename passed is actually a namespace
          if (isNamespace?(table_name))
            # Namespace should exist first.
            namespace_name = table_name[1...table_name.length]
            raise(ArgumentError, "Can't find a namespace: #{namespace_name}") unless namespace_exists?(namespace_name)

            #We pass the namespace name along with "@" so that we can differentiate a namespace from a table.
            tablebytes=table_name.to_java_bytes
            # invoke cp endpoint to perform access controlse
            org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.revoke(
              protocol, user, tablebytes)
          else
             # Table should exist
             raise(ArgumentError, "Can't find a table: #{table_name}") unless exists?(table_name)

             tableName = org.javenstudio.raptor.bigdb.TableName.valueOf(table_name.to_java_bytes)
             htd = @admin.getTableDescriptor(tableName)

             if (family != nil)
               raise(ArgumentError, "Can't find a family: #{family}") unless htd.hasFamily(family.to_java_bytes)
             end

             fambytes = family.to_java_bytes if (family != nil)
             qualbytes = qualifier.to_java_bytes if (qualifier != nil)

            # invoke cp endpoint to perform access controlse
            org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.revoke(
              protocol, user, tableName, fambytes, qualbytes)
          end
        else
          # invoke cp endpoint to perform access controlse
          perm = org.javenstudio.raptor.bigdb.security.access.Permission.new(''.to_java_bytes)
          org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.revoke(protocol, user, perm.getActions())
        end
      ensure
        meta_table.close()
      end
    end

    #----------------------------------------------------------------------------------------------
    def user_permission(table_name=nil)
      security_available?

      begin
        meta_table = org.javenstudio.raptor.bigdb.client.DBTable.new(@config,
          org.javenstudio.raptor.bigdb.security.access.AccessControlLists::ACL_TABLE_NAME)
        service = meta_table.coprocessorService(
          org.javenstudio.raptor.bigdb.DBConstants::EMPTY_START_ROW)

        protocol = org.javenstudio.raptor.bigdb.protobuf.generated.AccessControlProtos::
          AccessControlService.newBlockingStub(service)

        if (table_name != nil)
          #check if namespace is passed.
          if (isNamespace?(table_name))
            # Namespace should exist first.
            namespace_name = table_name[1...table_name.length]
            raise(ArgumentError, "Can't find a namespace: #{namespace_name}") unless namespace_exists?(namespace_name)
            # invoke cp endpoint to perform access controls
            perms = org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.getUserPermissions(
              protocol, table_name.to_java_bytes)
          else
             raise(ArgumentError, "Can't find table: #{table_name}") unless exists?(table_name)
             perms = org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.getUserPermissions(
               protocol, org.javenstudio.raptor.bigdb.TableName.valueOf(table_name))
          end
        else
          perms = org.javenstudio.raptor.bigdb.protobuf.ProtobufUtil.getUserPermissions(protocol)
        end
      ensure
        meta_table.close()
      end

      res = {}
      count  = 0
      perms.each do |value|
        user_name = String.from_java_bytes(value.getUser)
        table = (value.getTable != nil) ? value.getTable.toString() : ''
        family = (value.getFamily != nil) ? org.javenstudio.raptor.bigdb.util.Bytes::toStringBinary(value.getFamily) : ''
        qualifier = (value.getQualifier != nil) ? org.javenstudio.raptor.bigdb.util.Bytes::toStringBinary(value.getQualifier) : ''

        action = org.javenstudio.raptor.bigdb.security.access.Permission.new value.getActions

        if block_given?
          yield(user_name, "#{table},#{family},#{qualifier}: #{action.to_s}")
        else
          res[user_name] ||= {}
          res[user_name][family + ":" +qualifier] = action
        end
        count += 1
      end

      return ((block_given?) ? count : res)
    end

    # Does table exist?
    def exists?(table_name)
      @admin.tableExists(table_name)
    end

    def isNamespace?(table_name)
      table_name.start_with?('@')
    end

     # Does Namespace exist
    def namespace_exists?(namespace_name)
      namespaceDesc = @admin.getNamespaceDescriptor(namespace_name)
      if(namespaceDesc == nil)
        return false
      else
        return true
      end
    end

    # Make sure that security tables are available
    def security_available?()
      raise(ArgumentError, "DISABLED: Security features are not available") \
        unless exists?(org.javenstudio.raptor.bigdb.security.access.AccessControlLists::ACL_TABLE_NAME)
    end
  end
end
