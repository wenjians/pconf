package cli;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;


import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*; 

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import conf.ConfigNode;
import util.DataRange;
import util.PConfError;

public class CliNode {
    enum NodeType {INVALID, KEYWORD, PARAMETER, FUNCTION}
    
    public static final int CLI_MAX_KEYWORD_LENGTH = 13;
    
    private CliNodeList parent;
    private CliNodeList childs;
    
    private boolean isNodeShow;
    private NodeType nodeType;
    
    /* keyword is pure keyword:
     *   for CliNodeKeword, CliNodeParameter, it is the name
     *   for CliNodeFunction, it is function name
     * syntaxKeyword; only valid for CliNodeParameter, include optional flag
     *                e.g. <locktime>, [idletime] {enable|disable}
     */
    // for function, it is function name
    private String  keyword;        //
    private String  syntaxKeyword;
    
    static PConfError errProc = PConfError.getInstance();
    
    
    CliNode(NodeType aNodeType)  {
        assert aNodeType!=NodeType.INVALID;
        
        nodeType   = aNodeType;
        isNodeShow = true;
        keyword    = "";
        syntaxKeyword="";
        
        parent = null;
        childs = null;  
    }

    NodeType getNodeType() {   
        return nodeType;
    }
    
    boolean isFunctionNode() {
        return (getNodeType() == NodeType.FUNCTION);
    }
    
    boolean isParameterNode() {
        return (getNodeType() == NodeType.PARAMETER);
    }
    
    boolean isKeywordNode() {
    	return (getNodeType() == NodeType.KEYWORD);
    }

    public boolean equals(CliNode aNode) {
        return ((aNode.getNodeType() == nodeType) && aNode.getKeyword().contentEquals(getKeyword()));
    }
    
    
    CliNodeList getParent() {   
        return parent; 
    }
    
    void setParent(CliNodeList newParent)  {   
        parent = newParent;    
    }
    
    CliNodeList getChilds() {   
        return childs;  
    }
    
    void setChilds(CliNodeList newChilds)  {   
        childs = newChilds;     
    }
   
    
    boolean isNodeShow()  {   
        return isNodeShow;  
    }
    
    void setNodeShow(boolean newShow) {   
        isNodeShow = newShow;       
    }
    
    String  getSyntaxKeyword()  {   
        return syntaxKeyword;       
    }

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
    
    String getKeyword()  {   
        return keyword;         
    }

    
    boolean isKeywordValid() {
    	if (isKeywordNode()) {
    		return (getKeyword().length()<=CLI_MAX_KEYWORD_LENGTH);
    	}
    	
    	return true;
    }
    
    
    public String toString()   {   
        return keyword;         
    }
    
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




class CliNodeFunction extends CliNode {

    private CliCommand  cliCommand;
    
    CliNodeFunction()
    {
        super(CliNode.NodeType.FUNCTION);
        
        cliCommand = null;
    }
    
    void setCliCommand(CliCommand aCliCommand) { 
        cliCommand = aCliCommand; 
    }
    
    CliCommand getCliCommand() { 
        return cliCommand;    
    }
    
    
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
}


class CliNodeKeyword extends CliNode {
    
