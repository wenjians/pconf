import java.util.List;
import java.util.ArrayList;


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
    
    private static PConfCommand[] commandList = {
        //type,  range need, default rule, minimal rule, maximum rule
        new PConfCommand("-yang",   1),
        new PConfCommand("-yin",    1),
        new PConfCommand("-pyang",  0),
        new PConfCommand("-pdd",    1),
        new PConfCommand("-clixml", 2),
        new PConfCommand("-clitree",2),
        
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
            
            for (int i=0; i<command.paramCnt; i++) {
                argc ++;
                command.paramList.add(args[argc]);
                command.setParameter();
            }
            argc++;
        }

        if ((commandList[COMMAND_YANG_DIR].paramList.size() == 0) || 
                (commandList[COMMAND_YIN_DIR].paramList.size() == 0))
        {
            errProc.addMessage("Both Yang input and output file must specified!\n");
        }

        errProc.checkError(); 

        // Print the parameters
        for (int idx=0; idx<commandList.length; idx++) {
            if (!commandList[idx].isParamSet())
                continue;
            
            System.out.println("param idx:" + idx + commandList[idx]);
        }
        
        parseYang2Yin();
        
        parseYinFiles();
        
        //if (commandl)
        if (commandList[COMMAND_CLIXML].isParamSet()) {
            parseCliXml(commandList[COMMAND_CLIXML].paramList.get(0), 
                        commandList[COMMAND_CLIXML].paramList.get(1));
            errProc.checkError();
        }
        
        if (commandList[COMMAND_CLITREE].isParamSet()) {
            exportCliTree(commandList[COMMAND_CLITREE].paramList.get(0), 
                          commandList[COMMAND_CLITREE].paramList.get(1));
            errProc.checkError();
        }
        
        exportConfig();
        
        // TODO dumy for compiler, need change later
        parseCli("", "");
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

    private static void exportConfig() {
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
        
        /*
        boolean isScmCmd = false;        
        if (boardType.contains("scm"))
            isScmCmd = true;
        */
    
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
    
    
    private static boolean parseCli(String boardType, String uiDefPath) {
        /*
        boolean isScmCmd = false;
        String  inputFileName;
        String  outputFileName;
        
        System.out.println("process UI for board:" + boardType + ", ui definition path:" + uiDefPath);
        
        if (boardType.contains("scm"))
            isScmCmd = true;
        */
        
        //CliCommandTreeBoard cmdTreeBoard = new CliCommandTreeBoard();
        


        /*
        CliDefParser defParser = new CliDefParser();
        defParser.setSCMCommand(isScmCmd);
        
        // process the tdm command from uitrtdm_scm.def, here scm is board type, can be cim, mcm, ...
        // actually, it is not used, here just for history compatible
        inputFileName = "uitrtdm_"+boardType+".def";
        defParser.setCliCmdMode(CliCommand.CliMode.main);
        if (!defParser.parseDefFile(cliTdmCmdTree, inputFileName))
            return false;
        
        // process the main command from uitrtop_scm.def, here scm is board type, can be cim, mcm, ...
        inputFileName = "uitrtop_"+boardType+".def";
        defParser.setCliCmdMode(CliCommand.CliMode.main);
        if (!defParser.parseDefFile(cliMainCmdTree, inputFileName))
            return false;
        
        // process the main command from uitrdiag_scm.def, here scm is board type, can be cim, mcm, ...
        inputFileName = "uitrdiag_"+boardType+".def";
        defParser.setCliCmdMode(CliCommand.CliMode.diag);
        if (!defParser.parseDefFile(cliDiagCmdTree, inputFileName))
            return false;
        */
        
        
        /*
        // process the xml to command tree  ui_scm.xml, here scm is board type, can be cim, mcm, ...
        xmlParser.setSCMCommand(isScmCmd);
        inputFileName = "ui_top_"+boardType+".xml";
        if (!xmlParser.parseXMLFile(inputFileName, cliMainCmdTree, cliDiagCmdTree))
            return false;
        */
        
        
        /*
        CliCheckKeywordLen cliKeyword = new CliCheckKeywordLen();
        if (!cliKeyword.export(cliMainCmdTree, cliDiagCmdTree))
            return false;
        */
        
        /*
        // produce the main and diag command tree in C file
        outputFileName = "uitrtdm_" +boardType+".c";
        cliTdmCmdTree.exportToCRuntime(outputFileName, "");
        
        outputFileName = "uitrtop_" +boardType+".c";
        cliMainCmdTree.exportToCRuntime(outputFileName, "top");
        
        outputFileName = "uitrdiag_" +boardType+".c";
        cliDiagCmdTree.exportToCRuntime(outputFileName, "diag");
        */
        
        
        //CliExport cliExport = new CliExport();
        /*
        CliExportXML cliXml = new CliExportXML();
        outputFileName = imagePath+"/ui_" + boardType + "_all.xml";
        cliXml.export(outputFileName, cliMainCmdTree, cliDiagCmdTree);
        */
        
        /* the following is check the UI privilege */
        /*
        CliCheckPrivilege cliCheck = new CliCheckPrivilege();
        cliCheck.setBordType(boardType);
        cliCheck.setRuleFileName(defPrivilegeRuleFile);
        */
        
        /* check the privilege error and print during compiler */
        /*
        StringBuffer privilegeCheckResult = new StringBuffer();
        privilegeCheckResult.append(cliCheck.export(cliMainCmdTree));
        if (privilegeCheckResult.length() > 0)
        {
            privilegeCheckResult.insert(0, cliCheck.getTitle());
            System.out.println("The privilege of following UIs is not follow expected:\n");
            System.out.println(privilegeCheckResult);
            System.out.println("you can do following actions:");
            System.out.println("1: change the privilege of UI definition in *.def or *.xml");
            System.out.println("2: add exception in file <" + defPrivilegeRuleFile + ">\n");
            return false;
        }
        System.out.println("check main command privilege finished!");
        */

        /*
        StringBuffer checkResult = new StringBuffer();
        outputFileName = imagePath+"/../ui_privilege_check.csv";
        
        checkResult.append(cliCheck.filterBoard(outputFileName));
        checkResult.append(cliCheck.export(cliMainCmdTree));
        //checkResult.append(existFilterResult);
        if (checkResult.length() > 0)
            checkResult.insert(0, cliCheck.getTitle());

        // if size is zero, then will remove the file
        //System.out.println("file length:" + checkResult.length() + "\n");
        //System.out.println("String: <" + checkResult + ">\n");
        writeFile(outputFileName, checkResult);

        System.out.println("check main command privilege, create file <" + outputFileName + "> done!");
        */
        
        
        /* the following will export the UI list */
        /*
        CliExportUIList cliUIList = new CliExportUIList();
        cliUIList.setBordType(boardType);
        
        StringBuffer uiListResult = new StringBuffer();
        String cliListOutputFileName = imagePath+"/../ui_list.csv";
        
        uiListResult.append(cliUIList.filterBoard(cliListOutputFileName));
        uiListResult.append(cliUIList.export(cliMainCmdTree, cliDiagCmdTree));
        //checkResult.append(existFilterResult);
        if (uiListResult.length() > 0)
            uiListResult.insert(0, cliUIList.getTitle());

        writeFile(cliListOutputFileName, uiListResult);
        System.out.println("export CLI command list to file <" + cliListOutputFileName + "> done!");
        */
        
        /* the following is get the HTML help */
        /*
        CliExportHTMLHelp htmlHelp = new CliExportHTMLHelp();
        StringBuffer htmlMsg = htmlHelp.export(cliMainCmdTree, cliDiagCmdTree);
        outputFileName = imagePath+"/ui_" + boardType + "_help.html";
        writeFile(outputFileName, htmlMsg);
        
        if (isScmCmd) {
            CliExportParameter exportParam = new CliExportParameter();
            outputFileName = imagePath+"/ui_" + boardType + "_sysparam.xls";
            //System.out.println("create system parameter file <" + outputFileName + "> ...");
            exportParam.export(outputFileName, cliMainCmdTree);
            System.out.println("create system parameter file <" + outputFileName + "> done!");
        }
        */
        
        return true;
    }

    
    private static void printHelpMsg() {
        String helpMsg = "";
        
        helpMsg += "The following is the help information:\n";

        System.out.print(helpMsg);
    }
}
