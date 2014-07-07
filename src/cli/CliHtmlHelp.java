package cli;

import cli.CliExport.Sequence;

public class CliHtmlHelp extends CliExport{
    
    private enum HtmlSection  { cmdList, cmdDetail  }
    
    public StringBuffer exportResult;
    public HtmlSection  section;
    
    void setHtmlSection(HtmlSection aHtmlSection) {
        section = aHtmlSection;
    }
    
    void exportCliCommand(CliCommand cliCommand) 
    {
        
        if (!cliCommand.isXMLCommand())
            return;
        
        if (section == HtmlSection.cmdList)
        {
            exportResult.append("<li><a href=\"#" + transferHTML(cliCommand.getSyntaxString())+"\">");
            exportResult.append(transferHTML(cliCommand.getSyntaxString())+"</a></li>");
            exportResult.append("\n");
        }
        else if (section == HtmlSection.cmdDetail) 
        {
            exportResult.append("<h4><a name=\"" + transferHTML(cliCommand.getSyntaxString()) + "\">");
            exportResult.append(transferHTML(cliCommand.getSyntaxString())+"</a></h4>");
            
            //exportResult.append("\n<code><pre>\n");
            exportResult.append("\n<pre style=\"padding:8.5px; font-family:Menlo,Monaco,'Courier New',monospace; color:rgb(51,51,51); margin-top:0px; margin-bottom:9px; line-height:18px; background-color:rgb(245,245,245); white-space:pre-wrap; word-break:break-all; word-wrap:break-word\">");
            exportResult.append("\n<code style=\"padding:0px; font-family:Menlo,Monaco,'Courier New',monospace; color:inherit; background-color:transparent; border:0px\">");
            
            exportResult.append(transferHTML(cliCommand.getRuntimeHelpMsg(true)));
            exportResult.append("</code></pre><hr>\n\n");
        }
    }
    
    public StringBuffer export(CliCommandTree mainCmdTree, CliCommandTree diagCmdTree) {
        
        exportResult= new StringBuffer();

        super.setSequence(Sequence.lexical);
        setHtmlSection(HtmlSection.cmdList);
        
        exportResult.append("<br>The following are main UI command<hr>\n");
        
        exportResult.append("<ul>\n");
        super.walkthrough(mainCmdTree);
        exportResult.append("</ul>\n");
        
        exportResult.append("<br><br>The following are diag UI command<hr>\n");
        exportResult.append("<ul>\n");
        super.walkthrough(diagCmdTree);
        exportResult.append("</ul>\n");
        
        exportResult.append("<br><hr><br>\n\n\n");
        
        setHtmlSection(HtmlSection.cmdDetail);
        super.walkthrough(mainCmdTree);
        super.walkthrough(diagCmdTree);
        
        if (exportResult.length()>0) {
            exportResult.insert(0, "<html><body>\n\n");
            exportResult.append("\n\n</body></html>");
        }
        return exportResult;
        
        
    }
}