import java.util.List;
import java.util.ArrayList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
//import java.io.*;
//import cli.*;;

/**
 * 
 */


class PConfCommand {
    static final int MAX_COMMAND_PARAM = 2;
    
    String  command;
    int     paramCnt;
    List<String>  paramList;
    boolean paramSet;
    
    //PConfMain.CommandIdx cmdIdx;
    
    PConfCommand(/*PConfMain.CommandIdx idx, */String cmd, int paramCount) {
        //cmdIdx   = idx;
        command  = cmd;
        paramCnt = paramCount;
        paramList = new ArrayList<String> ();
        paramSet = false;
    }
    
    boolean isParamSet() { 
        return paramSet;  
    }

    
    void setParameter() {
        paramSet = true;
    }

    @Override
    public String toString() {
        String result = "";
        
        if (isParamSet()) {
            result += " " + command;
            
            for (int i=0; i<paramList.size(); i++) {
                result += " " + paramList.get(i);
            }
        }
        
        return result;
    }
} ;



/**
 * @author wenjians
 * @since 2014/5/30
 */
public class PConfMain {
    
    
    /* The following value should according to the command list type */
    public static final int COMMAND_YANG_DIR = 0;
    public static final int COMMAND_YIN_DIR  = 1;
    public static final int COMMAND_PYANG    = 2;
    public static final int COMMAND_PDD      = 3;
    public static final int COMMAND_CLIXML   = 4;
    public static final int COMMAND_CLITREE  = 5;
    public static final int COMMAND_CLIEXPORT= 6;
    public static final int COMMAND_BOARD    = 7;
    
    private static PConfCommand[] commandList = {
        //type,  range need, default rule, minimal rule, maximum rule
        new PConfCommand("-yang",   1),
        new PConfCommand("-yin",    1),
        new PConfCommand("-pyang",  0),
        new PConfCommand("-pdd",    1),
        new PConfCommand("-clixml", 1),
        new PConfCommand("-clitree", 1),
        new PConfCommand("-cliexport", 1),
        new PConfCommand("-board", 1),
        
    };
    
    static PConfError errProc = PConfError.getInstance();;
    static YinParser  yinParser  = new YinParser();
    static YangParser yangParser  = new YangParser();
    static YangFileTree yangTree = new YangFileTree();
    static ConfigTree configTree = new ConfigTree();
    
    static CliDefParser defParser = new CliDefParser();
    static CliXmlParser xmlParser = new CliXmlParser(); 
    
    static CliCommandTree cliMainCmdTree = new CliCommandTree();
    static CliCommandTree cliDiagCmdTree = new CliCommandTree();
    static CliCommandTree cliTdmCmdTree = new CliCommandTree();
    

    static PConfCommand getCommand(String cmd) {
        for (int i=0; i<commandList.length; i++) {
            if (cmd.contentEquals(commandList[i].command))
                return commandList[i];
        }
        
        return null;
    }
    


    /** pConfMain, it is used to generate XML files from Yang file list
    *
    *  @author  Sun Wenjian
    *  @version 1.0
    */
    public static void main(String[] args) 
    {
        if (args.length < 1) {
            printHelpMsg();
            return;
        }
        
        int argc = 0;
        PConfCommand command = null;
                
        while (argc < args.length)
        {
            command = getCommand(args[argc]);
            if (command == null) {
                errProc.addMessage(String.format("unknown command <%s>\n", args[argc]));
                break;
            }
            
            if (argc+command.paramCnt > args.length) {
                errProc.addMessage("Parameter count error\n");
                break;
            }
            
            if (command.paramCnt == 0) {
            	command.setParameter();
            } else {
            	for (int i=0; i<command.paramCnt; i++) {
            		argc ++;
            		command.paramList.add(args[argc]);
            		command.setParameter();
            	}
            }
            argc++;
        }
        errProc.checkError();

        if ((commandList[COMMAND_YANG_DIR].paramList.size() == 0) || 
            (commandList[COMMAND_YIN_DIR].paramList.size() == 0))
        {
            errProc.addMessage("Both Yang input and output file must specified!\n");
        }


        // Print the parameters
        for (int idx=0; idx<commandList.length; idx++) {
            if (!commandList[idx].isParamSet())
                continue;
            
            System.out.println("param idx:" + idx + commandList[idx]);
        }
        
        
        parseYang2Yin();
        
        parseYinFiles();
        
        exportConfigNode();
        
        /*
        if (commandList[COMMAND_CLIXML].isParamSet() && commandList[COMMAND_BOARD].isParamSet()) {
            parseCliXml(commandList[COMMAND_BOARD].paramList.get(0),
                        commandList[COMMAND_CLIXML].paramList.get(0));
            errProc.checkError();
        }
        
        if (commandList[COMMAND_CLITREE].isParamSet() && commandList[COMMAND_BOARD].isParamSet()) {
            exportCliTree(commandList[COMMAND_BOARD].paramList.get(0),
                          commandList[COMMAND_CLITREE].paramList.get(0));
            errProc.checkError();
        }
        
        
        
        if (commandList[COMMAND_CLIEXPORT].isParamSet() && commandList[COMMAND_BOARD].isParamSet()) {
            exportCli(commandList[COMMAND_BOARD].paramList.get(0),
                      commandList[COMMAND_CLIEXPORT].paramList.get(0));
            errProc.checkError();
        }
        
        errProc.checkError();
        */
    }


