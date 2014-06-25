

//import java.io.*;
import cli.*;;

/**
 * 
 */


class PConfCommand {
    String  command;
    int     paramCnt;
    String  paramStr;
    boolean paramSet;
    
    //PConfMain.CommandIdx cmdIdx;
    
    PConfCommand(/*PConfMain.CommandIdx idx, */String cmd, int paramCount, String paramString) {
        //cmdIdx   = idx;
        command  = cmd;
        paramCnt = paramCount;
        paramStr = paramString;
        paramSet = false;
    }
    
    boolean isParamSet()    { return paramSet;  }

    void setParameter(String param) {
        paramStr = param;
        paramSet = true;
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
    
    private static PConfCommand[] commandList = {
        //type,  range need, default rule, minimal rule, maximum rule
        new PConfCommand("-yang",   1,    ""),
        new PConfCommand("-yin",    1,    ""),
        new PConfCommand("-pyang",  0,    ""),
        new PConfCommand("-pdd",    1,    ""),
    };
    
    static PConfError errProc;
    static ConfigTree configTree;
    static YangParser yangPaser;
    static YangFileTree yangTree;
    static YinParser  yinParser;
    
    /*
    static CliCommandTree cliMainCmdTree = new CliCommandTree();
    static CliCommandTree cliDiagCmdTree = new CliCommandTree();
    static CliCommandTree cliTdmCmdTree = new CliCommandTree();
    
    CliDefParser defParser = new CliDefParser();
    */
    
    
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
        errProc = PConfError.getInstance();
        
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
            
            if (argc+command.paramCnt>args.length) {
                errProc.addMessage("Parameter count error\n");
                break;
            }
            
            if (command.paramCnt == 1) {
                argc ++;
                command.setParameter(args[argc]);
            } else {
                command.setParameter("");
            }
            
            argc++;
        }
        
        errProc.checkError(); 
        
        for (int idx=0; idx<commandList.length; idx++) {
            if (!commandList[idx].isParamSet())
                continue;
            
            System.out.format("%d: %7s %5d %s\n", idx,  commandList[idx].command, 
                              commandList[idx].paramCnt,commandList[idx].paramStr);
        }
        
        if ((commandList[COMMAND_YANG_DIR].paramStr.length() == 0) || 
            (commandList[COMMAND_YIN_DIR].paramStr.length() == 0))
        {
            errProc.addMessage("Both Yang input and output file must specified!\n");
            errProc.checkError();
        }

        yangPaser  = new YangParser();
        yangTree   = new YangFileTree();
        yinParser  = new YinParser();
        
        parseYang2Yin();
        
        parseYinFiles();
        
        processCRuntime("scm", )
        

        String pddFile = commandList[COMMAND_PDD].paramStr;
        if (pddFile.length() != 0) {
            System.out.print("now export PDD document ...");
            
            ConfigExportPDD exportPdd = new ConfigExportPDD();
            
            exportPdd.export(pddFile, configTree);
            
            System.out.println(" done!");
        }
    }

    static void parseYang2Yin() {
        String yangPath = commandList[COMMAND_YANG_DIR].paramStr;
        String yinPath  = commandList[COMMAND_YIN_DIR].paramStr;
        
        boolean result = true;

                
        if (commandList[COMMAND_PYANG].isParamSet())
            yangPaser.enabletYang2Yin();
        yangPaser.setYangFileTree(yangTree);
        
        /* add two default extension to yang File Tree */
        YangFileModule extCOM = new YangFileModule();
        extCOM.setModuleGroup(YangFileModule.ModuleGroup.extension);
        extCOM.setYangFileName("ALUYangExtensions.yang");
        yangTree.addYangModule(extCOM);
        
        YangFileModule fileMGW = new YangFileModule();
        fileMGW.setModuleGroup(YangFileModule.ModuleGroup.extension);
        fileMGW.setYangFileName("MGWYangExtensions.yang");
        yangTree.addYangModule(fileMGW);
        
        result = yangPaser.parseYangFiles(yangPath, yinPath);

        if ((!result) || (!yangTree.validation()))  {
            errProc.addMessage("process the yang failure!");
        }
        
        errProc.checkError();
    }
    
    
    static void parseYinFiles() {
        System.out.println("\n\n\nNow Yin paser to Configuration Tree ... \n\n\n");

        String yinPath  = commandList[COMMAND_YIN_DIR].paramStr;

        configTree = new ConfigTree();
        
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
    
    
    private static boolean processCRuntime(String boardType, String uiDefPath) {
        boolean isScmCmd = false;
        String  inputFileName;
        String  outputFileName;
        
        System.out.println("process UI for board:" + boardType + ", ui definition path:" + uiDefPath);
        
        if (boardType.contains("scm"))
            isScmCmd = true;
    
        CliCommandTree cliMainCmdTree = new CliCommandTree();
        CliCommandTree cliDiagCmdTree = new CliCommandTree();
        CliCommandTree cliTdmCmdTree = new CliCommandTree();

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
        
        // process the xml to command tree  ui_scm.xml, here scm is board type, can be cim, mcm, ...
        CliXmlParser xmlParser = new CliXmlParser();
        xmlParser.setSCMCommand(isScmCmd);
        
        inputFileName = "ui_top_"+boardType+".xml";
        if (!xmlParser.parseXMLFile(inputFileName, cliMainCmdTree, cliDiagCmdTree))
            return false;
        
        /*
        CliCheckKeywordLen cliKeyword = new CliCheckKeywordLen();
        if (!cliKeyword.export(cliMainCmdTree, cliDiagCmdTree))
            return false;
        */
        
        // produce the main and diag command tree in C file
        outputFileName = "uitrtdm_" +boardType+".c";
        cliTdmCmdTree.exportToCRuntime(outputFileName, "");
        
        outputFileName = "uitrtop_" +boardType+".c";
        cliMainCmdTree.exportToCRuntime(outputFileName, "top");
        
        outputFileName = "uitrdiag_" +boardType+".c";
        cliDiagCmdTree.exportToCRuntime(outputFileName, "diag");
        
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
