package cli;

//import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;



public class CliNode {
    public static final int CLI_NODE_TYPE_KEYWORD   = 1;
    public static final int CLI_NODE_TYPE_PARAMETER = 2;
    public static final int CLI_NODE_TYPE_FUNCTION  = 3;
    
    public static final int CLI_MAX_KEYWORD_LENGTH = 13;
    
    private CliNodeList parent;
    private CliNodeList childs;
    
    private boolean isNodeShow;
    private int     nodeType;   /* keyword, parameter, function */
    //private int     cliCmdMode;   /* main or diag command */
    
    /* keyword is pure keyword:
     *   for CliNodeKeword, CliNodeParameter, it is the name
     *   for CliNodeFunction, it is function name
     * syntaxKeyword; only valid for CliNodeParameter, include optional flag
     *                e.g. <locktime>, [idletime] {enable|disable}
     */
    // for function, it is function name
    private String  keyword;        //
    private String  syntaxKeyword;
    
    CliNode() {
        
    }
    
    CliNode(int aNodeType)  {
        assert isValidNodeType(aNodeType);
        
        nodeType   = aNodeType;
        isNodeShow = true;
        keyword    = "";
        syntaxKeyword="";
        
        parent = null;
        childs = null;  
    }
    
    static boolean isValidNodeType(int aNodeType) {
        return ((aNodeType == CLI_NODE_TYPE_KEYWORD) ||
                (aNodeType == CLI_NODE_TYPE_PARAMETER) ||
                (aNodeType == CLI_NODE_TYPE_FUNCTION));
    }
    
    boolean isFunctionNode() {
        return (getNodeType() == CLI_NODE_TYPE_FUNCTION);
    }
    
    boolean isParameterNode() {
        return (getNodeType() == CLI_NODE_TYPE_PARAMETER);
    }
    
    boolean isKeywordNode() {
    	return (getNodeType() == CLI_NODE_TYPE_KEYWORD);
    }

    static CliNode createNode(int aNodeType){
        assert isValidNodeType(aNodeType);
        
        CliNode node;
        
        if (aNodeType == CLI_NODE_TYPE_KEYWORD)
            node = new CliNodeKeyword();
        if (aNodeType == CLI_NODE_TYPE_PARAMETER)
            node = new CliNodeParameter();
        if (aNodeType == CLI_NODE_TYPE_FUNCTION)
            node = new CliNodeFunction();
        else
            node = null;
        
        return node;
    }
    
    public boolean equals(CliNode aNode) {
        return ((aNode.getNodeType() == nodeType) && aNode.getKeyword().equals(getKeyword()));
    }
    
    CliNodeList getParent() {   return parent;  }
    
    CliNodeList getChilds() {   return childs;  }
    
    int     getNodeType()   {   return nodeType;}
    
    //int     getCliCmdMode()   {   return cliCmdMode;  }
    
    boolean isNodeShow()    {   return isNodeShow;  }
    
    void setNodeShow(boolean newShow)       {   isNodeShow = newShow;       }
    
    String  getSyntaxKeyword()              {   return syntaxKeyword;       }

    boolean setSyntaxKeyword(String aKeyword)   {   
        syntaxKeyword = aKeyword;       
    
        if (keyword.isEmpty()) 
        {
            if ((syntaxKeyword.startsWith("<") && syntaxKeyword.endsWith(">")) || 
                (syntaxKeyword.startsWith("[") && syntaxKeyword.endsWith("]")) ||
                (syntaxKeyword.startsWith("{") && syntaxKeyword.endsWith("}")))
            {
                keyword = syntaxKeyword.substring(1, syntaxKeyword.length()-1);
            }
            else
                keyword = syntaxKeyword;
        }
        
        return true;
    }
    
    String getKeyword()                     {   return keyword;         }
    //void   setKeyword(String aKeyword)        {   keyword = aKeyword;     }
    
    boolean isKeywordValid() {
    	if (isKeywordNode()) {
    		return (getKeyword().length()<=CLI_MAX_KEYWORD_LENGTH);
    	}
    	
    	return true;
    }
    
    void setParent(CliNodeList newParent)   {   parent = newParent;     }
    
    void setChilds(CliNodeList newChilds)   {   childs = newChilds;     }
    
    
    
    public String toString()                {   return keyword;         }
    