    static void parseYang2Yin() {
        
        boolean result = true;
        
        if (commandList[COMMAND_PYANG].isParamSet()) {
            yangParser.enabletYang2Yin();
        }
        
        yangParser.setYangFileTree(yangTree);
        
        /* add two default extension to yang File Tree */
        YangFileModule extCOM = new YangFileModule();
        extCOM.setModuleGroup(YangFileModule.ModuleGroup.extension);
        extCOM.setYangFileName("ALUYangExtensions.yang");
        yangTree.addYangModule(extCOM);
        
        YangFileModule fileMGW = new YangFileModule();
        fileMGW.setModuleGroup(YangFileModule.ModuleGroup.extension);
        fileMGW.setYangFileName("MGWYangExtensions.yang");
        yangTree.addYangModule(fileMGW);

        String yangPath = commandList[COMMAND_YANG_DIR].paramList.get(0);
        String yinPath  = commandList[COMMAND_YIN_DIR].paramList.get(0);
        result = yangParser.parseYangFiles(yangPath, yinPath);

        if ((!result) || (!yangTree.validation()))  {
            errProc.addMessage("process the yang failure!");
        }
        
        errProc.checkError();
    }
    
    
    static void parseYinFiles() {
        System.out.println("\nNow parser Yin to Configuration Tree ... ");

        String yinPath  = commandList[COMMAND_YIN_DIR].paramList.get(0);
        System.out.println("parseYinFiles yin path:" + yinPath);

        /* add the built-in type to Configure Tree */
        ConfigBuiltin.Init();
        for (String type: ConfigBuiltin.getYangBuiltinTypes()) {
            configTree.addTypedef("", type, new ConfigBuiltin(type));
        }

        yinParser.setConfigTree(configTree);
        yinParser.setYinFileDirectory(yinPath);
        yinParser.setYangFileTree(yangTree);        
        yinParser.parseYangFileTree();
        
        errProc.checkError();        
    }

    private static void exportConfigNode() {
        String pddFile = commandList[COMMAND_PDD].paramList.get(0);
        if (pddFile.length() != 0) {
            System.out.print("now export PDD document ...");
            
            ConfigExportPDD exportPdd = new ConfigExportPDD();
            
            exportPdd.export(pddFile, configTree);
            
            System.out.println(" done!");
        }
    }
    
