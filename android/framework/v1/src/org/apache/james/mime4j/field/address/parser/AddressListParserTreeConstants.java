package org.apache.james.mime4j.field.address.parser;

public interface AddressListParserTreeConstants
{
  public int JJTVOID = 0;
  public int JJTADDRESS_LIST = 1;
  public int JJTADDRESS = 2;
  public int JJTMAILBOX = 3;
  public int JJTNAME_ADDR = 4;
  public int JJTGROUP_BODY = 5;
  public int JJTANGLE_ADDR = 6;
  public int JJTROUTE = 7;
  public int JJTPHRASE = 8;
  public int JJTADDR_SPEC = 9;
  public int JJTLOCAL_PART = 10;
  public int JJTDOMAIN = 11;


  public String[] jjtNodeName = {
    "void",
    "address_list",
    "address",
    "mailbox",
    "name_addr",
    "group_body",
    "angle_addr",
    "route",
    "phrase",
    "addr_spec",
    "local_part",
    "domain",
  };
}