    /* 
     * tye total node print format is:
     *      {&uidiagParseNode251,&uidiagParseNode249,UI_HIDE,UIT_PARAM_OPT,"<cci>",0,1,16000,uiParseInt}
     * in this function, need print the following
     *      &uidiagParseNode251,&uidiagParseNode249,UI_HIDE
     */
    public String exportToCRuntime(String prefix) {
        String result;
        
        result  = "&" + prefix + "ParseNode" + childs.getCliNodeID() + ",";
        result += "&" + prefix + "ParseNode" + parent.getCliNodeID() + ",";
        if (isNodeShow())
            result += "UI_SHOW";
        else
            result += "UI_HIDE"; 

        return result;
    }
}






class CliNodeList {
    
    private long myCliNodeListID;
    
    private List<CliNode> cliEntities;

    CliNodeList(long nodeListID)    {
        myCliNodeListID = nodeListID;
        cliEntities = new ArrayList<CliNode> ();        
    }

    long getCliNodeID() { return myCliNodeListID; }
    
    List<CliNode> getCliEntities() { return cliEntities; }
    
    
    public boolean isNodeExist(CliNode node) {
        for (CliNode curNode: cliEntities) {
            if (curNode.equals(node))
                return true;
        }
        
        return false;
    }
    

    
    /* print the NodeList with following format
        uiParseEnt      uitopParseEnt0[] = {
        {&uitopParseNode24,&uitopParseNode0,UI_SHOW,UIT_STRING,"append",0,0,0,0},
        {&uitopParseNode75,&uitopParseNode0,UI_SHOW,UIT_STRING,"view",0,0,0,0},
        ...
        {0,0,UIT_NULL,0,0,0,0}
        };
        uiParseNd       uitopParseNode0 = {uitopParseEnt0 };
     */
    public String exportToCRuntimeFile(String prefix){
        String str = "";

        str += "uiParseEnt      " + prefix + "ParseEnt" + getCliNodeID() + "[] = {\n";
        
        for (CliNode curNode: cliEntities) {
            str += curNode.exportToCRuntime(prefix) + "\n";         
        }
        str += "{0,0,UIT_NULL,0,0,0,0}\n";
        str += "};\n";
        
        str += "uiParseNd       " + prefix + "ParseNode" + getCliNodeID() + 
               " = {" + prefix + "ParseEnt" + getCliNodeID() + " };\n";
        
        
        return str;
    }
}



class CliNodeFunction extends CliNode {

    private CliCommand  cliCommand;
    
    /*
    CliNodeFunction(CliCommand aCliCommand) {
        super(CliNode.CLI_NODE_TYPE_FUNCTION, aCliCommand.getDisplayMode());
        super.setKeyword(aCliCommand.getFunctionName());
        
        cliCommand = aCliCommand;
    }
    */
    
    CliNodeFunction()
    {
        super(CliNode.CLI_NODE_TYPE_FUNCTION);
        
        cliCommand = null;
    }
    
    void setCliCommand(CliCommand aCliCommand)  { cliCommand = aCliCommand; }
    
    CliCommand getCliCommand()      { return cliCommand;    }
    
    //void setFunctionName(String funName) { setKeyword(funName); }
    
    //String getFunctionName()  { return getKeyword();  }
    
    //int getPrivilege()        {  return privilege;    }
    
    
    /* following function translate to string mode in runtime:
     *   {&uidiagParseNode0,&uidiagParseNode56,UI_HIDE,UIT_FCNSYSTEM,(char *)UIHotpatchDisable,&helpUIHotpatchDisable,0,0,0},
     * and CliNode already do following
     *   &uidiagParseNode0,&uidiagParseNode56,UI_HIDE
     */
    //public String toString() {    return super.toString(); }
    
    public String exportToCRuntime(String prefix) 
    {
        String result;
        String privilegeName;
        
        if (cliCommand.getPrivilege() == CliCommand.CLI_COMMAND_PRIVILEGE_NETMAN)
            privilegeName = "UIT_FCNNETMAN";
        else if (cliCommand.getPrivilege() == CliCommand.CLI_COMMAND_PRIVILEGE_FUNCTION)
            privilegeName = "UIT_FCN";
        else if (cliCommand.getPrivilege() == CliCommand.CLI_COMMAND_PRIVILEGE_SYSTEM)
            privilegeName = "UIT_FCNSYSTEM";
        else if (cliCommand.getPrivilege() == CliCommand.CLI_COMMAND_PRIVILEGE_UPDATE)
            privilegeName = "UIT_FCNUPDATE";
        else if (cliCommand.getPrivilege() == CliCommand.CLI_COMMAND_PRIVILEGE_CODE)
            privilegeName = "UIT_FCNCODE";
        else
            privilegeName = "UIT_FCN";  

        result = "{";
        result += super.exportToCRuntime(prefix) + ",";
        result += privilegeName + ",";
        result += "(char *)" + getSyntaxKeyword() + ",";
        result += "&help" + getSyntaxKeyword() + ",0,0,0},";
        
        return result;
    }
    
    
    //private String cbFunctionName;
    
    
}