    private static boolean parseCliXml(String boardType, String xmlFileName) {
        System.out.println("parseCliXml: process UI for board:" + boardType + ", ui XML file:" + xmlFileName);

        boolean isScmCmd = false;
        if (boardType.contains("scm"))
            isScmCmd = true;
    
        xmlParser.setConfigTree(configTree);
        xmlParser.setSCMCommand(isScmCmd);
        if (!xmlParser.parseXMLFile(xmlFileName, cliMainCmdTree, cliDiagCmdTree))
            return false;
        
        return true;
    }

    
    private static boolean exportCliTree(String boardType, String uiDefPath) {
        
        System.out.println("exportCliTree board:" + boardType + ", ui definition path:" + uiDefPath);

    
        // produce the main and diag command tree in C file
        String outputFileName;
        outputFileName = uiDefPath + "/" + "uitrtdm_" +boardType+".c";
        cliTdmCmdTree.exportToCRuntime(outputFileName, "");
        
        outputFileName = uiDefPath + "/" + "uitrtop_" +boardType+".c";
        cliMainCmdTree.exportToCRuntime(outputFileName, "top");
        
        outputFileName = uiDefPath + "/" + "uitrdiag_" +boardType+".c";
        cliDiagCmdTree.exportToCRuntime(outputFileName, "diag");
        
        return true;
    }
    
    
    private static boolean exportCli(String boardType, String outputPath) {
        /* TODO
        CliKeywordLenCheck cliKeyword = new CliKeywordLenCheck();
        if (!cliKeyword.export(cliMainCmdTree, cliDiagCmdTree))
            return false;
        */
        /* TODO
        CliXmlExport cliXml = new CliXmlExport();
        String outputFileName = outputPath+"/ui_" + boardType + "_all.xml";
        cliXml.export(outputFileName, cliMainCmdTree, cliDiagCmdTree);
        */
        
        /* the following is check the UI privilege */
        StringBuffer privilegeCheckResult = new StringBuffer();
        String defPrivilegeRuleFile = "rules/cli_privilege_rules.xml";
 
        ClikPrivilegeCheck cliCheck = new ClikPrivilegeCheck();
        cliCheck.setBordType(boardType);
        cliCheck.setRuleFileName(defPrivilegeRuleFile);
        privilegeCheckResult.append(cliCheck.export(cliMainCmdTree));
        if (privilegeCheckResult.length() > 0)
        {
            privilegeCheckResult.insert(0, cliCheck.getTitle());
            errProc.addMessage("The privilege of following UIs is not follow expected:");
            errProc.addMessage(privilegeCheckResult.toString());
            errProc.addMessage("you can do following actions:");
            errProc.addMessage("1: change the privilege of UI definition in *.def or *.xml");
            errProc.addMessage("2: add exception in file <" + defPrivilegeRuleFile + ">\n");
            return false;
        }
        System.out.println("check main command privilege finished!");

        /* the following will export the UI list */
        CliListExport cliUIList = new CliListExport();
        cliUIList.setBordType(boardType);
        
        StringBuffer uiListResult = new StringBuffer();
        String cliListOutputFileName = outputPath+"/../ui_list.csv";
        
        uiListResult.append(cliUIList.filterBoard(cliListOutputFileName));
        uiListResult.append(cliUIList.export(cliMainCmdTree, cliDiagCmdTree));
        //checkResult.append(existFilterResult);
        if (uiListResult.length() > 0)
            uiListResult.insert(0, cliUIList.getTitle());

        writeFile(cliListOutputFileName, uiListResult);
        System.out.println("export CLI command list to file <" + cliListOutputFileName + "> done!");
        
        /* the following is get the HTML help */
        CliHtmlHelp htmlHelp = new CliHtmlHelp();
        StringBuffer htmlMsg = htmlHelp.export(cliMainCmdTree, cliDiagCmdTree);
        String outputFileName = outputPath+"/ui_" + boardType + "_help.html";
        writeFile(outputFileName, htmlMsg);
        
        return true;
    }

    
    private static void writeFile(String outFileName, StringBuffer buffer)
    {
        /* if the size is zero, means remove the file */
        if (buffer.length() == 0) {
        
            File file = new File(outFileName);
            if (file.exists())
            {
                //System.out.println("remove file: " + outFileName + "\n");
                file.delete();
            }
            return;
        }
        
        try {
            PrintWriter out =
                    new PrintWriter(new BufferedWriter(new FileWriter(outFileName)));
            try {
                out.println(buffer);
                System.out.println("write file <" + outFileName + "> successfully!");
            } finally {
                out.close();
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }     
    
    private static void printHelpMsg() {
        String helpMsg = "";
        
        helpMsg += "The following is the help information:\n";
        helpMsg += "Usage example:";

        System.out.print(helpMsg);
    }
}
