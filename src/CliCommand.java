
import java.util.*;

public class CliCommand
{
    // define the CLI command is comming from *.def or *.xml or configuration module;
    public enum Source  { def, xml, conf }
    public enum CliMode { main, diag    }
    
    public static final int CLI_COMMAND_PRIVILEGE_INVALID   = 0;
    public static final int CLI_COMMAND_PRIVILEGE_NETMAN    = 1; /* it is admin */
    public static final int CLI_COMMAND_PRIVILEGE_ADMIN     = 1; /* it is admin */
    public static final int CLI_COMMAND_PRIVILEGE_FUNCTION  = 2; /* same as view */
    public static final int CLI_COMMAND_PRIVILEGE_VIEW      = 2; /* view is same as function */
    public static final int CLI_COMMAND_PRIVILEGE_SYSTEM    = 3;
    public static final int CLI_COMMAND_PRIVILEGE_UPDATE    = 4;
    public static final int CLI_COMMAND_PRIVILEGE_CODE      = 5;
    
    // full keyword for this CLI command, e.g. create:ip:if, view:version
    private String fullKeywords;  
    private String functionName;
    
    private List<String> helps;
    private List<String> examples;

    PConfError errorMsg;
    
    private int     privilege;

    private boolean isScmCommand;
    private boolean isCmdShow;
    
    private int     cliCmdType;
    
    CliMode         cliMode;
    Source          cliSource;
    
    List<CliNode>   cliCmdNodes;

    
    CliCommand()
    {
        fullKeywords  = "";
        functionName  = null;
        
        errorMsg = PConfError.getInstance();
        helps    = new ArrayList<String> ();
        examples = new ArrayList<String> ();
        
        cliCmdNodes   = new ArrayList<CliNode> ();
        
        // the following two set to default, it is okay even not explicitly defined
        isScmCommand = false;
        isCmdShow    = true;        
        privilege    = CLI_COMMAND_PRIVILEGE_INVALID;
        
        cliSource    = Source.def;
        cliMode      = CliMode.main;
    }

    static boolean isValidPrivilege(int aPrivilege){
        return  ((aPrivilege == CLI_COMMAND_PRIVILEGE_NETMAN) ||
                 (aPrivilege == CLI_COMMAND_PRIVILEGE_FUNCTION) ||
                 (aPrivilege == CLI_COMMAND_PRIVILEGE_SYSTEM) ||
                 (aPrivilege == CLI_COMMAND_PRIVILEGE_UPDATE) ||
                 (aPrivilege == CLI_COMMAND_PRIVILEGE_CODE));
    }
    
    public void    setSource(Source source)     { cliSource = source;       }
    public Source  getSource()                  { return cliSource;         }
    public boolean isDefCommand()               { return (cliSource == Source.def); }
    public boolean isXMLCommand()               { return (cliSource == Source.xml); }
    public boolean isConfCommand()               { return (cliSource == Source.conf); }

    public CliMode getCliCmdMode()              { return cliMode;           }
    public void setCliCmdMode(CliMode mode)     { cliMode = mode;           }
        
    public String getKeywords()                 { return fullKeywords;      }
    
    public void setFunctionName(String name)    { functionName = name;      }
    public String getFunctionName()             { return functionName;      }
    
    public void setSCMCommand(boolean isScm)    { isScmCommand = isScm;     }
    public boolean isSCMCommand()               { return isScmCommand;      }
    
    public static int getPrivilege(String aPrivilege)
    {
        int result = CLI_COMMAND_PRIVILEGE_INVALID ;
        
        if (aPrivilege.contentEquals("netman"))
        	result = CLI_COMMAND_PRIVILEGE_NETMAN;
        else if (aPrivilege.contentEquals("password"))
        	result = CLI_COMMAND_PRIVILEGE_NETMAN;
        else if (aPrivilege.contentEquals("user"))
        	result = CLI_COMMAND_PRIVILEGE_FUNCTION;
        else if (aPrivilege.contentEquals("view"))
        	result = CLI_COMMAND_PRIVILEGE_FUNCTION;
        else if (aPrivilege.contentEquals("system"))
        	result = CLI_COMMAND_PRIVILEGE_SYSTEM;
        else if (aPrivilege.contentEquals("update"))
        	result = CLI_COMMAND_PRIVILEGE_UPDATE;
        else if (aPrivilege.contentEquals("code"))
        	result = CLI_COMMAND_PRIVILEGE_CODE;
        else {
        	result = CLI_COMMAND_PRIVILEGE_INVALID;
        }
            
        return result;
    }
    
