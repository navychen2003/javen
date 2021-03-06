package org.apache.james.mime4j.field.address.parser;

@SuppressWarnings({"all"})
public class AddressListParser/*@bgen(jjtree)*/implements AddressListParserTreeConstants, AddressListParserConstants {/*@bgen(jjtree)*/
	  protected JJTAddressListParserState jjtree = new JJTAddressListParserState();public static void main(String args[]) throws ParseException {
	                while (true) {
	                    try {
	                                AddressListParser parser = new AddressListParser(System.in);
	                        parser.parseLine();
	                        ((SimpleNode)parser.jjtree.rootNode()).dump("> ");
	                    } catch (Exception x) {
	                                x.printStackTrace();
	                                return;
	                    }
	                }
	    }

	    private static void log(String msg) {
	        System.out.print(msg);
	    }

	    public ASTaddress_list parse() throws ParseException {
	        try {
	            parseAll();
	            return (ASTaddress_list)jjtree.rootNode();
	        } catch (TokenMgrError tme) {
	            throw new ParseException(tme.getMessage());
	        }
	    }


	    void jjtreeOpenNodeScope(Node n) {
	        ((SimpleNode)n).firstToken = getToken(1);
	    }

	    void jjtreeCloseNodeScope(Node n) {
	        ((SimpleNode)n).lastToken = getToken(0);
	    }

