package cli;
import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import org.xml.sax.*; 

public class CliExport {

	Sequence walkSequence =Sequence.definition;
	
	public enum Sequence  { lexical, definition  }

	
	
	CliExport() {
	}
	
	void setSequence(Sequence seq) { 
	    walkSequence = seq;		
	}
	
	
    void exportCliCommand(CliCommand cliCommand) {
        
    }

    String transferHTML(String str) {
        str = str.replace("&", "&amp;");
        str = str.replace("<", "&lt;");
        str = str.replace(">", "&gt;");
        str = str.replace("\"", "&quot;");
        str = str.replace(" ", "&#32;");
        //str = str.replace(" ", "&nbsp;");
        //str = str.replace("\n", "<br>");
        
        return str;
    }
    
    private void walkthrough_definition(CliCommandTree cmdTree) 
    {
        for (CliNodeList curNodeList: cmdTree.getAllCmdNodeList())
        {
            for (CliNode curEntry : curNodeList.getCliEntities()) 
            {
                if (!curEntry.isFunctionNode())
                    continue;
                
                exportCliCommand(((CliNodeFunction)curEntry).getCliCommand());
            }               
        }
    }     


    private void walkthrough_lexical(CliCommandTree cmdTree, CliNodeList curNodeList) 
    {
        for (CliNode curEntry : curNodeList.getCliEntities()) 
        {
            if (curEntry.isFunctionNode()) 
            {
            	exportCliCommand(((CliNodeFunction)curEntry).getCliCommand());
            }
            
            if (!cmdTree.isRootNodeList(curEntry.getChilds()))
            	walkthrough_lexical(cmdTree, curEntry.getChilds());
        }
    }    

    
    public void walkthrough(CliCommandTree cmdTree) 
    {
    	if (walkSequence == Sequence.definition)
    		walkthrough_definition(cmdTree);
    	else
    		walkthrough_lexical(cmdTree, cmdTree.getRootEntry());
    }
}