    public boolean setPrivilege(String aPrivilege)
    {
        boolean result = true ;
        
        privilege = getPrivilege(aPrivilege);
        if (privilege == CLI_COMMAND_PRIVILEGE_INVALID)
        {        
            addErrorMsg("Error: invalid privilege defined <" + aPrivilege + ">");
            result    = false;
        }
            
        return result;
    }
    
    public int getPrivilege()           { return privilege;         }
    
    public static String getPrivilegeName(int privilege) {
        String name;
        switch (privilege) {
        case CLI_COMMAND_PRIVILEGE_NETMAN:
            name = "password";
            break;
        case CLI_COMMAND_PRIVILEGE_FUNCTION:
            name = "view";
            break;
        case CLI_COMMAND_PRIVILEGE_SYSTEM:
            name = "system";
            break;
        case CLI_COMMAND_PRIVILEGE_UPDATE:
            name = "update";
            break;
        case CLI_COMMAND_PRIVILEGE_CODE:
            name = "code";
            break;
        default:
            name = "unknown(" + privilege + ")";
            break;
        }
        
        return name;        
    }
    
    public String getPrivilegeName() {
        return CliCommand.getPrivilegeName(privilege);
    }
    
    public boolean setDisplayMode(String mode)
    {
        if (mode.contentEquals("disp") || mode.contentEquals("display"))  {
            isCmdShow = true;
            return true;
        }
        else if (mode.contentEquals("hide")) {
            isCmdShow = false;
            return true;
        }
        
        addErrorMsg("Error: display mode defined <" + mode + ">");
        return false;
    }
    
    public boolean getDisplayMode()     {       return isCmdShow;   }
    
    public String getDisplayModeName() {
        if (isCmdShow)
            return "display";
        return "hide";
    }
    
    public void addErrorMsg(String errMsg)  { errorMsg.addMessage(errMsg);     }
    
    public void addHelp(String aHelp)       { helps.add(aHelp);         }
    public List<String> getHelp()			{ return helps;				}
    
    public void addExample(String aExample) { examples.add(aExample);   }

    public List<CliNode> getNodes()         { return cliCmdNodes;       }
    
    public void addNode(CliNode cliNode)    {
        cliCmdNodes.add(cliNode);
        
        if (cliNode.isKeywordNode()) {
            if (!fullKeywords.isEmpty())
                fullKeywords += ":";
            fullKeywords += cliNode.getSyntaxKeyword();
        }
    }
    
    public int getKeywordsCount() {
        
        int keyWordCount = 0;
        
        for (CliNode node: cliCmdNodes) {
            if (!node.isKeywordNode())
                break;
                
            
            keyWordCount ++;
        }
        
        return keyWordCount;
    }
    
    
    public boolean validate()
    {
        StringBuffer errMsg = new StringBuffer();
        
        if (fullKeywords.isEmpty())
            errMsg.append("Error: NO keyword defined");
        
        if (functionName.trim().length() == 0)
            errMsg.append("Error: No function name defined");
        
        if (!isValidPrivilege(privilege))
            errMsg.append("Error: privilege is wrong defined");

        if (isXMLCommand() && (helps.size()==0))
            errMsg.append("Error: NO help defined!");
        
        if ((cliMode == CliMode.main) && (isCmdShow == false))
        {
            errMsg.append("Error: main mode UI deosnot support hide UI");
        }
        
        //to check all mandatory parameter must before optional 
        boolean optParamStarted=false;
        for (CliNode node: cliCmdNodes) {
            if (node.getSyntaxKeyword().trim().isEmpty())
                addErrorMsg("Command or Parameter keyword is not defined!");
            
            if (node.isParameterNode()) {
                CliNodeParameter param = (CliNodeParameter)node;
                
                if (!param.getRequired())
                    optParamStarted = true;
                
                if (optParamStarted && param.getRequired() && isXMLCommand())
                    errMsg.append("mandatory parameter MUST before optional parameter");
                
                if (((getSource() == Source.xml) || (getSource() == Source.conf))
                        && (helps.size()==0))
                    errMsg.append("Error: help of parameter " + param.getSyntaxKeyword() + "is not defined!");
                    
                
                String paramErrorMsg = param.getErrorMsg(cliCmdType);
                if (!paramErrorMsg.isEmpty()) {
                    errMsg.append(paramErrorMsg);
                }
            }
        }
        
        if (errMsg.length() != 0) {
            errMsg.insert(0, "CLI command error found in keyword: <" + fullKeywords + ">\n");
            errorMsg.addMessage(errMsg.toString());
        }

        return !errorMsg.hasErrorMsg();
    }

  
    
    public String getSyntaxString()
    {
        String str = "";
        boolean firstNode = true;
        
        for (CliNode node: cliCmdNodes) {
            if (node.isFunctionNode())
                continue;
            
            if (firstNode) {
                firstNode = false;
            } else {
                str += " ";
            }
            str += node.getSyntaxKeyword();
        }       
        return str;
    }
    
