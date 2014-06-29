import java.io.*;
import java.util.*;
//import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*; 
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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








class CliCheckPrivilege extends CliExport {
    
    class PrivilegeRule {
        String matchString;
        int    privilege;
        java.util.regex.Pattern pattern;
        
        PrivilegeRule(String aMatchString, int aPrivilege) {
            matchString = aMatchString;
            privilege   = aPrivilege;
            pattern     = java.util.regex.Pattern.compile(matchString);
        }
        
    }
    
    String ruleFileName;
    String boardTypeName;
    
    List<PrivilegeRule>   cliPrivilegeRules;

    StringBuffer exportResult;       // include all mismatch and returned
    
    public String getTitle() {
        return "board,keyword,defined,expect\n";
    }
    
    public void setBordType(String board) {
        boardTypeName = board;
    }
    
    public void setRuleFileName(String fileName){
        ruleFileName = fileName;
    }
    
    
    
    void exportCliCommand(CliCommand cliCommand) 
    {
        boolean keywordMatched = false;
        
        for (int i=0; i<cliPrivilegeRules.size(); i++) {
            PrivilegeRule privilegeRule = cliPrivilegeRules.get(i);
            java.util.regex.Matcher matcher = privilegeRule.pattern.matcher(cliCommand.getKeywords());
            
            if (matcher.matches()) {
                if (cliCommand.getPrivilege() != privilegeRule.privilege) {
                    exportResult.append( boardTypeName + ',' + cliCommand.getKeywords() + "," + 
                                         cliCommand.getPrivilegeName()+","+
                                         CliCommand.getPrivilegeName(privilegeRule.privilege) + "\n");
                }
                
                keywordMatched = true;
                break;
            }
        }
        
        if (!keywordMatched) {
            exportResult.append(boardTypeName + ',' +  cliCommand.getKeywords() + "," + 
                                cliCommand.getPrivilegeName() + "," + "NA\n");
        }        
    }
    
    
    
    public StringBuffer export(CliCommandTree cmdTree) 
    {
        if (!readPrivilegeRules())
        {
            System.out.println("Wrong reading: "+ruleFileName);
            System.exit(-1);
        }
        
        exportResult= new StringBuffer();
        
        super.setSequence(Sequence.lexical);
        super.walkthrough(cmdTree);
        
        return exportResult;
    }   
    
