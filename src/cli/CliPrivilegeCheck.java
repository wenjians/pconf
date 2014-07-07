package cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cli.CliExport.Sequence;

public class CliPrivilegeCheck extends CliExport {
    
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