    // split long infomration message into different length with total 80 chars
    private static String splitLines(String preMsg, String longMsg, boolean indent) {
        String retMsg = "";
        String oneLine = "";
        int     space = 0;      // whether add space before the string
        boolean firstLine = true;
        
        // split the string with space, including [ \t\n\x0B\f\r]
        String[] tokens = longMsg.split("\\s+");
        
        for (String str : tokens) {
            if ((oneLine.length() + str.length() + space)<= 64) {
                if (space == 1) 
                    oneLine += " ";
                oneLine += str;
                space = 1;
            }
            else {
                if (firstLine && preMsg!=null)
                    retMsg += String.format("%-11s: %s\n", preMsg, oneLine);
                else if (firstLine || !indent)
                	retMsg += String.format("%-11s  %s\n", " ", oneLine);
                else if (indent)
                    retMsg += String.format("%-11s    %s\n", " ", oneLine);
                    
                firstLine = false;
                oneLine = str;
                space = 1;
            }
        }
        if (oneLine.length() != 0) {
            if (firstLine && preMsg!=null)
                retMsg += String.format("%-11s: %s\n", preMsg, oneLine);
            else if (firstLine || !indent)
                retMsg += String.format("%-11s  %s\n", " ", oneLine);
            else
            	retMsg += String.format("%-11s    %s\n", " ", oneLine);
        }
        
        return retMsg;      
    }
    
    
    /* out get the help message for CLI command, the following is one example
    Syntax:       define ui inactivity timeout <lock-timeout-minutes>
                  <logout-timeout-minutes>
    Description:  Sets the amount of idle time in minutes, UI client will do some
                  action according to define timer
    Parameters    lock-timeout-minutes: after lock-timeout, the screen will be
                  cleared and locked
                      Default: 10
                      Values:  1-60
                  logout-timeout-minutes: after logout-timeout, UI sessions are
                  automatically logged out 
                      Default: 20
                      Value: 1-60
    Example       define ui inactivity timeout 10 20
                  define ui inactivity timeout 40 50

    Privilege:    PASSWORD
    Mode     :    main
    */
    
    public String getRuntimeHelpMsg(boolean needPrivilege) {
        //final int MsgWidthLen = 65;
        //final int preWidthLen = 15;
        
        String helpMsg="";
        
        helpMsg += splitLines("Syntax", getSyntaxString(), true);
        
        // get the help output for command help
        boolean firstLine=true;
        for (String help: helps) {
            if (firstLine) {
                helpMsg += splitLines("Description", help, false); 
                firstLine = false;
            } 
            else {
                helpMsg += splitLines(null, help, false);
            }
        }
        
        // get help output for parameters
        boolean paramNeed = true;
        String  paramMsg = null;
        for (CliNode node: cliCmdNodes) {
            if (node.isParameterNode()) {
                
                CliNodeParameter param = (CliNodeParameter)node;
                
                boolean keywordNeed=true;
                for (String help: param.getHelps()) {
                    String helpLine;

                    if (paramNeed) {
                        paramMsg = "Parameters";
                        paramNeed = false;
                    } else {
                        paramMsg = null;
                    }
                    
                    if (param.getDataType().contentEquals("Case"))
                        keywordNeed = false;
                    
                    if (keywordNeed) {
                        helpLine = param.getSyntaxKeyword() +": " + help;
                        keywordNeed = false;
                    } else {
                        helpLine = help;
                    }
                    
                    helpMsg += splitLines(paramMsg, helpLine, true);
                }
                
                if (param.getDefValue().length()>0) {
                    helpMsg += "                     Default: " + param.getDefValue().trim() + " " + 
                               param.getUnit().trim() + "\n";
                }
                
                if (param.isRangeSet()) {
                    helpMsg += "                     Range  : " + param.getMinValue().trim() + " - " + 
                               param.getMaxValue().trim() + " " + param.getUnit().trim();
                    
                    if (param.isParamString()) {
                    	helpMsg += " characters";
                    }
                    
                    helpMsg += "\n";
                }
            }
        }
        
        firstLine=true;
        for (String example: examples) {
            if (firstLine) {
                helpMsg += splitLines("Example", example, false);  
                firstLine = false;
            } 
            else {
                helpMsg += splitLines(null, example, false);
            }
        }
        
        helpMsg += "Mode       : " + getCliCmdMode() + "\n";
        
        if (needPrivilege) {
            helpMsg += "Privilege  : " + getPrivilegeName() + "\n";
        }

        return helpMsg;
    }
}
