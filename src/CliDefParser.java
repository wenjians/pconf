

import java.io.*;

public final class CliDefParser {
    
    public static final int CLI_TOKENS_MIN_COUNT  = 4; 
    public static final int CLI_TOKENS_MAX_COUNT  = 32;
    
    private CliCommand.CliMode cliCmdMode = CliCommand.CliMode.main;
    private boolean isScmCommand=false;
    
    private PConfError errProc = PConfError.getInstance();
    
    public void setSCMCommand(boolean isScm)        { isScmCommand = isScm; }
    
    
    public void setCliCmdMode(CliCommand.CliMode newCliCmdMode) {
        //assert CliCommand.isValidCmdMode(newCliCmdMode);
        cliCmdMode = newCliCmdMode;
    }
    
    public boolean parseDefFile(CliCommandTree cliCommandTree, String fileName)
    {
        boolean result = true;
        
        System.out.println("process file: " + fileName + "...");
        
        try {
            
            BufferedReader input = new BufferedReader(new FileReader(new File(fileName).getAbsoluteFile()));
            try {
                
                String cmdLine;
                while ((cmdLine = readOneCmdLine(input)) != null) 
                {
                    cmdLine = cmdLine.trim();
                    if (cmdLine.length() == 0)
                        continue;
                    
                    if (cmdLine.endsWith(";")) {
                        cmdLine = cmdLine.substring(0, cmdLine.length()-1);
                    }
                    
                    if (isCommentLine(cmdLine))
                        continue;
                    
                    CliCommand cliCommand = new CliCommand();
                    cliCommand.setCliCmdMode(cliCmdMode);
                    cliCommand.setSource(CliCommand.Source.def);
                    
                    if (!parseCmdLine(cmdLine, cliCommand)) {
                        result = false;
                    }
                        
                    if (cliCommand.isSCMCommand() && (!isScmCommand))
                        continue;
                    
                    if (!cliCommand.validate()) {
                        result = false;
                        continue;
                    }
                    
                    if (!cliCommandTree.addCommand(cliCommand)) {
                        result = false;
                        continue;
                    }
                        
                    //System.out.println("CliDefParser::parseDefFile, output:" + cliCommand);
                }
            } finally {
                input.close();
            }    
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        if (result)
            System.out.println("process file: " + fileName + " done!\n");
        else
            System.out.println("Error in process file: " + fileName + "!\n");
        
        return result;
    }
    
    
    private String readOneCmdLine(BufferedReader input)
    {
        try {
            int newLineNeed=0;
            String cmdLine = input.readLine();
            if (cmdLine == null)  return null;
        
            do {
                newLineNeed = 0;
                for (int i=0; i<cmdLine.length(); i++)
                {
                    if (cmdLine.charAt(i) == '\\')
                    {
                        newLineNeed = 1;
                        String newLine = input.readLine();
                        
                        if ( newLine == null)
                        {
                            printError("CliDefParser file terminated on continuation", cmdLine);
                            return null;
                        }

                        cmdLine = cmdLine.substring(0, i) + newLine;
                        break;
                    }
                }
    
            } while (newLineNeed == 1);
        
            return cmdLine;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private boolean isCommentLine(String line)
    {
        // one line at least have 2 chars "{}", or it is invalid
        if (line.length() <= 2)
            return true;
        
        //  the following is considered as comments start: "#" "//" and space
        //  the following is considered as version control "%"
        if (Character.isSpaceChar(line.charAt(0)) || line.charAt(0)=='#')
            return true;
        
        if (line.charAt(0) == '%')
            return true;
        
        if (line.startsWith("//"))
            return true;
        
        return false;
    }
    
    /* parse one command line
     * example of UI line in *.def file
     * {UIsysDumpMem:netman:diag:"dm":(Addr,<address>,0,0xffffffff):(Int,<count>,0,0xffffffff):[Ustr,<option>,0,2]}  
     * return:
     *      true : no error happens, continue next command line
     *      false: error happen, stop the parse and exist 
     */
    private boolean parseCmdLine(String cmdLine, CliCommand cliCommand)
    {
        if ((cmdLine.charAt(0) == 'S') || (cmdLine.charAt(0)=='s'))
        {
            cliCommand.setSCMCommand(true);
            cmdLine = cmdLine.substring(1);
        }

        String[] tokens = splitTokens(cmdLine);
        if (tokens == null)
            return false;

        cliCommand.setFunctionName(tokens[0]);
        
        if (!cliCommand.setPrivilege(tokens[1])) {
            return false;           
        }
            
        if (!cliCommand.setDisplayMode(tokens[2])) {
            return false;
        }
        
        //System.out.println("CliDefParser::parseCmdLine, function name:" + cliCommand.getFunctionName());

        int idx;
        String curKey = "";
        
        // start parse the keyword
        for (idx=3; (idx<tokens.length); idx++) {
            
            curKey = tokens[idx].trim();
            
            if (curKey.startsWith("\"") && curKey.endsWith("\""))
            {
                // remove start and end '"'
                curKey = curKey.substring(1, curKey.length()-1);
                
                CliNodeKeyword cliKeyword = new CliNodeKeyword();
                /*
                if (!cliKeyword.setSyntaxKeyword(curKey.trim()))
                {
                    printError("Error: keyword is too longer: " + curKey, cmdLine);
                    return false;
                }
                */
                
                cliKeyword.setNodeShow(cliCommand.getDisplayMode());
                
                cliCommand.addNode(cliKeyword);
            } 
            else if (curKey.startsWith("(") || curKey.startsWith("[")) 
            {
                CliNodeParameter cliParam = 
                        parseParam(cmdLine, curKey, cliCommand.getDisplayMode());
                
                if (cliParam==null) {
                    printError("invalid parameter in key: " + curKey, cmdLine);
                    return false;
                }
                
                cliCommand.addNode(cliParam);               
            }
            else 
            {
                printError("invalid token defined: " + curKey, cmdLine);
                return false;
            }
        }
        
        // start parse the parameters
        for (; idx<tokens.length; idx++)
        {
            //System.out.println("parse parameter: "+tokens[idx]);
            CliNodeParameter cliParam = parseParam(cmdLine, tokens[idx], cliCommand.getDisplayMode());
            if (cliParam==null)
                return false;
            
            //System.out.println("add parameter to command");
            cliCommand.addNode(cliParam);
        }
        
        // set the function node
        CliNodeFunction cliFun = new CliNodeFunction();
        cliFun.setSyntaxKeyword(cliCommand.getFunctionName());
        cliFun.setNodeShow(cliCommand.getDisplayMode());
        cliFun.setCliCommand(cliCommand);
        cliCommand.addNode(cliFun);
        
        //System.out.println(cliCommand);
        return true;
    }

    /* translate the following string from *.def to CliParameter
     *      [Int,<time-to-wait>,1,10]
     *      (Int,<count>,0,0xffffffff)
     *      (Case,{current|previous},0,0)
     */
    private CliNodeParameter parseParam(String cmdLine, String param, boolean cliShow)
    {
        CliNodeParameter cliParam = new CliNodeParameter();
                
        if (param.startsWith("(") && param.endsWith(")"))
            cliParam.setRequired("mandatory");
        else if (param.startsWith("[") && param.endsWith("]"))
            cliParam.setRequired("optional");
        else
        {
            printError("CliDefParser Invalid parameter definition: " + param, cmdLine);
            return null;                
        }
        
        // remove begin and end flag for "[]" or "()"
        param = param.substring(1,  param.length()-1);
        //System.out.println("parseParam, param: " + param);
        
        String[] tokens = param.split(",");
    
        // each parameter have 4 tokens
        if (tokens.length != 4) {   
            printError("CliDefParser Invalid parameter definition: " + param, cmdLine);
            return null;
        }
        
        if (!cliParam.setSyntaxKeyword(tokens[1]))
        {
            printError("CliDefParser Invalid parameter definition (keyword too long): " + param, cmdLine);
            return null;
        }
        cliParam.setNodeShow(cliShow);
        
        if (!cliParam.setDataType(tokens[0]))
            return null;

        if (!cliParam.setRange(tokens[2]+","+tokens[3]))
            return null;
        
        /*
        if (!cliParam.setMinValue(tokens[2]))
            return null;
        
        if (!cliParam.setMaxValue(tokens[3]))
            return null;
        */
        
        return cliParam;
    }
    
    private String[] splitTokens(String cmdLine) 
    {
        String [] tokens = cmdLine.split(":");
        
        for (int i=0; i<tokens.length; i++)
            tokens[i] = tokens[i].trim();
        
        if ((tokens.length < CLI_TOKENS_MIN_COUNT) || 
            (tokens.length > CLI_TOKENS_MAX_COUNT))
        {
            printError("Invalid UI definition", cmdLine);
            return null;
        }

        if (tokens[0].charAt(0) != '{')
        {
            printError("Illegal opening delimiter on line", cmdLine);
            return null;
        }
        else
        {
            tokens[0] = tokens[0].substring(1);
        }
        
        String lastToken = tokens[tokens.length-1];
        if (!lastToken.endsWith("}"))
        {
            printError("Illegal ending delimiter on line", cmdLine);
            return null;
        }
        else
        {
            tokens[tokens.length-1] = lastToken.substring(0, lastToken.length()-1);
        }
        
        /*
        System.out.println("CliDefParser::splitTokens: ");
        for (int i=0; i<tokens.length; i++)
            System.out.print(tokens[i] + " ");
        System.out.println();
        */
        
        return tokens;
    }
    
    private void printError(String description, String cmdLine) {
        errProc.addMessage("Error: " + description);
        errProc.addMessage("Error command line: " + cmdLine + "\n");
    }
}