    CliNodeKeyword() {
        super(CliNode.NodeType.KEYWORD);
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


class CliDataTypeRule {
    
    String dataTypeShort;
    String dataTypeLong;
    String ruleDefault;
    String ruleMinimal;
    String ruleMaximum;
    boolean rangeNeed;
    
    CliDataTypeRule(String typeShort, String typeLong, boolean range, String ruleDef, String ruleMin, String ruleMax) {
        dataTypeShort = typeShort;
        dataTypeLong = typeLong;
        ruleDefault = ruleDef;
        ruleMinimal = ruleMin;
        ruleMaximum = ruleMax;
        rangeNeed   = range;
    }
    
    boolean matches(String str, String aRule) {
        if (aRule.isEmpty()) {
            return true;
        }
        
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
    private static final String ruleUint = "^([0-9]\\d*|0x[0-9a-fA-F]+)(ul)?$"; // unsigned int
    private static final String ruleInt  = "^(-?[0-9]\\d*||0x[0-9a-fA-F]+)$";   // signed int
    private static final String ruleIpa  = "^\\d+\\.\\d+\\.\\d+\\.\\d+$";       // IPv4 address
    private static final String ruleZero = "0";                 // 0
    
    private static CliDataTypeRule[] dataTypeRules = 
                        //type,  range need, default rule, minimal rule, maximum rule
        {new CliDataTypeRule("",     "",                false, "",        "",         ""      ), // default usage
         new CliDataTypeRule("Int",  "uint32",          true,  "",        ruleUint,   ruleUint), // unsigned int
         new CliDataTypeRule("Sint", "int32",           true,  "",        ruleInt,    ruleInt ), // signed int
         new CliDataTypeRule("Ustr", "string-word",     true,  "",        ruleUint,   ruleUint), // string, NO space 
         new CliDataTypeRule("Str",  "string",          true,  "",        ruleUint,   ruleUint), // string, have space
         new CliDataTypeRule("Case", "enumeration",     false, "",        ruleUint,   ruleUint), // choice
         new CliDataTypeRule("Ipa",  "ipv4-address",    false, ruleIpa,   ruleUint,   ruleUint), // IPv4 address, x.y.z.a
         new CliDataTypeRule("Ip6",  "ipv6-address",    false, "",        ruleZero,   ruleZero), // IPv6 address
         new CliDataTypeRule("Ipng", "ip-address",      false, "",        "",         ""      ), // IPv4 or IPv6
         new CliDataTypeRule("Addr", "memory-address",  true,  "",        "",         ""      ), // memory address, 0xffffffff
         new CliDataTypeRule("Macad","mac-address",     false, "",        "",         ""      ), // MAC address
        };
    
    private boolean required;   // is the parameter mandatory or optional
    private boolean requiredSet;// whether attribute <required> are defined
    
    private String value_def;
    private String value_min;
    private String value_max;
    private String value_unit;
    
    private boolean rangeSet;   // does the range defined?
    
    String dataType;        /* Int, Sint, Str, Ipa, ... */
    CliDataTypeRule dataTypeRule; 
    
    private List<String> helps;
    
    CliCommand cliCommand;
    
    String confReferName;
    ConfigNode confReferNode;
    
    CliNodeParameter()
    {
        super(CliNode.NodeType.PARAMETER);
        
        required  = true;
        value_def = "";
        value_min = "";
        value_max = "";
        value_unit= "";
        confReferName = "";
        confReferNode = null;
    
        rangeSet    = false;
        requiredSet = false;
        
        dataType="";
        dataTypeRule = dataTypeRules[0];
        helps    = new ArrayList<String> ();
    }

    public void setUnit(String unit) {  value_unit = unit;  }
    public String getUnit()          {  return value_unit;  }

    public boolean getRequired()     { return required;      }
    public boolean setRequired(String req) {
        req = req.trim();
        if (req.contentEquals("mandatory")) {
            required = true;
            requiredSet = true;
            return true;
        }
        else if (req.contentEquals("optional")){
            required = false;
            requiredSet = true;
            return true;
        }
            
        return false;
    }
    
    public String getRequiredName() {
        if (required) {
            return "mandatory";
        } else  {
            return "optional";
        }
    }

    public boolean equals(CliNodeParameter aParam) {
        return (getKeyword().contentEquals(aParam.getKeyword()) &&
                dataType.contentEquals(aParam.getDataType()) &&
                value_def.contentEquals(aParam.getDefValue()) &&
                value_min.contentEquals(aParam.getMinValue()) &&
                value_max.contentEquals(aParam.getMaxValue()) &&
                value_unit.contentEquals(aParam.getUnit()) &&
                getRequiredName().contentEquals(aParam.getRequiredName()));
    }
    
    public String getDataType()     { return dataType;      }
    
    boolean isParamString()         { 
        return dataType.contentEquals("Str") || dataType.contentEquals("Ustr"); 
    }
    
    public boolean setDataType(String aType)    {
        for (CliDataTypeRule rule: dataTypeRules) {
            if ((rule.dataTypeShort.contentEquals(aType)) ||
                (rule.dataTypeLong.contentEquals(aType)))   {
                
                dataTypeRule = rule;
                dataType     = rule.dataTypeShort;
                
                if (!dataTypeRule.rangeNeed) {
                    value_min = "0";
                    value_max = "0";
                }

                return true;
            }
        }
        
        return false;
    }
    
    
    public String getMaxValue()     { return value_max; }
    
    public boolean setMaxValue(String max) {
        max = max.trim();
        
        if (dataTypeRule.matches(max, dataTypeRule.ruleMaximum)) {
            value_max = max;
            return true;
        }
        
        return false;
    }
    
    public String getMinValue()     { return value_min;     }
    
    public boolean setMinValue(String min) {
        min = min.trim();
        
        if (dataTypeRule.matches(min, dataTypeRule.ruleMinimal)){
            value_min = min;
            return true;
        }
        
        return false;
        
    }
    
    
    public String getDefValue()     { return value_def;     }
    
    public boolean setDefValue(String def) {

        def = removeStringBracket(def.trim()).trim();
        if (def.length() == 0)
            return true;
        
        if (dataTypeRule.matches(def, dataTypeRule.ruleDefault)){
            value_def = def;
            return true;
        }
        
        return false;
    }
    
    
    /* format of range:
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
        //System.out.println("\nsetRange:" + aRange);
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
    
    public List<String> getHelps() { 
        return helps; 
    }
    
    public void addHelp(String aHelp)   {
        helps.add(aHelp);
    }

    public String getErrorMsg(int cliCmdType)
    {
        String errMsg= "";
        
        if (!requiredSet)
            errMsg += "Error: Parameter attribute <required> is not defined for " + getSyntaxKeyword() + "\n";
        
        if (dataType.isEmpty())
            errMsg += "Error: Parameter attribute <datatype> is not defined for " + getSyntaxKeyword() + "\n";

        if ((dataTypeRule.rangeNeed) &&
            (value_min.isEmpty() || value_max.isEmpty())) {
            errMsg += "Error: minimal or maximum value not defined for parameter " + getSyntaxKeyword() + "\n";
        }

        return errMsg;
    }
    
    boolean copyFromConfigNode(ConfigNode configNode) {
        if (!configNode.isLeaf() && !configNode.isLeafList()) {
            return false;
        }

        confReferNode = configNode;
        
        String yangTypeName = configNode.getBuiltinName();
        if (!setDataType(yangTypeName)) {
            errProc.addMessage("Error: unsupportted data type defined: <" + dataType 
                    + "> in reference parameter <" + yangTypeName + ">");
            return false;
        }

        // get the unit
        //System.out.println("units = "+configNode.getRecursionUnits());
        setUnit(configNode.getRecursionUnits());

        // get the keyword from CliNode
        setSyntaxKeyword(configNode.getCliName(getRequired()));

        if (!setDefValue(configNode.getRecursionDefault())) {
            errProc.addMessage("Error: unsupportted default format in reference parameter <" 
                        + yangTypeName + "> in CLI command :" + cliCommand.getKeywords());
            return false;
        }

        /* set the minimum and maximum value from ConfigNode */
        String min = configNode.getRangeMin();
        String max = configNode.getRangeMax();
        if (!(min.contentEquals("0") && max.contentEquals("0"))) {
            if (!setRange(min + ", " + max)) {
                errProc.addMessage("Error: unsupportted range format in reference parameter <" 
                        + yangTypeName + "> in CLI command :" + cliCommand.getKeywords());
                return false;
            }
        }

        addHelp(configNode.getDescription());
        
        return true;
    }
    
    /* for CRuntime string, in *.def definition, if it is not a *case* parameter, then
     * it is always using <> for both optional and mandatory, so here translate from [] to <>
     */
    String getCRuntimeSyntaxKeyword(String aKeyword)   {
        String cruntimeString;
    
        if ((aKeyword.startsWith("[") && aKeyword.endsWith("]")) &&
            (!required) && (!getDataType().contentEquals("Case"))
           ) {
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
        
        //System.out.println("parameter <" + result + ">");
        return result; 
    }
}

