

import java.util.*;
import java.io.*;

/* for each board, there are three type of command tree exist
 *  main: command tree used for main mode
 *  diag: command tree used for diag mode
 *  tmd:  commadn tree actually is not used
 */
/*
class CliCommandTreeBoard {
    CliCommandTree mainCmdTree;
    CliCommandTree diagCmdTree;
    CliCommandTree tdmCmdTree;
    
    CliCommandTreeBoard() {
        mainCmdTree = new CliCommandTree();
        diagCmdTree = new CliCommandTree();
        tdmCmdTree = new CliCommandTree();
    }
    
}
*/


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

public class CliCommandTree {
    
    public static final int PRINT_DECLARATION  = 1;
    public static final int PRINT_NODE_TABLES  = 2;
    
    // this is manage the CliNodeList index
    private long        curCliNodeListID;
    private CliNodeList cliCmdTree;
    private String      prefix;
    
    /* it used to store all node list which current tree have
     * which is very easy to walk through
     */
    private List<CliNodeList> cliCmdNodeLists;
    
    static private PConfError errProc = PConfError.getInstance();
    
    public CliCommandTree()
    {
        curCliNodeListID = 0;
        prefix           = null;
    
        cliCmdNodeLists = new ArrayList<CliNodeList> ();
        cliCmdTree      = newCliNodeList();     
    }
    
    public CliNodeList getRootEntry()   {   return cliCmdTree;  }
    public List<CliNodeList> getAllCmdNodeList()    { return cliCmdNodeLists;   }
    
    
    public boolean isRootNodeList(CliNodeList nodeList) {
        return nodeList.equals(cliCmdTree);
    }

    public boolean addCommand(CliCommand cliCommand) 
    {
        //System.out.println("CliCommand.addCommand");
        
        CliNode insertNode = null;
        
        CliNodeList curNodes = cliCmdTree; 
        CliNodeList parNodes = cliCmdTree;

        for (CliNode curNode: cliCommand.getNodes())
        {
            //System.out.println("add parameter idx " + idx);
            insertNode = addNodeEntry(curNodes, curNode, cliCommand);
            if (insertNode == null) {
                return false;
            }

            insertNode.setParent(parNodes);

            // create the children and set, if function node, set to command tree 
            if (insertNode.isFunctionNode()) {
                insertNode.setChilds(cliCmdTree);
            } else if (insertNode.getChilds()==null) {
                CliNodeList newList = newCliNodeList();
                insertNode.setChilds(newList);
            }
            
            parNodes = curNodes;
            curNodes = insertNode.getChilds();
        }
        
        return true;
    }


    public CliCommand getCommand(CliCommand aCommand) {

        CliCommand command=null;
        
        //System.out.println("searching CLI: " + aCommand.getSyntaxString() + ":" + aCommand.getKeywordsCount());
        
        //int i=0;
        CliNodeList curNodeList = cliCmdTree;
        
        for (CliNode cmdNode: aCommand.getNodes()) {
            //i++;
            //System.out.println(i+cmdNode.getKeyword());
            
            CliNode curNode = isNodeExist(curNodeList, cmdNode);
            
            if (curNode == null) {
                //System.out.println("keyword:" + cmdNode.getKeyword() + " not exist");
                return null;
            }
            
            if (curNode.isFunctionNode()) {
                command = ((CliNodeFunction)curNode).getCliCommand();
                break;
            }
            
            curNodeList = curNode.getChilds();
        }
            
        return command;
    }
        
    private CliNode isNodeExist(CliNodeList nodeList, CliNode aNode) {
        int idx=0;

        for (idx=0; idx<nodeList.getCliEntities().size(); idx++) 
        {
            CliNode curNode = nodeList.getCliEntities().get(idx);

            // for function, do NOT compare the function name
            if (curNode.isFunctionNode() && aNode.isFunctionNode())
                return curNode;
            
            /* for parameter, if there is only one parameter, then do not change keyword name */
            if (curNode.isParameterNode() && aNode.isParameterNode())
            {
            	if (nodeList.getCliEntities().size() == 1)
            		return curNode;
            }
            
            if (curNode.equals(aNode))
            {
                return curNode;
            }
        }
        
        return null;    
    }
    
