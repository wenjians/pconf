package cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

//import cli.CliExport.Sequence;

public class CliListExport extends CliExport {

    private String boardTypeName;
    private StringBuffer exportResult;
    
    public CliListExport() {
        super();
        
        boardTypeName = "";
        exportResult = null;
    }

    public String getTitle() {
        return "board,source,mode,privilege,syntax string\n";
    }
    
    public void setBordType(String board) {
        boardTypeName = board;
    }
    
    
    void exportCliCommand(CliCommand cliCommand) 
    {

        // source, board, mode, privilege, syntax string
        exportResult.append(String.format("%s, %s, %s, %s, %s\n", 
                                          boardTypeName,
                                          cliCommand.getSource().toString(),
                                          cliCommand.getCliCmdMode().toString(),
                                          cliCommand.getPrivilegeName(),
                                          cliCommand.getSyntaxString()));
    }
    
    
    
    public StringBuffer export(CliCommandTree cmdMainTree, CliCommandTree cmdDiagTree) 
    {
        exportResult= new StringBuffer();
        
        super.setSequence(Sequence.lexical);
        
        super.walkthrough(cmdMainTree);
        super.walkthrough(cmdDiagTree);
        
        return exportResult;
    }   
    
    
    public StringBuffer filterBoard(String fileName){
        
        StringBuffer filterResult= new StringBuffer();
        
        String strTitle = "^board,.*$";
        String strBoard = "^" + boardTypeName + ",.*$";
        
        java.util.regex.Pattern patternTitle = java.util.regex.Pattern.compile(strTitle);
        java.util.regex.Pattern patternBoard = java.util.regex.Pattern.compile(strBoard);
        
        File file = new File(fileName);
        if (!file.exists())
            return filterResult;
        
        //System.out.println("pattern string: title<" + strTitle + "> board<" + strBoard + ">\n");
        
        BufferedReader reader = null;  
        try {  
            reader = new BufferedReader(new FileReader(file));  
            String tempString = null;  

            while ((tempString = reader.readLine()) != null){
                if (tempString.length() == 0)
                    continue;
                
                //System.out.println(tempString);
                
                java.util.regex.Matcher matcherTitle = patternTitle.matcher(tempString);
                java.util.regex.Matcher matcherBoard = patternBoard.matcher(tempString);
                
                /*
                System.out.println("match result: title(" + matcherTitle.matches() + 
                                   "), board(" + matcherBoard.matches() + ")\n");
                */
                
                if (!matcherTitle.matches() && !matcherBoard.matches())
                    filterResult.append(tempString + "\n");
            }
            
            reader.close();
            
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (reader != null){  
                try {  
                    reader.close();  
                } catch (IOException e1) {  
                }  
            }  
        }
        
        return filterResult;
    }
    
}