    boolean readPrivilegeRules() {
        boolean result = true;
        cliPrivilegeRules = new ArrayList<PrivilegeRule> ();
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xml_doc = db.parse(ruleFileName);
            xml_doc.normalize();
            
            Element root = xml_doc.getDocumentElement(); 
            if (root == null) return false; 
 
            NodeList cliPriRuleList = root.getElementsByTagName("rule"); 
            if (cliPriRuleList == null) return false;
            
            for(int i = 0; i < cliPriRuleList.getLength(); i++) 
            {  
                Element cliRuleElement = (Element) cliPriRuleList.item(i);
                if (cliRuleElement.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                
                String patternName   = cliRuleElement.getAttribute("pattern");
                String privilegeName = cliRuleElement.getAttribute("privilege");
                
                int privilege = CliCommand.getPrivilege(privilegeName);
                PrivilegeRule rule = new PrivilegeRule(patternName, privilege);
                
                cliPrivilegeRules.add(rule);
            }
        } catch (ParserConfigurationException e) {  
            e.printStackTrace();  
            result = false;
        } catch (FileNotFoundException e) {  
            e.printStackTrace();
            result = false;
        } catch (SAXException e) {  
            e.printStackTrace();
            result = false;            
        } catch (IOException e) {  
            e.printStackTrace();
            result = false;
        } 
        
        return result;
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





class CliExportUIList extends CliExport {

    String boardTypeName;
    StringBuffer exportResult;
    
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






class CliExportXML extends CliExport{

    Document document;
    Element root;
    
    void exportCliCommand(CliCommand cliCommand) {
        
        Comment comment = document.createComment(cliCommand.getKeywords());
        root.appendChild(comment);
        
        Element cliElement = document.createElement("command"); 
        
        Element keyword = document.createElement("keyword"); 
        keyword.appendChild(document.createTextNode(cliCommand.getKeywords())); 
        cliElement.appendChild(keyword); 
        
        Element syntax = document.createElement("syntax");
        syntax.setAttribute("mode", cliCommand.getCliCmdMode().toString());
        if (cliCommand.isSCMCommand())
            syntax.setAttribute("board", "scm");
        else
            syntax.setAttribute("board", "all");

        //System.out.println("keyword: " + cliCommand.getKeywords() + " sys_parameter:" + cliCommand.isSystemParameter());        
        syntax.setAttribute("display",   cliCommand.getDisplayModeName());
        syntax.setAttribute("privilege", cliCommand.getPrivilegeName());
        syntax.setAttribute("function", cliCommand.getFunctionName());
        syntax.setAttribute("source", cliCommand.getSource().toString());
        cliElement.appendChild(syntax);
        
        if (cliCommand.isXMLCommand()) {
            Element helpElement;
            for (String helpMsg: cliCommand.getHelp()) {
                 helpElement = document.createElement("help");
                 helpElement.appendChild(document.createTextNode(helpMsg)); 
                 cliElement.appendChild(helpElement); 
            }
        }
        
        for (CliNode cliNode: cliCommand.getNodes()) {
            if (cliNode.isParameterNode()) {
                Element cliParamElement = document.createElement("param"); 
                CliNodeParameter cliParameter = (CliNodeParameter)cliNode;
                
                cliParamElement.setAttribute("required", cliParameter.getRequiredName());
                cliParamElement.setAttribute("datatype", cliParameter.getDataType());

                //System.out.println("keyword: " + cliCommand.getKeywords() + " unit:" + cliParameter.getUnit());
                if (!cliParameter.getUnit().isEmpty()) {
                    cliParamElement.setAttribute("unit", cliParameter.getUnit());
                }
                
                Element paramKeyword = document.createElement("keyword"); 
                paramKeyword.appendChild(document.createTextNode(cliParameter.getKeyword())); 
                cliParamElement.appendChild(paramKeyword); 

                if (cliParameter.isRangeSet()) {
                    Element paramRange = document.createElement("range"); 
                    paramRange.appendChild(document.createTextNode(cliParameter.getMinValue() + "," + cliParameter.getMaxValue())); 
                    cliParamElement.appendChild(paramRange);
                }

                if (!cliParameter.getDefValue().isEmpty()) {
                    Element paramDef = document.createElement("default"); 
                    paramDef.appendChild(document.createTextNode(cliParameter.getDefValue())); 
                    cliParamElement.appendChild(paramDef);
                }
                
                if (cliCommand.isXMLCommand()) {
                    Element helpElement;
                    for (String helpMsg: cliParameter.getHelps()) {
                         helpElement = document.createElement("help");
                         helpElement.appendChild(document.createTextNode(helpMsg)); 
                         cliParamElement.appendChild(helpElement); 
                    }
                }
                
                cliElement.appendChild(cliParamElement);
            }
        }
        
        root.appendChild(cliElement);
    }
   
    public void export(String fileName, CliCommandTree cliMainCmdTree, CliCommandTree cliDiagCmdTree)
    { 
        document=null; 
        try { 
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
            DocumentBuilder builder = factory.newDocumentBuilder(); 
            document = builder.newDocument(); 
        } catch (ParserConfigurationException e) { 
            System.out.println(e.getMessage()); 
        } 
        
        if (document == null) return;
        
        root = document.createElement("ui_definition_file"); 
        document.appendChild(root); 

        super.setSequence(Sequence.lexical);
        if (cliMainCmdTree != null)
            super.walkthrough(cliMainCmdTree);
        if (cliDiagCmdTree != null)
            super.walkthrough(cliDiagCmdTree);

        
        TransformerFactory tf = TransformerFactory.newInstance(); 
        try { 
            Transformer transformer = tf.newTransformer(); 
            DOMSource source = new DOMSource(document); 
            transformer.setOutputProperty(OutputKeys.ENCODING, "gb2312"); 
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            PrintWriter pw = new PrintWriter(new FileOutputStream(fileName)); 
            StreamResult result = new StreamResult(pw); 
            //XMLOutputter out;
            transformer.transform(source, result); 
            System.out.println("create XML file <"+ fileName + "> successful!"); 
        } catch (TransformerConfigurationException e) { 
        System.out.println(e.getMessage()); 
        } catch (IllegalArgumentException e) { 
        System.out.println(e.getMessage()); 
        } catch (FileNotFoundException e) { 
        System.out.println(e.getMessage()); 
        } catch (TransformerException e) { 
        System.out.println(e.getMessage()); 
        } 
    }   
}




class CliCheckKeywordLen extends CliExport {
    
    String keyworkFileName = "/VOBS/MediaGW/tools/various/CliShell/bin/cli_keyword_list.xml";
    
    List<String>    cliExceptionKeywordList;
    boolean         checkResult;  /* whole check result */
    
    
    void exportCliCommand(CliCommand cliCommand) 
    {
        boolean keywordMatched = false;
        
        for (CliNode node: cliCommand.getNodes()) {
            if (!node.isKeywordValid()) {
                keywordMatched = false;
                
                for (int i=0; i<cliExceptionKeywordList.size(); i++) 
                {
                    if (cliExceptionKeywordList.get(i).equals(node.getKeyword()))
                    {
                        keywordMatched = true;
                        break;
                    }
                }
                
                if (keywordMatched == false)
                {
                    System.out.println("\nkeyword <" + node.getKeyword() + "> is too long in CLI: "+cliCommand.getSyntaxString()+"\n");
                    checkResult = false;
                }
            }
        }               
    }
    
    public boolean export(CliCommandTree cliMainCmdTree, CliCommandTree cliDiagCmdTree) 
    {
        checkResult = true;
        
        if (!readExceptionKeywordList())
        {
            System.out.println("Wrong reading: "+keyworkFileName);
            System.exit(-1);
        }
        
        super.setSequence(Sequence.lexical);
        super.walkthrough(cliMainCmdTree);
        super.walkthrough(cliDiagCmdTree);
        
        return checkResult;
    }   
    
    

    boolean readExceptionKeywordList() {
        boolean result = true;
        cliExceptionKeywordList = new ArrayList<String> ();
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xml_doc = db.parse(keyworkFileName);
            xml_doc.normalize();
            
            Element root = xml_doc.getDocumentElement(); 
            if (root == null) return false; 
 
            NodeList cliKeywordList = root.getElementsByTagName("keyword"); 
            if (cliKeywordList == null) return false;
            
            for(int i = 0; i < cliKeywordList.getLength(); i++) 
            {  
                Element cliKeywordElement = (Element) cliKeywordList.item(i);
                if (cliKeywordElement.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                String keyword   = cliKeywordElement.getAttribute("keyname");

                cliExceptionKeywordList.add(keyword);
                //System.out.println("keyword=<" + keyword + ">");
            }
        } catch (ParserConfigurationException e) {  
            e.printStackTrace();  
            result = false;
        } catch (FileNotFoundException e) {  
            e.printStackTrace();
            result = false;
        } catch (SAXException e) {  
            e.printStackTrace();
            result = false;            
        } catch (IOException e) {  
            e.printStackTrace();
            result = false;
        } 
        
        return result;
    }
}






class CliExportHTMLHelp extends CliExport{
    
    private enum HtmlSection  { cmdList, cmdDetail  }
    
    StringBuffer exportResult;
    HtmlSection  section;
    
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