class CliNodeKeywordCheck {
    
    //static final int CLI_MAX_KEYWORD_LENGTH = 13;
    
    /* keyword list validation will be done in class CliCheckKeywordLen  */
    /*
    static String[] ignoredKeyList = 
        {"24h-start-time", 
        "adapt-profiles",       
        "authentication", 
        "autoclearcounter",
        "backplane_ports",
        "book_g7114gappfp",
        "box-conf-number",
        "cancel-per-interval",
        "clean-ctx-per-interval",
        "CmskRejStartSystem",
        "CmskRejStopSystem",
        "CmskRejStartVmg",
        "CmskRejStopVmg",
        "coco-req-param",
        "debug_panic_flag",
        "delete_filetest",
        "detailassociation",
        "detection-time", 
        "dlmcVdebug_rec",
        "error-counters", 
        "ethernet_port_test",
        "free_avail4new",
        "free-block-scan",
        "h248-coco-stats",
        "hold-off-timer",
        "ingressstatistics",
        "init_alarm_sup_time",
        "interim-update", 
        "intermediate_ts",
        "l4_checksum_update",
        "logging-buffer",
        "mgc-pend-limit",
        "minbuffergauge",
        "num_per_interval",
        "num-of-packets",
        "orig-dos-check",
        "PCI-port-packets",
        "physical-address",
        "protocol_level",
        "rdi-generation",
        "recovery_action",
        "rejectAlgorithm", 
        "reflection-pkt",
        "restart-interval",
        "sendbulkpacket",
        "show-link-data",
        "snmp-alarm-status",
        "special_registers",
        "syncClientDataArr",
        "sys_loop_count",
        "timestamp-reset",
        "trace-identifier",     
        "traffic-selector", 
        "trafficcounter",
        "tx-rate-change",
        "txn_cmd_threshold",
        "update-history",
        "use-dynamic-gfi",
        "wait-to-restore-timer",    
        "auto-negotiation"
        };
        */
    
    static boolean isValidKeyword(String keyword) {
        /*
        if (keyword.length() > CLI_MAX_KEYWORD_LENGTH) {
            for (int i=0; i<ignoredKeyList.length; i++) {
                String key = ignoredKeyList[i];
                
                if (keyword.equals(key))
                    return true;
            }
            return false;
        }
        */
        
        return true;
    }
}


class CliNodeKeyword extends CliNode {
    
    //private String keyword;

    CliNodeKeyword() {
        super(CliNode.CLI_NODE_TYPE_KEYWORD);
    }
    
    
    boolean setSyntaxKeyword(String aKeyword)   {   
        
        if (!CliNodeKeywordCheck.isValidKeyword(aKeyword))
            return false;
        
        return super.setSyntaxKeyword(aKeyword);
    }

    
    
    /* the output format as following
     *      {&uidiagParseNode4653,&uidiagParseNode0,UI_SHOW,UIT_STRING,"port",0,0,0,0},
     * and following is done is CliNode
     *      &uidiagParseNode4653,&uidiagParseNode0,UI_SHOW
     */
    public String exportToCRuntime(String prefix) {
        String result;
        
        result = "{";
        result += super.exportToCRuntime(prefix);
        result += ",UIT_STRING,\"" + getSyntaxKeyword() + "\",0,0,0,0},";
        
        return result;
    }
}





class DataTypeRule {
    
    String dataType;
    String ruleDefault;
    String ruleMinimal;
    String ruleMaximum;
    boolean rangeNeed;
    
    DataTypeRule(String type, boolean range, String ruleDef, String ruleMin, String ruleMax) {
        dataType    = type;
        ruleDefault = ruleDef;
        ruleMinimal = ruleMin;
        ruleMaximum = ruleMax;
        rangeNeed   = range;
    }
    
    boolean matches(String str, String aRule) {
        if (aRule.isEmpty()) {
            //System.out.println("empty");
            return true;
        }
        
        //System.out.println("rule = " + matchRule);
        Pattern pattern =Pattern.compile(aRule);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }
}

/*
 * [Int,<time-to-wait>,1,10]
 * (Case,{current|previous},0,0)
 * set sequence: data type, keyword, default, range
 */
class CliNodeParameter extends CliNode {

