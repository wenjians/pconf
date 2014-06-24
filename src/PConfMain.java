

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
        //testShell();
        //return;
        
        PConfError errProc = PConfError.getInstance();
        
        
        //System.out.println(commandList.length);
        
        if (args.length < 1) {
            printHelpMsg();
            //parseTest();
            return;
        }
        
        int argc = 0;
        PConfCommand command = null;
        //System.out.println("total argument count:" + args.length);
                
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
            
            System.out.format("%d: %7s %5d %s\n", 
                              idx, 
                              commandList[idx].command, 
                              commandList[idx].paramCnt, 
                              commandList[idx].paramStr);
        }
        
        
        String yangPath = commandList[COMMAND_YANG_DIR].paramStr;
        String yinPath  = commandList[COMMAND_YIN_DIR].paramStr;
        
        if ((yangPath.length() == 0) || (yinPath.length() == 0))
        {
            errProc.addMessage("Both Yang input and output file must specified!\n");
            errProc.checkError();
        }
        

        boolean result = true;
        YangParser   yangPaser  = new YangParser();
        YangFileTree yangTree   = new YangFileTree();
                
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
            errProc.checkError();
        }
        
        //System.out.println("\n\n\nYang2Yin paser finished, Yang file tree:\n" + yangTree + "\n\n\n");
        
        System.out.println("\n\n\nNow Yin paser to Configuration Tree ... \n\n\n");
        
        
        ConfigTree configTree = new ConfigTree();
        
        /* add the built-in type to Configure Tree */
        for (String type: ConfndBuiltin.getBuiltinTypes()) {
        	configTree.addTypeDef("", type, new ConfndBuiltin(type));
        }

        
        YinParser  yinParser  = new YinParser();
        yinParser.setConfigTree(configTree);
        yinParser.setYinFileDirectory(yinPath);
        yinParser.setYangFileTree(yangTree);
        
        result = yinParser.parseYangFileTree();
        
        if (!result)  {
            errProc.addMessage("error process yin files!");
        }
        
        errProc.checkError();
        
        /*
        System.out.println("Yin Parser finished, now begining to print the ConfigTree\n");
        System.out.println("the following are the data type table definition:\n" 
                         + configTree.toStringTypedef());
        System.out.print("\n\n");
        System.out.println("the following are the Config Parameter table:\n" 
                         + configTree.toStringAllConfigModule());
        */
        
        
        
        String pddFile = commandList[COMMAND_PDD].paramStr;
        if (pddFile.length() != 0) {
            System.out.print("now export PDD document ...");
            
            ConfigExportPDD exportPdd = new ConfigExportPDD();
            
            exportPdd.export(pddFile, configTree);
            
            System.out.println(" done!");
        }
        
                
    }

    
    private static void printHelpMsg() {
        String helpMsg = "";
        
        helpMsg += "The following is the help information:\n";

        System.out.print(helpMsg);
    }
}
