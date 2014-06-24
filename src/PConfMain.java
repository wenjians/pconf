

//import java.io.*;


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
    static YangParser   yangPaser;
    static YangFileTree yangTree;
    static YinParser  yinParser;
    
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
    
    
    private static void printHelpMsg() {
        String helpMsg = "";
        
        helpMsg += "The following is the help information:\n";

        System.out.print(helpMsg);
    }
}