    private CliNode addNodeEntry(CliNodeList nodeList, CliNode newNode, CliCommand cliCommand) 
    {
        int idx=0;

        for (idx=0; idx<nodeList.getCliEntities().size(); idx++) 
        {
            CliNode curNode = nodeList.getCliEntities().get(idx);
            
            if (curNode.getNodeType() != newNode.getNodeType())
                continue;
            
            if (cliCommand.isXMLCommand() && 
                curNode.isParameterNode() && newNode.isParameterNode()) {
                String errMsg="";
                
                errMsg += "Error: UI Command duplicated with keylist <" + cliCommand.getKeywords() + ">";
                errMsg += " new command function callback is <" + cliCommand.getFunctionName() + ">";

                //System.out.println(errMsg);
                errProc.addMessage(errMsg);
                
                return null;
            }
            
            // in same NodeList, there should NOT have two function
            if (curNode.isFunctionNode() && newNode.isFunctionNode()) {
                CliNodeFunction nodeFunction = (CliNodeFunction)(curNode);
                CliCommand curCmd = nodeFunction.getCliCommand();
                
                nodeFunction = (CliNodeFunction)(newNode);
                CliCommand newCmd = nodeFunction.getCliCommand();
                
                //System.out.println("curKeyword:"+curNode.getKeyword()+" newKeyword:" + newNode.getKeyword());
                
                String errMsg="";
                
                errMsg += "Error: UI Command duplicated with keylist <" + newCmd.getKeywords() + ">";
                errMsg += " with function callback <" + curCmd.getFunctionName() + "> and <" + newCmd.getFunctionName() + ">";

                //System.out.println(errMsg);
                errProc.addMessage(errMsg);
                return null;
            }
            
            if (curNode.getSyntaxKeyword().contentEquals(newNode.getSyntaxKeyword()))
                return curNode;
            
            if (curNode.getSyntaxKeyword().compareTo(newNode.getSyntaxKeyword())>0)
                break;
        }

        nodeList.getCliEntities().add(idx, newNode);
                
        return newNode;
    }

    
    private CliNodeList newCliNodeList() {
        
        CliNodeList nodeList = new CliNodeList(curCliNodeListID);
        curCliNodeListID++;
        
        cliCmdNodeLists.add(nodeList);
        
        return nodeList;
    }
    
    // prefix should be "uitop" or "uidiag"
    public boolean exportToCRuntime(String outFileName, String prefixName)
    {
        prefix = "ui"+prefixName;
        
        try {
            
            PrintWriter out =
                    new PrintWriter(new BufferedWriter(new FileWriter(outFileName)));
            
            try {
                // print file header
                
                out.println("\n");
                out.println("/*******************************************************************************");
                out.println("**                                                                            **");
                out.println("**                             ALCATEL LUCENT TELECOM                         **");
                out.println("**                                                                            **");
                out.println("*******************************************************************************/");
                out.println("\n\n#include \"ui.h\"\n\n");
                out.println("#if P2_"+prefixName.toUpperCase());
                
                /* print the node declaration, with format
                 *  extern uiParseNd uitopParseNode2;
                 */
                for (int i=0; i<curCliNodeListID; i++)
                    out.println("extern uiParseNd "+ prefix + "ParseNode" + i + ";");
                
                /* print the CliNodeFunction node for function and help declaration, e.g.
                 *      extern int UIlogout();
                 *      extern char *helpUIlogout;
                 */
                out.println("\n/* Function Declarations */\n");
                for (CliNodeList curNodeList: cliCmdNodeLists)
                {
                    for (CliNode curEntry : curNodeList.getCliEntities()) 
                    {
                        if (curEntry.isFunctionNode()) {
                            out.println("extern int " + curEntry.getSyntaxKeyword() + "();");
                            out.println("extern char *help" + curEntry.getSyntaxKeyword() + ";");
                        }
                    }               
                }               

                /* print the all detail help informatino for those from XML, .
                 *      extern int UIlogout();
                 *      char *helpUIlogout = "\n
                 *       ...
                 *       \n;
                 */
                out.println("\n\n\n/* Help Message Definition */\n");
                for (CliNodeList curNodeList: cliCmdNodeLists)
                {
                    for (CliNode curEntry : curNodeList.getCliEntities()) 
                    {
                        if (curEntry.isFunctionNode())
                        {
                            CliCommand cliCommand = ((CliNodeFunction)curEntry).getCliCommand();
                            if (!cliCommand.isXMLCommand())
                                continue;
                            String helpMsg = cliCommand.getRuntimeHelpMsg(true).replace("\"", "\\\"");
                            String[] lines = helpMsg.split("\n+");
                            out.println("char *help" + curEntry.getSyntaxKeyword() + "= \"\\n\\");
                            for (int i=0; i<lines.length; i++)
                                out.println(lines[i] + "\\n\\");
                            out.println("\";\n");
                        }
                    }               
                }   
                
                /* print the all node tables, e.g.
                    uiParseEnt      uitopParseEnt0[] = {
                    {&uitopParseNode24,&uitopParseNode0,UI_SHOW,UIT_STRING,"append",0,0,0,0},
                    {&uitopParseNode75,&uitopParseNode0,UI_SHOW,UIT_STRING,"view",0,0,0,0},
                    {0,0,UIT_NULL,0,0,0,0}
                    };
                    uiParseNd       uitopParseNode0 = {uitopParseEnt0 };                 *  
                 */
                out.println("\n\n\n/* The Node table */\n");
                for (CliNodeList curNodeList: cliCmdNodeLists)
                {
                    out.println(curNodeList.exportToCRuntimeFile(prefix));                  
                }
                
                out.println("#endif\n");
            
            } finally {
                out.close();
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        
        System.out.println("process file: " + outFileName + " done!");
        return true;
    }   
    
    /*
    public StringBuffer getSystemPrameter() {
        StringBuffer result = new StringBuffer();
        
        for (CliNodeList curNodeList: cliCmdNodeLists)
        {
            for (CliNode curEntry : curNodeList.getCliEntities()) 
            {
                if (curEntry.isFunctionNode()) {
                    CliNodeFunction nodeFunction = (CliNodeFunction)curEntry;
                    CliCommand cliCommand = nodeFunction.getCliCommand();
                    
                    if (!cliCommand.isSystemParameter())
                        continue;
                    
                    System.out.println(cliCommand.getSyntaxString());
                }
            }               
        }       
        
        return result;
    }
    */
}