	  final public void parseLine() throws ParseException {
	    address_list();
	    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	    case 1:
	      jj_consume_token(1);
	      break;
	    default:
	      jj_la1[0] = jj_gen;
	      ;
	    }
	    jj_consume_token(2);
	  }

	  final public void parseAll() throws ParseException {
	    address_list();
	    jj_consume_token(0);
	  }

	  final public void address_list() throws ParseException {
	 /*@bgen(jjtree) address_list */
	  ASTaddress_list jjtn000 = new ASTaddress_list(JJTADDRESS_LIST);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	      case 6:
	      case DOTATOM:
	      case QUOTEDSTRING:
	        address();
	        break;
	      default:
	        jj_la1[1] = jj_gen;
	        ;
	      }
	      label_1:
	      while (true) {
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 3:
	          ;
	          break;
	        default:
	          jj_la1[2] = jj_gen;
	          break label_1;
	        }
	        jj_consume_token(3);
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 6:
	        case DOTATOM:
	        case QUOTEDSTRING:
	          address();
	          break;
	        default:
	          jj_la1[3] = jj_gen;
	          ;
	        }
	      }
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void address() throws ParseException {
	 /*@bgen(jjtree) address */
	  ASTaddress jjtn000 = new ASTaddress(JJTADDRESS);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      if (jj_2_1(2147483647)) {
	        addr_spec();
	      } else {
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 6:
	          angle_addr();
	          break;
	        case DOTATOM:
	        case QUOTEDSTRING:
	          phrase();
	          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	          case 4:
	            group_body();
	            break;
	          case 6:
	            angle_addr();
	            break;
	          default:
	            jj_la1[4] = jj_gen;
	            jj_consume_token(-1);
	            throw new ParseException();
	          }
	          break;
	        default:
	          jj_la1[5] = jj_gen;
	          jj_consume_token(-1);
	          throw new ParseException();
	        }
	      }
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void mailbox() throws ParseException {
	 /*@bgen(jjtree) mailbox */
	  ASTmailbox jjtn000 = new ASTmailbox(JJTMAILBOX);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      if (jj_2_2(2147483647)) {
	        addr_spec();
	      } else {
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 6:
	          angle_addr();
	          break;
	        case DOTATOM:
	        case QUOTEDSTRING:
	          name_addr();
	          break;
	        default:
	          jj_la1[6] = jj_gen;
	          jj_consume_token(-1);
	          throw new ParseException();
	        }
	      }
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void name_addr() throws ParseException {
	 /*@bgen(jjtree) name_addr */
	  ASTname_addr jjtn000 = new ASTname_addr(JJTNAME_ADDR);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      phrase();
	      angle_addr();
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void group_body() throws ParseException {
	 /*@bgen(jjtree) group_body */
	  ASTgroup_body jjtn000 = new ASTgroup_body(JJTGROUP_BODY);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      jj_consume_token(4);
	      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	      case 6:
	      case DOTATOM:
	      case QUOTEDSTRING:
	        mailbox();
	        break;
	      default:
	        jj_la1[7] = jj_gen;
	        ;
	      }
	      label_2:
	      while (true) {
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 3:
	          ;
	          break;
	        default:
	          jj_la1[8] = jj_gen;
	          break label_2;
	        }
	        jj_consume_token(3);
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 6:
	        case DOTATOM:
	        case QUOTEDSTRING:
	          mailbox();
	          break;
	        default:
	          jj_la1[9] = jj_gen;
	          ;
	        }
	      }
	      jj_consume_token(5);
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void angle_addr() throws ParseException {
	 /*@bgen(jjtree) angle_addr */
	  ASTangle_addr jjtn000 = new ASTangle_addr(JJTANGLE_ADDR);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      jj_consume_token(6);
	      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	      case 8:
	        route();
	        break;
	      default:
	        jj_la1[10] = jj_gen;
	        ;
	      }
	      addr_spec();
	      jj_consume_token(7);
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void route() throws ParseException {
	 /*@bgen(jjtree) route */
	  ASTroute jjtn000 = new ASTroute(JJTROUTE);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      jj_consume_token(8);
	      domain();
	      label_3:
	      while (true) {
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 3:
	        case 8:
	          ;
	          break;
	        default:
	          jj_la1[11] = jj_gen;
	          break label_3;
	        }
	        label_4:
	        while (true) {
	          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	          case 3:
	            ;
	            break;
	          default:
	            jj_la1[12] = jj_gen;
	            break label_4;
	          }
	          jj_consume_token(3);
	        }
	        jj_consume_token(8);
	        domain();
	      }
	      jj_consume_token(4);
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void phrase() throws ParseException {
	 /*@bgen(jjtree) phrase */
	  ASTphrase jjtn000 = new ASTphrase(JJTPHRASE);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      label_5:
	      while (true) {
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case DOTATOM:
	          jj_consume_token(DOTATOM);
	          break;
	        case QUOTEDSTRING:
	          jj_consume_token(QUOTEDSTRING);
	          break;
	        default:
	          jj_la1[13] = jj_gen;
	          jj_consume_token(-1);
	          throw new ParseException();
	        }
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case DOTATOM:
	        case QUOTEDSTRING:
	          ;
	          break;
	        default:
	          jj_la1[14] = jj_gen;
	          break label_5;
	        }
	      }
	    } finally {
	  if (jjtc000) {
	    jjtree.closeNodeScope(jjtn000, true);
	    jjtreeCloseNodeScope(jjtn000);
	  }
	    }
	  }

	  final public void addr_spec() throws ParseException {
	 /*@bgen(jjtree) addr_spec */
	  ASTaddr_spec jjtn000 = new ASTaddr_spec(JJTADDR_SPEC);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);
	    try {
	      local_part();
	      jj_consume_token(8);
	      domain();
	    } catch (Throwable jjte000) {
	          if (jjtc000) {
	            jjtree.clearNodeScope(jjtn000);
	            jjtc000 = false;
	          } else {
	            jjtree.popNode();
	          }
	          if (jjte000 instanceof RuntimeException) {
	            {if (true) throw (RuntimeException)jjte000;}
	          }
	          if (jjte000 instanceof ParseException) {
	            {if (true) throw (ParseException)jjte000;}
	          }
	          {if (true) throw (Error)jjte000;}
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void local_part() throws ParseException {
	 /*@bgen(jjtree) local_part */
	  ASTlocal_part jjtn000 = new ASTlocal_part(JJTLOCAL_PART);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);Token t;
	    try {
	      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	      case DOTATOM:
	        t = jj_consume_token(DOTATOM);
	        break;
	      case QUOTEDSTRING:
	        t = jj_consume_token(QUOTEDSTRING);
	        break;
	      default:
	        jj_la1[15] = jj_gen;
	        jj_consume_token(-1);
	        throw new ParseException();
	      }
	      label_6:
	      while (true) {
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 9:
	        case DOTATOM:
	        case QUOTEDSTRING:
	          ;
	          break;
	        default:
	          jj_la1[16] = jj_gen;
	          break label_6;
	        }
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case 9:
	          t = jj_consume_token(9);
	          break;
	        default:
	          jj_la1[17] = jj_gen;
	          ;
	        }
	                        if (t.image.charAt(t.image.length() - 1) != '.' || t.kind == AddressListParserConstants.QUOTEDSTRING)
	                                {if (true) throw new ParseException("Words in local part must be separated by '.'");}
	        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	        case DOTATOM:
	          t = jj_consume_token(DOTATOM);
	          break;
	        case QUOTEDSTRING:
	          t = jj_consume_token(QUOTEDSTRING);
	          break;
	        default:
	          jj_la1[18] = jj_gen;
	          jj_consume_token(-1);
	          throw new ParseException();
	        }
	      }
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final public void domain() throws ParseException {
	 /*@bgen(jjtree) domain */
	  ASTdomain jjtn000 = new ASTdomain(JJTDOMAIN);
	  boolean jjtc000 = true;
	  jjtree.openNodeScope(jjtn000);
	  jjtreeOpenNodeScope(jjtn000);Token t;
	    try {
	      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	      case DOTATOM:
	        t = jj_consume_token(DOTATOM);
	        label_7:
	        while (true) {
	          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	          case 9:
	          case DOTATOM:
	            ;
	            break;
	          default:
	            jj_la1[19] = jj_gen;
	            break label_7;
	          }
	          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
	          case 9:
	            t = jj_consume_token(9);
	            break;
	          default:
	            jj_la1[20] = jj_gen;
	            ;
	          }
	                                if (t.image.charAt(t.image.length() - 1) != '.')
	                                        {if (true) throw new ParseException("Atoms in domain names must be separated by '.'");}
	          t = jj_consume_token(DOTATOM);
	        }
	        break;
	      case DOMAINLITERAL:
	        jj_consume_token(DOMAINLITERAL);
	        break;
	      default:
	        jj_la1[21] = jj_gen;
	        jj_consume_token(-1);
	        throw new ParseException();
	      }
	    } finally {
	          if (jjtc000) {
	            jjtree.closeNodeScope(jjtn000, true);
	            jjtreeCloseNodeScope(jjtn000);
	          }
	    }
	  }

	  final private boolean jj_2_1(int xla) {
	    jj_la = xla; jj_lastpos = jj_scanpos = token;
	    try { return !jj_3_1(); }
	    catch(LookaheadSuccess ls) { return true; }
	    finally { jj_save(0, xla); }
	  }

	  final private boolean jj_2_2(int xla) {
	    jj_la = xla; jj_lastpos = jj_scanpos = token;
	    try { return !jj_3_2(); }
	    catch(LookaheadSuccess ls) { return true; }
	    finally { jj_save(1, xla); }
	  }

	  final private boolean jj_3R_11() {
	    Token xsp;
	    xsp = jj_scanpos;
	    if (jj_scan_token(9)) jj_scanpos = xsp;
	    xsp = jj_scanpos;
	    if (jj_scan_token(14)) {
	    jj_scanpos = xsp;
	    if (jj_scan_token(31)) return true;
	    }
	    return false;
	  }

	  final private boolean jj_3R_13() {
	    Token xsp;
	    xsp = jj_scanpos;
	    if (jj_scan_token(9)) jj_scanpos = xsp;
	    if (jj_scan_token(DOTATOM)) return true;
	    return false;
	  }

	  final private boolean jj_3R_8() {
	    if (jj_3R_9()) return true;
	    if (jj_scan_token(8)) return true;
	    if (jj_3R_10()) return true;
	    return false;
	  }

	  final private boolean jj_3_1() {
	    if (jj_3R_8()) return true;
	    return false;
	  }

	  final private boolean jj_3R_12() {
	    if (jj_scan_token(DOTATOM)) return true;
	    Token xsp;
	    while (true) {
	      xsp = jj_scanpos;
	      if (jj_3R_13()) { jj_scanpos = xsp; break; }
	    }
	    return false;
	  }

	  final private boolean jj_3R_10() {
	    Token xsp;
	    xsp = jj_scanpos;
	    if (jj_3R_12()) {
	    jj_scanpos = xsp;
	    if (jj_scan_token(18)) return true;
	    }
	    return false;
	  }

	  final private boolean jj_3_2() {
	    if (jj_3R_8()) return true;
	    return false;
	  }

	  final private boolean jj_3R_9() {
	    Token xsp;
	    xsp = jj_scanpos;
	    if (jj_scan_token(14)) {
	    jj_scanpos = xsp;
	    if (jj_scan_token(31)) return true;
	    }
	    while (true) {
	      xsp = jj_scanpos;
	      if (jj_3R_11()) { jj_scanpos = xsp; break; }
	    }
	    return false;
	  }

	  public AddressListParserTokenManager token_source;
	  SimpleCharStream jj_input_stream;
	  public Token token, jj_nt;
	  private int jj_ntk;
	  private Token jj_scanpos, jj_lastpos;
	  private int jj_la;
	  public boolean lookingAhead = false;
	  private boolean jj_semLA;
	  private int jj_gen;
	  final private int[] jj_la1 = new int[22];
	  static private int[] jj_la1_0;
	  static private int[] jj_la1_1;
	  static {
	      jj_la1_0();
	      jj_la1_1();
	   }
	   private static void jj_la1_0() {
	      jj_la1_0 = new int[] {0x2,0x80004040,0x8,0x80004040,0x50,0x80004040,0x80004040,0x80004040,0x8,0x80004040,0x100,0x108,0x8,0x80004000,0x80004000,0x80004000,0x80004200,0x200,0x80004000,0x4200,0x200,0x44000,};
	   }
	   private static void jj_la1_1() {
	      jj_la1_1 = new int[] {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,};
	   }
	  final private JJCalls[] jj_2_rtns = new JJCalls[2];
	  private boolean jj_rescan = false;
	  private int jj_gc = 0;

	  public AddressListParser(java.io.InputStream stream) {
	     this(stream, null);
	  }
	  public AddressListParser(java.io.InputStream stream, String encoding) {
	    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	    token_source = new AddressListParserTokenManager(jj_input_stream);
	    token = new Token();
	    jj_ntk = -1;
	    jj_gen = 0;
	    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
	    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
	  }

	  public void ReInit(java.io.InputStream stream) {
	     ReInit(stream, null);
	  }
	  public void ReInit(java.io.InputStream stream, String encoding) {
	    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	    token_source.ReInit(jj_input_stream);
	    token = new Token();
	    jj_ntk = -1;
	    jjtree.reset();
	    jj_gen = 0;
	    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
	    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
	  }

	  public AddressListParser(java.io.Reader stream) {
	    jj_input_stream = new SimpleCharStream(stream, 1, 1);
	    token_source = new AddressListParserTokenManager(jj_input_stream);
	    token = new Token();
	    jj_ntk = -1;
	    jj_gen = 0;
	    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
	    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
	  }

	  public void ReInit(java.io.Reader stream) {
	    jj_input_stream.ReInit(stream, 1, 1);
	    token_source.ReInit(jj_input_stream);
	    token = new Token();
	    jj_ntk = -1;
	    jjtree.reset();
	    jj_gen = 0;
	    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
	    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
	  }

	  public AddressListParser(AddressListParserTokenManager tm) {
	    token_source = tm;
	    token = new Token();
	    jj_ntk = -1;
	    jj_gen = 0;
	    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
	    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
	  }

	  public void ReInit(AddressListParserTokenManager tm) {
	    token_source = tm;
	    token = new Token();
	    jj_ntk = -1;
	    jjtree.reset();
	    jj_gen = 0;
	    for (int i = 0; i < 22; i++) jj_la1[i] = -1;
	    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
	  }

	  final private Token jj_consume_token(int kind) throws ParseException {
	    Token oldToken;
	    if ((oldToken = token).next != null) token = token.next;
	    else token = token.next = token_source.getNextToken();
	    jj_ntk = -1;
	    if (token.kind == kind) {
	      jj_gen++;
	      if (++jj_gc > 100) {
	        jj_gc = 0;
	        for (int i = 0; i < jj_2_rtns.length; i++) {
	          JJCalls c = jj_2_rtns[i];
	          while (c != null) {
	            if (c.gen < jj_gen) c.first = null;
	            c = c.next;
	          }
	        }
	      }
	      return token;
	    }
	    token = oldToken;
	    jj_kind = kind;
	    throw generateParseException();
	  }

	  static private final class LookaheadSuccess extends java.lang.Error { }
	  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
	  final private boolean jj_scan_token(int kind) {
	    if (jj_scanpos == jj_lastpos) {
	      jj_la--;
	      if (jj_scanpos.next == null) {
	        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
	      } else {
	        jj_lastpos = jj_scanpos = jj_scanpos.next;
	      }
	    } else {
	      jj_scanpos = jj_scanpos.next;
	    }
	    if (jj_rescan) {
	      int i = 0; Token tok = token;
	      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
	      if (tok != null) jj_add_error_token(kind, i);
	    }
	    if (jj_scanpos.kind != kind) return true;
	    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
	    return false;
	  }

	  final public Token getNextToken() {
	    if (token.next != null) token = token.next;
	    else token = token.next = token_source.getNextToken();
	    jj_ntk = -1;
	    jj_gen++;
	    return token;
	  }

	  final public Token getToken(int index) {
	    Token t = lookingAhead ? jj_scanpos : token;
	    for (int i = 0; i < index; i++) {
	      if (t.next != null) t = t.next;
	      else t = t.next = token_source.getNextToken();
	    }
	    return t;
	  }

	  final private int jj_ntk() {
	    if ((jj_nt=token.next) == null)
	      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
	    else
	      return (jj_ntk = jj_nt.kind);
	  }

	  private java.util.Vector jj_expentries = new java.util.Vector();
	  private int[] jj_expentry;
	  private int jj_kind = -1;
	  private int[] jj_lasttokens = new int[100];
	  private int jj_endpos;

	  private void jj_add_error_token(int kind, int pos) {
	    if (pos >= 100) return;
	    if (pos == jj_endpos + 1) {
	      jj_lasttokens[jj_endpos++] = kind;
	    } else if (jj_endpos != 0) {
	      jj_expentry = new int[jj_endpos];
	      for (int i = 0; i < jj_endpos; i++) {
	        jj_expentry[i] = jj_lasttokens[i];
	      }
	      boolean exists = false;
	      for (java.util.Enumeration e = jj_expentries.elements(); e.hasMoreElements();) {
	        int[] oldentry = (int[])(e.nextElement());
	        if (oldentry.length == jj_expentry.length) {
	          exists = true;
	          for (int i = 0; i < jj_expentry.length; i++) {
	            if (oldentry[i] != jj_expentry[i]) {
	              exists = false;
	              break;
	            }
	          }
	          if (exists) break;
	        }
	      }
	      if (!exists) jj_expentries.addElement(jj_expentry);
	      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
	    }
	  }

	  public ParseException generateParseException() {
	    jj_expentries.removeAllElements();
	    boolean[] la1tokens = new boolean[34];
	    for (int i = 0; i < 34; i++) {
	      la1tokens[i] = false;
	    }
	    if (jj_kind >= 0) {
	      la1tokens[jj_kind] = true;
	      jj_kind = -1;
	    }
	    for (int i = 0; i < 22; i++) {
	      if (jj_la1[i] == jj_gen) {
	        for (int j = 0; j < 32; j++) {
	          if ((jj_la1_0[i] & (1<<j)) != 0) {
	            la1tokens[j] = true;
	          }
	          if ((jj_la1_1[i] & (1<<j)) != 0) {
	            la1tokens[32+j] = true;
	          }
	        }
	      }
	    }
	    for (int i = 0; i < 34; i++) {
	      if (la1tokens[i]) {
	        jj_expentry = new int[1];
	        jj_expentry[0] = i;
	        jj_expentries.addElement(jj_expentry);
	      }
	    }
	    jj_endpos = 0;
	    jj_rescan_token();
	    jj_add_error_token(0, 0);
	    int[][] exptokseq = new int[jj_expentries.size()][];
	    for (int i = 0; i < jj_expentries.size(); i++) {
	      exptokseq[i] = (int[])jj_expentries.elementAt(i);
	    }
	    return new ParseException(token, exptokseq, tokenImage);
	  }

	  final public void enable_tracing() {
	  }

	  final public void disable_tracing() {
	  }

	  final private void jj_rescan_token() {
	    jj_rescan = true;
	    for (int i = 0; i < 2; i++) {
	    try {
	      JJCalls p = jj_2_rtns[i];
	      do {
	        if (p.gen > jj_gen) {
	          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
	          switch (i) {
	            case 0: jj_3_1(); break;
	            case 1: jj_3_2(); break;
	          }
	        }
	        p = p.next;
	      } while (p != null);
	      } catch(LookaheadSuccess ls) { }
	    }
	    jj_rescan = false;
	  }

	  final private void jj_save(int index, int xla) {
	    JJCalls p = jj_2_rtns[index];
	    while (p.gen > jj_gen) {
	      if (p.next == null) { p = p.next = new JJCalls(); break; }
	      p = p.next;
	    }
	    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
	  }

	  static final class JJCalls {
	    int gen;
	    Token first;
	    int arg;
	    JJCalls next;
	  }

	}