    ///^#?([a-f0-9]{6}|[a-f0-9]{3})$/
    private static final String ruleUint = "^([0-9]\\d*|0x[0-9a-fA-F]+)(ul)?$";      // unsigned int
    private static final String ruleInt  = "^(-?[0-9]\\d*||0x[0-9a-fA-F]+)$";   // signed int
    private static final String ruleIpa  = "^\\d+\\.\\d+\\.\\d+\\.\\d+$";       // IPv4 address
    private static final String ruleZero = "0";                 // 0
    
    private static DataTypeRule[] dataTypeRules = 
                        //type,  range need, default rule, minimal rule, maximum rule
        {new DataTypeRule("",     false, "",        "",         ""      ),      // for default usage
         new DataTypeRule("Int",  true,  "",        ruleUint,   ruleUint),      // unsigned int
         new DataTypeRule("Sint", true,  "",        ruleInt,    ruleInt ),      // signed int
         new DataTypeRule("Ustr", true,  "",        ruleUint,   ruleUint),      // string, don't have space 
         new DataTypeRule("Str",  true,  "",        ruleUint,   ruleUint),      // string, can have space
         new DataTypeRule("Case", false, "",        ruleUint,   ruleUint),      // choice
         new DataTypeRule("Ipa",  false, ruleIpa,   ruleUint,   ruleUint),      // IPv4 IP address, x.y.z.a
         new DataTypeRule("Ip6",  false, "",        ruleZero,   ruleZero),      // IPv6 address
         new DataTypeRule("Ipng", false, "",        "",         ""      ),      // IPv4 or IPv6
         new DataTypeRule("Addr", true,  "",        "",         ""      ),      // memory address, 0xffffffff
         new DataTypeRule("Macad",false, "",        "",         ""      ),      // MAC address
        };
    
    private boolean required;   // is the parameter mandatory or optional
    private boolean requiredSet;// whether attribute <required> are defined
    
    private String value_def;
    private String value_min;
    private String value_max;
    private String value_unit;
    
    private boolean rangeSet;   // does the range defined?
    
    //private boolean configtxtSet;
    String  configClient;
    String  configKeyword;
    String  configColumn;
    
    String  specialNote;
    
        
    String dataType;        /* Int, Sint, Str, Ipa, ... */
    DataTypeRule dataTypeRule; 
    
    private List<String> helps;
    
    CliNodeParameter()
    {
        super(CliNode.CLI_NODE_TYPE_PARAMETER);
        
        required  = true;
        value_def = "";
        value_min = "";
        value_max = "";
        value_unit= "";
    
        rangeSet    = false;
        requiredSet = false;
        
        configClient = "";
        configKeyword= "";
        configColumn = "";
        specialNote  = "";
        
        dataType="";
        dataTypeRule = dataTypeRules[0];
        helps    = new ArrayList<String> ();
        
    }
    public void setUnit(String unit) {  value_unit = unit;  }
    public String getUnit()          {  return value_unit;  }

    public boolean getRequired()    { return required;      }
    

    public boolean setRequired(String req) {
        req = req.trim();
        if (req.equals("mandatory")) {
            required = true;
            requiredSet = true;
            return true;
        }
        else if (req.equals("optional")){
            required = false;
            requiredSet = true;
            return true;
        }
            
        return false;
    }
    
    public String getRequiredName() {
        if (required)   return "mandatory";
        else            return "optional";
    }

    public boolean equals(CliNodeParameter aParam) {
        return (getKeyword().equals(aParam.getKeyword()) &&
                dataType.equals(aParam.getDataType()) &&
                value_def.equals(aParam.getDefValue()) &&
                value_min.equals(aParam.getMinValue()) &&
                value_max.equals(aParam.getMaxValue()) &&
                value_unit.equals(aParam.getUnit()) &&
                getRequiredName().equals(aParam.getRequiredName()));
    }
    
    public String getDataType()     { return dataType;      }
    
    boolean isParamString()         { 
        return dataType.equals("Str") || dataType.equals("Ustr"); 
    }
    
    public boolean setDataType(String aType)    {
        for (DataTypeRule rule: dataTypeRules) {
            if (rule.dataType.equals(aType)) {
                dataTypeRule = rule;
                dataType     = aType;
                
                if (!dataTypeRule.rangeNeed) {
                    value_min = "0";
                    value_max = "0";
                }

                return true;
            }
        }
        
        return false;
    }
    
    
    
    public boolean setMaxValue(String max) {
        max = max.trim();
        
        if (dataTypeRule.matches(max, dataTypeRule.ruleMaximum)) {
            value_max = max;
            return true;
        }
        
        return false;
    }
    
    public String getMaxValue()     { return value_max; }
    
    public boolean setMinValue(String min) {
        min = min.trim();
        
        if (dataTypeRule.matches(min, dataTypeRule.ruleMinimal)){
            value_min = min;
            return true;
        }
        
        return false;
        
    }
    
    public String getMinValue()     { return value_min;     }
    
    public boolean setDefValue(String def) {
        def = removeStringBracket(def.trim()).trim();
        
        if (dataTypeRule.matches(def, dataTypeRule.ruleDefault)){
            value_def = def;
            return true;
        }
        
        return false;
    }
    
    public String getDefValue()     { return value_def;     }
    

    
    /* forat of range:
     * Addr: valid memory address range, must provided, default can be "0-0xffffffff"
        Int/Sint: "a,b-c,d-e,f", support different range definition, "0-0" means disable the range
        string: the min/max length of string, "a-b"
     */
   
    private String removeStringBracket(String strBuffer) {
        while (strBuffer.startsWith("(")) {
            if (!strBuffer.endsWith(")"))
                break;
            
            strBuffer = strBuffer.substring(1, strBuffer.length()-1);
        }
        
        return strBuffer;
    }
    
    public boolean setRange(String aRange) {
        String[] tokens = aRange.split(",");
        
        if (tokens.length != 2) {
            return false;
        }
        
        if (!setMinValue(removeStringBracket(tokens[0].trim())))
            return false;
        
        if (!setMaxValue(removeStringBracket(tokens[1].trim())))
            return false;
        
        rangeSet = true;
        return true;
    }

    public boolean isRangeSet()     { return rangeSet;      }
    
    public void addHelp(String aHelp)   {
        helps.add(aHelp);
    }

    public List<String> getHelps()      { return helps; }
    
    public void setSpecialNote(String note) { specialNote = note.trim();    }
    public String getSpecialNote()          { return specialNote;           }
    
    
    public boolean setConfigTxt(String aClient, String aKeyword, String aCollum) {
        if (aClient.trim().isEmpty() || aKeyword.trim().isEmpty() || aCollum.trim().isEmpty())
            return false;
        
        configClient = aClient.trim();
        configKeyword= aKeyword.trim();
        configColumn = aCollum.trim();
        
        return true;
    }
    
    public String getErrorMsg(int cliCmdType)
    {
        String errMsg= "";

        if (!requiredSet)
            errMsg += "Error: Parameter attribute <required> is not defined for " + getSyntaxKeyword();
        
        if (dataType.isEmpty())
            errMsg += "Error: Parameter attribute <datatype> is not defined for " + getSyntaxKeyword();

        if ((dataTypeRule.rangeNeed) &&
            (value_min.isEmpty() || value_max.isEmpty())) {
            errMsg += "Error: minimal or maximum value not defined for parameter " + getSyntaxKeyword();
        }

        if (cliCmdType == CliCommand.CLI_COMMAND_TYPE_SYS_PARAM) {
            if (configClient.isEmpty() || configKeyword.isEmpty() || configColumn.isEmpty()) 
            {
                errMsg += "Error: config_txt is not defined for system parameter " + getSyntaxKeyword();
            }
        }
        
        return errMsg;
    }
    
    
    /* for CRuntime string, in *.def definition, if it is not a *case* parameter, then
     * it is always using <> for both optional and mandatory, so here translate from [] to <>
     */
    String getCRuntimeSyntaxKeyword(String aKeyword)   {
        String cruntimeString;
    
        if ((aKeyword.startsWith("[") && aKeyword.endsWith("]")) &&
            (!required) && (!getDataType().equals("Case"))
           )
        {
            cruntimeString = "<" + aKeyword.substring(1, aKeyword.length()-1) + ">";
        }
        else
            cruntimeString = aKeyword;
        
        return cruntimeString;
    }
    
    /* the output format as following
     *      {&uitopParseNode298,&uitopParseNode296,UI_SHOW,UIT_PARAM,"<filename>",0,0,128,uiParseUstr},
     * and following is done is CliNode
     *      &uidiagParseNode4653,&uidiagParseNode0,UI_SHOW
     */
    //{&uidiagParseNode3011,&uidiagParseNode3009,UI_SHOW,UIT_PARAM,"{none|critical|error|warning|notice|function|detail|debug|periodic|all}",0,warn,,uiParseCase},
    public String exportToCRuntime(String prefix)
    {
        String result = "{";
        
        result += super.exportToCRuntime(prefix) + ","; 
        
        if (required)
            result += "UIT_PARAM";
        else
            result += "UIT_PARAM_OPT";
        
        result += ",\"" + getCRuntimeSyntaxKeyword(getSyntaxKeyword()) + "\"";
        result += ",0," + value_min + ","+ value_max + ",";
        result += "uiParse" + dataType;
        result += "},";
        
        return result; 
    }
}

